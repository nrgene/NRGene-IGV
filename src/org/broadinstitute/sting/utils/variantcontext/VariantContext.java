package org.broadinstitute.sting.utils.variantcontext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.broad.tribble.Feature;
import org.broad.tribble.TribbleException.InternalCodecException;
import org.broad.tribble.util.ParsingUtils;
import org.broadinstitute.sting.utils.exceptions.ReviewedStingException;

public class VariantContext implements Feature {
   protected CommonInfo commonInfo;
   public static final double NO_LOG10_PERROR = 1.0D;
   /** @deprecated */
   @Deprecated
   private static final String ID_KEY = "ID";
   private final Byte REFERENCE_BASE_FOR_INDEL;
   public static final Set PASSES_FILTERS = Collections.unmodifiableSet(new LinkedHashSet());
   protected final String contig;
   protected final long start;
   protected final long stop;
   private final String ID;
   protected VariantContext.Type type;
   protected final List<Allele> alleles;
   protected GenotypesContext genotypes;
   protected int[] genotypeCounts;
   public static final GenotypesContext NO_GENOTYPES;
   private Allele REF;
   private Allele ALT;
   private Boolean monomorphic;
   private static final EnumSet ALL_VALIDATION;
   private static final EnumSet NO_VALIDATION;

   protected VariantContext(VariantContext other) {
      this(other.getSource(), other.getID(), other.getChr(), (long)other.getStart(), (long)other.getEnd(), other.getAlleles(), other.getGenotypes(), other.getLog10PError(), other.getFiltersMaybeNull(), other.getAttributes(), other.REFERENCE_BASE_FOR_INDEL, NO_VALIDATION);
   }

   protected VariantContext(String source, String ID, String contig, long start, long stop, Collection<Allele> alleles, GenotypesContext genotypes, double log10PError, Set filters, Map attributes, Byte referenceBaseForIndel, EnumSet validationToPerform) {
      this.commonInfo = null;
      this.type = null;
      this.genotypes = null;
      this.genotypeCounts = null;
      this.REF = null;
      this.ALT = null;
      this.monomorphic = null;
      if (contig == null) {
         throw new IllegalArgumentException("Contig cannot be null");
      } else {
         this.contig = contig;
         this.start = start;
         this.stop = stop;
         if (ID != null && !ID.equals("")) {
            this.ID = ID.equals(".") ? "." : ID;
            this.commonInfo = new CommonInfo(source, log10PError, filters, attributes);
            this.REFERENCE_BASE_FOR_INDEL = referenceBaseForIndel;
            if (this.commonInfo.hasAttribute("ID")) {
               throw new IllegalArgumentException("Trying to create a VariantContext with a ID key.  Please use provided constructor argument ID");
            } else if (alleles == null) {
               throw new IllegalArgumentException("Alleles cannot be null");
            } else {
               this.alleles = makeAlleles(alleles);
               if (genotypes != null && genotypes != NO_GENOTYPES) {
                  this.genotypes = genotypes.immutable();
               } else {
                  this.genotypes = NO_GENOTYPES;
               }

               int nAlleles = alleles.size();

               for (Object allele : alleles) {
                  Allele a = (Allele) allele;
                  if (a.isReference()) {
                     this.REF = a;
                  } else if (nAlleles == 2) {
                     this.ALT = a;
                  }
               }

               if (!validationToPerform.isEmpty()) {
                  this.validate(validationToPerform);
               }

            }
         } else {
            throw new IllegalArgumentException("ID field cannot be the null or the empty string");
         }
      }
   }

   public VariantContext subContextFromSamples(Set sampleNames, Collection alleles) {
      VariantContextBuilder builder = new VariantContextBuilder(this);
      return builder.genotypes(this.genotypes.subsetToSamples(sampleNames)).alleles(alleles).make();
   }

