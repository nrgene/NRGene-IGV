package org.broadinstitute.sting.utils.variantcontext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.broad.tribble.util.ParsingUtils;
import org.broadinstitute.sting.utils.exceptions.ReviewedStingException;

public class Genotype implements Comparable<Genotype> {
   public static final String PHASED_ALLELE_SEPARATOR = "|";
   public static final String UNPHASED_ALLELE_SEPARATOR = "/";
   protected CommonInfo commonInfo;
   public static final double NO_LOG10_PERROR = 1.0D;
   protected List alleles;
   protected Genotype.Type type;
   protected boolean isPhased;

   public Genotype(String sampleName, List alleles, double log10PError, Set filters, Map attributes, boolean isPhased) {
      this(sampleName, alleles, log10PError, filters, attributes, isPhased, (double[])null);
   }

   public Genotype(String sampleName, List alleles, double log10PError, Set filters, Map attributes, boolean isPhased, double[] log10Likelihoods) {
      this.alleles = null;
      this.type = null;
      this.isPhased = false;
      if (alleles != null && !alleles.isEmpty()) {
         this.alleles = Collections.unmodifiableList(alleles);
      } else {
         this.alleles = Collections.emptyList();
      }

      this.commonInfo = new CommonInfo(sampleName, log10PError, filters, attributes);
      if (log10Likelihoods != null) {
         this.commonInfo.putAttribute("PL", GenotypeLikelihoods.fromLog10Likelihoods(log10Likelihoods));
      }

      this.isPhased = isPhased;
      this.validate();
   }

   public Genotype(String sampleName, List alleles, double log10PError, double[] log10Likelihoods) {
      this(sampleName, alleles, log10PError, (Set)null, (Map)null, false, log10Likelihoods);
   }

   public Genotype(String sampleName, List alleles, double log10PError) {
      this(sampleName, alleles, log10PError, (Set)null, (Map)null, false);
   }

   public Genotype(String sampleName, List alleles) {
      this(sampleName, alleles, 1.0D, (Set)null, (Map)null, false);
   }

   public Genotype(String sampleName, Genotype parent) {
      this(sampleName, parent.getAlleles(), parent.getLog10PError(), parent.getFilters(), parent.getAttributes(), parent.isPhased());
   }

   public static Genotype modifyName(Genotype g, String name) {
      return new Genotype(name, g.getAlleles(), g.getLog10PError(), g.filtersWereApplied() ? g.getFilters() : null, g.getAttributes(), g.isPhased());
   }

   public static Genotype modifyAttributes(Genotype g, Map attributes) {
      return new Genotype(g.getSampleName(), g.getAlleles(), g.getLog10PError(), g.filtersWereApplied() ? g.getFilters() : null, attributes, g.isPhased());
   }

   public static Genotype modifyAlleles(Genotype g, List alleles) {
      return new Genotype(g.getSampleName(), alleles, g.getLog10PError(), g.filtersWereApplied() ? g.getFilters() : null, g.getAttributes(), g.isPhased());
   }

   public List getAlleles() {
      return this.alleles;
   }

   public List getAlleles(Allele allele) {
      List al = new ArrayList();
      Iterator j = this.alleles.iterator();

      while(j.hasNext()) {
         Allele a = (Allele)j.next();
         if (a.equals(allele)) {
            al.add(a);
         }
      }

      return Collections.unmodifiableList(al);
   }

   public Allele getAllele(int i) {
      if (this.getType() == Genotype.Type.UNAVAILABLE) {
         throw new ReviewedStingException("Requesting alleles for an UNAVAILABLE genotype");
      } else {
         return (Allele)this.alleles.get(i);
      }
   }

   public boolean isPhased() {
      return this.isPhased;
   }

   public int getPloidy() {
      if (this.alleles.size() == 0) {
         throw new ReviewedStingException("Requesting ploidy for an UNAVAILABLE genotype");
      } else {
         return this.alleles.size();
      }
   }

   public Genotype.Type getType() {
      if (this.type == null) {
         this.type = this.determineType();
      }

      return this.type;
   }

   protected Genotype.Type determineType() {
      if (this.alleles.size() == 0) {
         return Genotype.Type.UNAVAILABLE;
      } else {
         boolean sawNoCall = false;
         boolean sawMultipleAlleles = false;
         Allele observedAllele = null;
         Iterator j = this.alleles.iterator();

         while(j.hasNext()) {
            Allele allele = (Allele)j.next();
            if (allele.isNoCall()) {
               sawNoCall = true;
            } else if (observedAllele == null) {
               observedAllele = allele;
            } else if (!allele.equals(observedAllele)) {
               sawMultipleAlleles = true;
            }
         }

         if (sawNoCall) {
            if (observedAllele == null) {
               return Genotype.Type.NO_CALL;
            } else {
               return Genotype.Type.MIXED;
            }
         } else if (observedAllele == null) {
            throw new ReviewedStingException("BUG: there are no alleles present in this genotype but the alleles list is not null");
         } else {
            return sawMultipleAlleles ? Genotype.Type.HET : (observedAllele.isReference() ? Genotype.Type.HOM_REF : Genotype.Type.HOM_VAR);
         }
      }
   }