   public VariantContext subContextFromSamples(Set sampleNames) {
      VariantContextBuilder builder = new VariantContextBuilder(this);
      GenotypesContext newGenotypes = this.genotypes.subsetToSamples(sampleNames);
      return builder.genotypes(newGenotypes).alleles(this.allelesOfGenotypes(newGenotypes)).make();
   }

   public VariantContext subContextFromSample(String sampleName) {
      return this.subContextFromSamples(Collections.singleton(sampleName));
   }

   private final Set<Allele> allelesOfGenotypes(Collection genotypes) {
      Set<Allele> alleles = new HashSet<>();
      boolean addedref = false;

      for (Object genotype : genotypes) {
         Genotype g = (Genotype) genotype;

         for (Object o : g.getAlleles()) {
            Allele a = (Allele) o;
            addedref = addedref || a.isReference();
            if (a.isCalled()) {
               alleles.add(a);
            }
         }
      }

      if (!addedref) {
         alleles.add(this.getReference());
      }

      return alleles;
   }

   public VariantContext.Type getType() {
      if (this.type == null) {
         this.determineType();
      }

      return this.type;
   }

   public boolean isSNP() {
      return this.getType() == VariantContext.Type.SNP;
   }

   public boolean isVariant() {
      return this.getType() != VariantContext.Type.NO_VARIATION;
   }

   public boolean isPointEvent() {
      return this.isSNP() || !this.isVariant();
   }

   public boolean isIndel() {
      return this.getType() == VariantContext.Type.INDEL;
   }

   /**
    * @return true if the alleles indicate a simple insertion (i.e., the reference allele is Null)
    */
   public boolean isSimpleInsertion() {
      // can't just call !isSimpleDeletion() because of complex indels
      return isSimpleIndel() && getReference().length() == 1;
   }

   /**
    * @return true if the alleles indicate a simple deletion (i.e., a single alt allele that is Null)
    */
   public boolean isSimpleDeletion() {
      // can't just call !isSimpleInsertion() because of complex indels
      return isSimpleIndel() && getAlternateAllele(0).length() == 1;
   }

   /**
    * @return true if the alleles indicate a simple indel, false otherwise.
    */
   public boolean isSimpleIndel() {
      return getType() == Type.INDEL                   // allelic lengths differ
          && isBiallelic()                         // exactly 2 alleles
          && getReference().length() > 0           // ref is not null or symbolic
          && getAlternateAllele(0).length() > 0    // alt is not null or symbolic
          && getReference().getBases()[0] == getAlternateAllele(0).getBases()[0]    // leading bases match for both alleles
          && (getReference().length() == 1 || getAlternateAllele(0).length() == 1);
   }

   public boolean isComplexIndel() {
      return this.isIndel() && !this.isSimpleDeletion() && !this.isSimpleInsertion();
   }

   public boolean isSymbolic() {
      return this.getType() == VariantContext.Type.SYMBOLIC;
   }

   public boolean isMNP() {
      return this.getType() == VariantContext.Type.MNP;
   }

   public boolean isMixed() {
      return this.getType() == VariantContext.Type.MIXED;
   }

   public boolean hasID() {
      return this.getID() != ".";
   }

   public boolean emptyID() {
      return !this.hasID();
   }

   public String getID() {
      return this.ID;
   }

   public boolean hasReferenceBaseForIndel() {
      return this.REFERENCE_BASE_FOR_INDEL != null;
   }

   public Byte getReferenceBaseForIndel() {
      return this.REFERENCE_BASE_FOR_INDEL;
   }

   public String getSource() {
      return this.commonInfo.getName();
   }

   public Set getFiltersMaybeNull() {
      return this.commonInfo.getFiltersMaybeNull();
   }

   public Set getFilters() {
      return this.commonInfo.getFilters();
   }

   public boolean isFiltered() {
      return this.commonInfo.isFiltered();
   }

   public boolean isNotFiltered() {
      return this.commonInfo.isNotFiltered();
   }

   public boolean filtersWereApplied() {
      return this.commonInfo.filtersWereApplied();
   }

   public boolean hasLog10PError() {
      return this.commonInfo.hasLog10PError();
   }

   public double getLog10PError() {
      return this.commonInfo.getLog10PError();
   }

   public double getPhredScaledQual() {
      return this.commonInfo.getPhredScaledQual();
   }

   public Map getAttributes() {
      return this.commonInfo.getAttributes();
   }

   public boolean hasAttribute(String key) {
      return this.commonInfo.hasAttribute(key);
   }

   public Object getAttribute(String key) {
      return this.commonInfo.getAttribute(key);
   }

   public Object getAttribute(String key, Object defaultValue) {
      return this.commonInfo.getAttribute(key, defaultValue);
   }

   public String getAttributeAsString(String key, String defaultValue) {
      return this.commonInfo.getAttributeAsString(key, defaultValue);
   }

   public int getAttributeAsInt(String key, int defaultValue) {
      return this.commonInfo.getAttributeAsInt(key, defaultValue);
   }

   public double getAttributeAsDouble(String key, double defaultValue) {
      return this.commonInfo.getAttributeAsDouble(key, defaultValue);
   }

   public boolean getAttributeAsBoolean(String key, boolean defaultValue) {
      return this.commonInfo.getAttributeAsBoolean(key, defaultValue);
   }

   public Allele getReference() {
      Allele ref = this.REF;
      if (ref == null) {
         throw new IllegalStateException("BUG: no reference allele found at " + this);
      } else {
         return ref;
      }
   }

   public boolean isBiallelic() {
      return this.getNAlleles() == 2;
   }

   public int getNAlleles() {
      return this.alleles.size();
   }

   public Allele getAllele(String allele) {
      return this.getAllele(allele.getBytes());
   }

   public Allele getAllele(byte[] allele) {
      return Allele.getMatchingAllele(this.getAlleles(), (byte[])allele);
   }

   public boolean hasAllele(Allele allele) {
      return this.hasAllele(allele, false, true);
   }

   public boolean hasAllele(Allele allele, boolean ignoreRefState) {
      return this.hasAllele(allele, ignoreRefState, true);
   }

   public boolean hasAlternateAllele(Allele allele) {
      return this.hasAllele(allele, false, false);
   }

   public boolean hasAlternateAllele(Allele allele, boolean ignoreRefState) {
      return this.hasAllele(allele, ignoreRefState, false);
   }

   private boolean hasAllele(Allele allele, boolean ignoreRefState, boolean considerRefAllele) {
      if ((!considerRefAllele || allele != this.REF) && allele != this.ALT) {
         List allelesToConsider = considerRefAllele ? this.getAlleles() : this.getAlternateAlleles();
         Iterator j = allelesToConsider.iterator();

         Allele a;
         do {
            if (!j.hasNext()) {
               return false;
            }

            a = (Allele)j.next();
         } while(!a.equals(allele, ignoreRefState));

         return true;
      } else {
         return true;
      }
   }

   public List<Allele> getAlleles() {
      return this.alleles;
   }

   public List getAlternateAlleles() {
      return this.alleles.subList(1, this.alleles.size());
   }

   public List getIndelLengths() {
      if (this.getType() != VariantContext.Type.INDEL && this.getType() != VariantContext.Type.MIXED) {
         return null;
      } else {
         List lengths = new ArrayList();
         Iterator j = this.getAlternateAlleles().iterator();

         while(j.hasNext()) {
            Allele a = (Allele)j.next();
            lengths.add(a.length() - this.getReference().length());
         }

         return lengths;
      }
   }

   public Allele getAlternateAllele(int i) {
      return (Allele)this.alleles.get(i + 1);
   }

   public boolean hasSameAllelesAs(VariantContext other) {
      return this.hasSameAlternateAllelesAs(other) && other.getReference().equals(this.getReference(), false);
   }