   public boolean isHom() {
      return this.isHomRef() || this.isHomVar();
   }

   public boolean isHomRef() {
      return this.getType() == Genotype.Type.HOM_REF;
   }

   public boolean isHomVar() {
      return this.getType() == Genotype.Type.HOM_VAR;
   }

   public boolean isHet() {
      return this.getType() == Genotype.Type.HET;
   }

   public boolean isNoCall() {
      return this.getType() == Genotype.Type.NO_CALL;
   }

   public boolean isCalled() {
      return this.getType() != Genotype.Type.NO_CALL && this.getType() != Genotype.Type.UNAVAILABLE;
   }

   public boolean isMixed() {
      return this.getType() == Genotype.Type.MIXED;
   }

   public boolean isAvailable() {
      return this.getType() != Genotype.Type.UNAVAILABLE;
   }

   public boolean hasLikelihoods() {
      return this.hasAttribute("PL") && !this.getAttribute("PL").equals(".") || this.hasAttribute("GL") && !this.getAttribute("GL").equals(".");
   }

   public GenotypeLikelihoods getLikelihoods() {
      GenotypeLikelihoods x = this.getLikelihoods("PL", true);
      if (x != null) {
         return x;
      } else {
         x = this.getLikelihoods("GL", false);
         if (x != null) {
            return x;
         } else {
            throw new IllegalStateException("BUG: genotype likelihood field in " + this.getSampleName() + " sample are not either a string or a genotype likelihood class!");
         }
      }
   }

   private GenotypeLikelihoods getLikelihoods(String key, boolean asPL) {
      Object x = this.getAttribute(key);
      if (x instanceof String) {
         return asPL ? GenotypeLikelihoods.fromPLField((String)x) : GenotypeLikelihoods.fromGLField((String)x);
      } else {
         return x instanceof GenotypeLikelihoods ? (GenotypeLikelihoods)x : null;
      }
   }

   public void validate() {
      if (this.alleles.size() != 0) {
         Iterator j = this.alleles.iterator();

         Allele allele;
         do {
            if (!j.hasNext()) {
               return;
            }

            allele = (Allele)j.next();
         } while(allele != null);

         throw new IllegalArgumentException("BUG: allele cannot be null in Genotype");
      }
   }

   public String getGenotypeString() {
      return this.getGenotypeString(true);
   }

   public String getGenotypeString(boolean ignoreRefState) {
      return this.alleles.size() == 0 ? null : ParsingUtils.join(this.isPhased() ? "|" : "/", ignoreRefState ? this.getAlleleStrings() : (this.isPhased() ? this.getAlleles() : ParsingUtils.sortList(this.getAlleles())));
   }

   private List getAlleleStrings() {
      List al = new ArrayList();
      Iterator j = this.alleles.iterator();

      while(j.hasNext()) {
         Allele a = (Allele)j.next();
         al.add(a.getBaseString());
      }

      return al;
   }

   public String toString() {
      int Q = (int)Math.round(this.getPhredScaledQual());
      return String.format("[%s %s Q%s %s]", this.getSampleName(), this.getGenotypeString(false), Q == -10 ? "." : String.format("%2d", Q), sortedString(this.getAttributes()));
   }

   public String toBriefString() {
      return String.format("%s:Q%.2f", this.getGenotypeString(false), this.getPhredScaledQual());
   }

   public boolean sameGenotype(Genotype other) {
      return this.sameGenotype(other, true);
   }

   public boolean sameGenotype(Genotype other, boolean ignorePhase) {
      if (this.getPloidy() != other.getPloidy()) {
         return false;
      } else {
         Collection thisAlleles = this.getAlleles();
         Collection otherAlleles = other.getAlleles();
         if (ignorePhase) {
            thisAlleles = new TreeSet((Collection)thisAlleles);
            otherAlleles = new TreeSet((Collection)otherAlleles);
         }

         return thisAlleles.equals(otherAlleles);
      }
   }

   private static String sortedString(Map c) {
      List t = new ArrayList(c.keySet());
      Collections.sort(t);
      List pairs = new ArrayList();
      Iterator j = t.iterator();

      while(j.hasNext()) {
         Comparable k = (Comparable)j.next();
         pairs.add(k + "=" + c.get(k));
      }

      return "{" + ParsingUtils.join(", ", (String[])pairs.toArray(new String[pairs.size()])) + "}";
   }

   public String getSampleName() {
      return this.commonInfo.getName();
   }

   public Set getFilters() {
      return this.commonInfo.getFilters();
   }

   public Set getFiltersMaybeNull() {
      return this.commonInfo.getFiltersMaybeNull();
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

   public int compareTo(Genotype genotype) {
      return this.getSampleName().compareTo(genotype.getSampleName());
   }

   public static enum Type {
      NO_CALL,
      HOM_REF,
      HET,
      HOM_VAR,
      UNAVAILABLE,
      MIXED;
   }
}