   public boolean hasSameAlternateAllelesAs(VariantContext other) {
      List thisAlternateAlleles = this.getAlternateAlleles();
      List otherAlternateAlleles = other.getAlternateAlleles();
      if (thisAlternateAlleles.size() != otherAlternateAlleles.size()) {
         return false;
      } else {
         Iterator j = thisAlternateAlleles.iterator();

         Allele allele;
         do {
            if (!j.hasNext()) {
               return true;
            }

            allele = (Allele)j.next();
         } while(otherAlternateAlleles.contains(allele));

         return false;
      }
   }

   public int getNSamples() {
      return this.genotypes.size();
   }

   public boolean hasGenotypes() {
      return !this.genotypes.isEmpty();
   }

   public boolean hasGenotypes(Collection sampleNames) {
      return this.genotypes.containsSamples(sampleNames);
   }

   public GenotypesContext getGenotypes() {
      return this.genotypes;
   }

   public Iterable getGenotypesOrderedByName() {
      return this.genotypes.iterateInSampleNameOrder();
   }

   public Iterable getGenotypesOrderedBy(Iterable sampleOrdering) {
      return this.genotypes.iterateInSampleNameOrder(sampleOrdering);
   }

   public GenotypesContext getGenotypes(String sampleName) {
      return this.getGenotypes(Collections.singleton(sampleName));
   }

   protected GenotypesContext getGenotypes(Collection sampleNames) {
      return this.getGenotypes().subsetToSamples(new HashSet(sampleNames));
   }

   public GenotypesContext getGenotypes(Set sampleNames) {
      return this.getGenotypes().subsetToSamples(sampleNames);
   }

   public Set getSampleNames() {
      return this.getGenotypes().getSampleNames();
   }

   public List getSampleNamesOrderedByName() {
      return this.getGenotypes().getSampleNamesOrderedByName();
   }

   public Genotype getGenotype(String sample) {
      return this.getGenotypes().get(sample);
   }

   public boolean hasGenotype(String sample) {
      return this.getGenotypes().containsSample(sample);
   }

   public Genotype getGenotype(int ith) {
      return this.genotypes.get(ith);
   }

   public int getCalledChrCount() {
      return this.getCalledChrCount((Set)(new HashSet(0)));
   }

   public int getCalledChrCount(Set sampleIds) {
      int n = 0;
      GenotypesContext genotypes = sampleIds.isEmpty() ? this.getGenotypes() : this.getGenotypes(sampleIds);
      Iterator j = genotypes.iterator();

      while(j.hasNext()) {
         Genotype g = (Genotype)j.next();

         Allele a;
         for(Iterator k = g.getAlleles().iterator();k.hasNext(); n += a.isNoCall() ? 0 : 1) {
            a = (Allele)j.next();
         }
      }

      return n;
   }

   public int getCalledChrCount(Allele a) {
      return this.getCalledChrCount(a, new HashSet(0));
   }

   public int getCalledChrCount(Allele a, Set sampleIds) {
      int n = 0;
      GenotypesContext genotypes = sampleIds.isEmpty() ? this.getGenotypes() : this.getGenotypes(sampleIds);

      Genotype g;
      for(Iterator j = genotypes.iterator(); j.hasNext(); n += g.getAlleles(a).size()) {
         g = (Genotype)j.next();
      }

      return n;
   }

   public boolean isMonomorphicInSamples() {
      if (this.monomorphic == null) {
         this.monomorphic = !this.isVariant() || this.hasGenotypes() && this.getCalledChrCount(this.getReference()) == this.getCalledChrCount();
      }

      return this.monomorphic;
   }

   public boolean isPolymorphicInSamples() {
      return !this.isMonomorphicInSamples();
   }

   private void calculateGenotypeCounts() {
      if (this.genotypeCounts == null) {
         this.genotypeCounts = new int[Genotype.Type.values().length];

         Genotype g;
         int var10002;
         for(Iterator j = this.getGenotypes().iterator(); j.hasNext(); var10002 = this.genotypeCounts[g.getType().ordinal()]++) {
            g = (Genotype)j.next();
         }
      }

   }

   public int getNoCallCount() {
      this.calculateGenotypeCounts();
      return this.genotypeCounts[Genotype.Type.NO_CALL.ordinal()];
   }

   public int getHomRefCount() {
      this.calculateGenotypeCounts();
      return this.genotypeCounts[Genotype.Type.HOM_REF.ordinal()];
   }

   public int getHetCount() {
      this.calculateGenotypeCounts();
      return this.genotypeCounts[Genotype.Type.HET.ordinal()];
   }

   public int getHomVarCount() {
      this.calculateGenotypeCounts();
      return this.genotypeCounts[Genotype.Type.HOM_VAR.ordinal()];
   }

   public int getMixedCount() {
      this.calculateGenotypeCounts();
      return this.genotypeCounts[Genotype.Type.MIXED.ordinal()];
   }


   public void validateRSIDs(Set rsIDs) {
      if (rsIDs != null && this.hasID()) {
         String[] arr$ = this.getID().split(";");
         int len$ = arr$.length;

         for(int j = 0; j < len$; ++j) {
            String id = arr$[j];
            if (id.startsWith("rs") && !rsIDs.contains(id)) {
               throw new InternalCodecException(String.format("the rsID %s for the record at position %s:%d is not in dbSNP", id, this.getChr(), this.getStart()));
            }
         }
      }

   }

   public void validateAlternateAlleles() {
      if (this.hasGenotypes()) {
         List reportedAlleles = this.getAlleles();
         Set observedAlleles = new HashSet();
         observedAlleles.add(this.getReference());
         Iterator j = this.getGenotypes().iterator();

         while(j.hasNext()) {
            Genotype g = (Genotype)j.next();
            if (g.isCalled()) {
               observedAlleles.addAll(g.getAlleles());
            }
         }

         if (reportedAlleles.size() != observedAlleles.size()) {
            throw new InternalCodecException(String.format("the ALT allele(s) for the record at position %s:%d do not match what is observed in the per-sample genotypes", this.getChr(), this.getStart()));
         } else {
            int originalSize = reportedAlleles.size();
            observedAlleles.retainAll(reportedAlleles);
            if (observedAlleles.size() != originalSize) {
               throw new InternalCodecException(String.format("the ALT allele(s) for the record at position %s:%d do not match what is observed in the per-sample genotypes", this.getChr(), this.getStart()));
            }
         }
      }
   }

   public void validateChromosomeCounts() {
      if (this.hasGenotypes()) {
         int reportedAC;
         if (this.hasAttribute("AN")) {
            int reportedAN = Integer.valueOf(this.getAttribute("AN").toString());
            reportedAC = this.getCalledChrCount();
            if (reportedAN != reportedAC) {
               throw new InternalCodecException(String.format("the Allele Number (AN) tag is incorrect for the record at position %s:%d, %d vs. %d", this.getChr(), this.getStart(), reportedAN, reportedAC));
            }
         }

         if (this.hasAttribute("AC")) {
            ArrayList observedACs = new ArrayList();
            if (this.getAlternateAlleles().size() > 0) {
               Iterator j = this.getAlternateAlleles().iterator();

               while(j.hasNext()) {
                  Allele allele = (Allele)j.next();
                  observedACs.add(this.getCalledChrCount(allele));
               }
            } else {
               observedACs.add(0);
            }

            if (this.getAttribute("AC") instanceof List) {
               Collections.sort(observedACs);
               List reportedACs = (List)this.getAttribute("AC");
               Collections.sort(reportedACs);
               if (observedACs.size() != reportedACs.size()) {
                  throw new InternalCodecException(String.format("the Allele Count (AC) tag doesn't have the correct number of values for the record at position %s:%d, %d vs. %d", this.getChr(), this.getStart(), reportedACs.size(), observedACs.size()));
               }

               for(int i = 0; i < observedACs.size(); ++i) {
                  if (Integer.valueOf(reportedACs.get(i).toString()) != observedACs.get(i)) {
                     throw new InternalCodecException(String.format("the Allele Count (AC) tag is incorrect for the record at position %s:%d, %s vs. %d", this.getChr(), this.getStart(), reportedACs.get(i), observedACs.get(i)));
                  }
               }
            } else {
               if (observedACs.size() != 1) {
                  throw new InternalCodecException(String.format("the Allele Count (AC) tag doesn't have enough values for the record at position %s:%d", this.getChr(), this.getStart()));
               }

               reportedAC = Integer.valueOf(this.getAttribute("AC").toString());
               if (reportedAC != (Integer)observedACs.get(0)) {
                  throw new InternalCodecException(String.format("the Allele Count (AC) tag is incorrect for the record at position %s:%d, %d vs. %d", this.getChr(), this.getStart(), reportedAC, observedACs.get(0)));
               }
            }
         }

      }
   }

   private boolean validate(EnumSet validationToPerform) {
      Iterator j = validationToPerform.iterator();

      while(j.hasNext()) {
         VariantContext.Validation val = (VariantContext.Validation)j.next();
         switch(val) {
         case ALLELES:
            this.validateAlleles();
            break;
         case REF_PADDING:
            this.validateReferencePadding();
            break;
         case GENOTYPES:
            this.validateGenotypes();
            break;
         default:
            throw new IllegalArgumentException("Unexpected validation mode " + val);
         }
      }

      return true;
   }

   private void validateReferencePadding() {
      if (!this.hasSymbolicAlleles()) {
         boolean needsPadding = this.getReference().length() == this.getEnd() - this.getStart();
         if (needsPadding && !this.hasReferenceBaseForIndel()) {
            throw new ReviewedStingException("Badly formed variant context at location " + this.getChr() + ":" + this.getStart() + "; no padded reference base was provided.");
         }
      }
   }

   private void validateAlleles() {

      boolean alreadySeenRef = false;

      for ( final Allele allele : alleles ) {
         // make sure there's only one reference allele
         if ( allele.isReference() ) {
            if ( alreadySeenRef ) throw new IllegalArgumentException("BUG: Received two reference tagged alleles in VariantContext " + alleles + " this=" + this);
            alreadySeenRef = true;
         }

         if ( allele.isNoCall() ) {
            throw new IllegalArgumentException("BUG: Cannot add a no call allele to a variant context " + alleles + " this=" + this);
         }
      }

      // make sure there's one reference allele
      if ( ! alreadySeenRef )
         throw new IllegalArgumentException("No reference allele found in VariantContext");
   }

   private void validateGenotypes() {
      if (this.genotypes == null) {
         throw new IllegalStateException("Genotypes is null");
      } else {
         Iterator j = this.genotypes.iterator();

         while(true) {
            Genotype g;
            do {
               if (!j.hasNext()) {
                  return;
               }

               g = (Genotype)j.next();
            } while(!g.isAvailable());

            Iterator k = g.getAlleles().iterator();

            while(k.hasNext()) {
               Allele gAllele = (Allele)j.next();
               if (!this.hasAllele(gAllele) && gAllele.isCalled()) {
                  throw new IllegalStateException("Allele in genotype " + gAllele + " not in the variant context " + this.alleles);
               }
            }
         }
      }
   }

   private void determineType() {
      if (this.type == null) {
         switch(this.getNAlleles()) {
         case 0:
            throw new IllegalStateException("Unexpected error: requested type of VariantContext with no alleles!" + this);
         case 1:
            this.type = VariantContext.Type.NO_VARIATION;
            break;
         default:
            this.determinePolymorphicType();
         }
      }

   }

   private void determinePolymorphicType() {
      this.type = null;
      Iterator j = this.alleles.iterator();

      while(j.hasNext()) {
         Allele allele = (Allele)j.next();
         if (allele != this.REF) {
            VariantContext.Type biallelicType = typeOfBiallelicVariant(this.REF, allele);
            if (this.type == null) {
               this.type = biallelicType;
            } else if (biallelicType != this.type) {
               this.type = VariantContext.Type.MIXED;
               return;
            }
         }
      }

   }

   private static VariantContext.Type typeOfBiallelicVariant(Allele ref, Allele allele) {
      if (ref.isSymbolic()) {
         throw new IllegalStateException("Unexpected error: encountered a record with a symbolic reference allele");
      } else if (allele.isSymbolic()) {
         return VariantContext.Type.SYMBOLIC;
      } else if (ref.length() == allele.length()) {
         return allele.length() == 1 ? VariantContext.Type.SNP : VariantContext.Type.MNP;
      } else {
         return VariantContext.Type.INDEL;
      }
   }

   public String toString() {
      return String.format("[VC %s @ %s of type=%s alleles=%s attr=%s GT=%s", this.getSource(), this.contig + ":" + (this.start - this.stop == 0L ? this.start : this.start + "-" + this.stop), this.getType(), ParsingUtils.sortList(this.getAlleles()), ParsingUtils.sortedString(this.getAttributes()), this.getGenotypes());
   }

   private static List<Allele> makeAlleles(Collection<Allele> alleles) {
      List<Allele> alleleList = new ArrayList<>(alleles.size());
      boolean sawRef = false;

      for (Object allele : alleles) {
         Allele a = (Allele) allele;

         for (Object anAlleleList : alleleList) {
            Allele b = (Allele) anAlleleList;
            if (a.equals(b, true)) {
               throw new IllegalArgumentException("Duplicate allele added to VariantContext: " + a);
            }
         }

         if (a.isReference()) {
            if (sawRef) {
               throw new IllegalArgumentException("Alleles for a VariantContext must contain at most one reference allele: " + alleles);
            }

            alleleList.add(0, a);
            sawRef = true;
         } else {
            alleleList.add(a);
         }
      }

      if (alleleList.isEmpty()) {
         throw new IllegalArgumentException("Cannot create a VariantContext with an empty allele list");
      } else if (alleleList.get(0).isNonReference()) {
         throw new IllegalArgumentException("Alleles for a VariantContext must contain at least one reference allele: " + alleles);
      } else {
         return alleleList;
      }
   }

   public String getChr() {
      return this.contig;
   }

   public int getStart() {
      return (int)this.start;
   }

   public int getEnd() {
      return (int)this.stop;
   }

   public boolean hasSymbolicAlleles() {
      Iterator j = this.getAlleles().iterator();

      Allele a;
      do {
         if (!j.hasNext()) {
            return false;
         }

         a = (Allele)j.next();
      } while(!a.isSymbolic());

      return true;
   }

   public Allele getAltAlleleWithHighestAlleleCount() {
      if (this.isBiallelic()) {
         return this.getAlternateAllele(0);
      } else {
         Allele best = null;
         int maxAC1 = 0;

         for (Object o : this.getAlternateAlleles()) {
            Allele a = (Allele) o;
            int ac = this.getCalledChrCount(a);
            if (ac >= maxAC1) {
               maxAC1 = ac;
               best = a;
            }
         }

         return best;
      }
   }

   public int[] getGLIndecesOfAlternateAllele(Allele targetAllele) {
      int index = 1;

      for(Iterator j = this.getAlternateAlleles().iterator(); j.hasNext(); ++index) {
         Allele allele = (Allele)j.next();
         if (allele.equals(targetAllele)) {
            break;
         }
      }

      return GenotypeLikelihoods.getPLIndecesOfAlleles(0, index);
   }

   static {
      NO_GENOTYPES = GenotypesContext.NO_GENOTYPES;
      ALL_VALIDATION = EnumSet.allOf(VariantContext.Validation.class);
      NO_VALIDATION = EnumSet.noneOf(VariantContext.Validation.class);
   }

   public static enum Type {
      NO_VARIATION,
      SNP,
      MNP,
      INDEL,
      SYMBOLIC,
      MIXED
   }

   public static enum Validation {
      REF_PADDING,
      ALLELES,
      GENOTYPES
   }
}
