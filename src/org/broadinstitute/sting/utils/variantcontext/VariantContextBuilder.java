package org.broadinstitute.sting.utils.variantcontext;

import com.google.java.contract.Ensures;
import com.google.java.contract.Requires;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.broadinstitute.sting.utils.GenomeLoc;
import org.broadinstitute.sting.utils.exceptions.ReviewedStingException;

public class VariantContextBuilder {
   private String source = null;
   private String contig = null;
   private long start = -1L;
   private long stop = -1L;
   private Collection alleles = null;
   private String ID = ".";
   private GenotypesContext genotypes;
   private double log10PError;
   private Set filters;
   private Map attributes;
   private boolean attributesCanBeModified;
   private Byte referenceBaseForIndel;
   private final EnumSet toValidate;

   public VariantContextBuilder() {
      this.genotypes = GenotypesContext.NO_GENOTYPES;
      this.log10PError = 1.0D;
      this.filters = null;
      this.attributes = null;
      this.attributesCanBeModified = false;
      this.referenceBaseForIndel = null;
      this.toValidate = EnumSet.noneOf(VariantContext.Validation.class);
   }

   @Requires({"source != null", "contig != null", "start >= 0", "stop >= 0", "alleles != null && !alleles.isEmpty()"})
   public VariantContextBuilder(String source, String contig, long start, long stop, Collection alleles) {
      this.genotypes = GenotypesContext.NO_GENOTYPES;
      this.log10PError = 1.0D;
      this.filters = null;
      this.attributes = null;
      this.attributesCanBeModified = false;
      this.referenceBaseForIndel = null;
      this.toValidate = EnumSet.noneOf(VariantContext.Validation.class);
      this.source = source;
      this.contig = contig;
      this.start = start;
      this.stop = stop;
      this.alleles = alleles;
      this.toValidate.add(VariantContext.Validation.ALLELES);
   }

   public VariantContextBuilder(VariantContext parent) {
      this.genotypes = GenotypesContext.NO_GENOTYPES;
      this.log10PError = 1.0D;
      this.filters = null;
      this.attributes = null;
      this.attributesCanBeModified = false;
      this.referenceBaseForIndel = null;
      this.toValidate = EnumSet.noneOf(VariantContext.Validation.class);
      if (parent == null) {
         throw new ReviewedStingException("BUG: VariantContext parent argument cannot be null in VariantContextBuilder");
      } else {
         this.alleles = parent.alleles;
         this.attributes = parent.getAttributes();
         this.attributesCanBeModified = false;
         this.contig = parent.contig;
         this.filters = parent.getFiltersMaybeNull();
         this.genotypes = parent.genotypes;
         this.ID = parent.getID();
         this.log10PError = parent.getLog10PError();
         this.referenceBaseForIndel = parent.getReferenceBaseForIndel();
         this.source = parent.getSource();
         this.start = (long)parent.getStart();
         this.stop = (long)parent.getEnd();
      }
   }

   @Requires({"alleles != null", "!alleles.isEmpty()"})
   public VariantContextBuilder alleles(Collection alleles) {
      this.alleles = alleles;
      this.toValidate.add(VariantContext.Validation.ALLELES);
      return this;
   }

   public VariantContextBuilder attributes(Map attributes) {
      this.attributes = attributes;
      this.attributesCanBeModified = true;
      return this;
   }

   @Requires({"key != null"})
   @Ensures({"this.attributes.size() == old(this.attributes.size()) || this.attributes.size() == old(this.attributes.size()+1)"})
   public VariantContextBuilder attribute(String key, Object value) {
      this.makeAttributesModifiable();
      this.attributes.put(key, value);
      return this;
   }

   @Requires({"key != null"})
   @Ensures({"this.attributes.size() == old(this.attributes.size()) || this.attributes.size() == old(this.attributes.size()-1)"})
   public VariantContextBuilder rmAttribute(String key) {
      this.makeAttributesModifiable();
      this.attributes.remove(key);
      return this;
   }

   private void makeAttributesModifiable() {
      if (!this.attributesCanBeModified) {
         this.attributesCanBeModified = true;
         this.attributes = new HashMap(this.attributes);
      }

   }

   public VariantContextBuilder filters(Set filters) {
      this.filters = filters;
      return this;
   }

   public VariantContextBuilder filters(String... filters) {
      this.filters((Set)(new HashSet(Arrays.asList(filters))));
      return this;
   }

   public VariantContextBuilder passFilters() {
      return this.filters(VariantContext.PASSES_FILTERS);
   }

   public VariantContextBuilder unfiltered() {
      this.filters = null;
      return this;
   }

   public VariantContextBuilder genotypes(GenotypesContext genotypes) {
      this.genotypes = genotypes;
      if (genotypes != null) {
         this.toValidate.add(VariantContext.Validation.GENOTYPES);
      }

      return this;
   }

   public VariantContextBuilder genotypesNoValidation(GenotypesContext genotypes) {
      this.genotypes = genotypes;
      return this;
   }

   public VariantContextBuilder genotypes(Collection genotypes) {
      return this.genotypes(GenotypesContext.copy(genotypes));
   }

   public VariantContextBuilder genotypes(Genotype... genotypes) {
      return this.genotypes(GenotypesContext.copy((Collection)Arrays.asList(genotypes)));
   }

   public VariantContextBuilder noGenotypes() {
      this.genotypes = null;
      return this;
   }

   @Requires({"ID != null"})
   public VariantContextBuilder id(String ID) {
      this.ID = ID;
      return this;
   }

   public VariantContextBuilder noID() {
      return this.id(".");
   }

   @Requires({"log10PError <= 0 || log10PError == VariantContext.NO_LOG10_PERROR"})
   public VariantContextBuilder log10PError(double log10PError) {
      this.log10PError = log10PError;
      return this;
   }

   public VariantContextBuilder referenceBaseForIndel(Byte referenceBaseForIndel) {
      this.referenceBaseForIndel = referenceBaseForIndel;
      this.toValidate.add(VariantContext.Validation.REF_PADDING);
      return this;
   }

   @Requires({"source != null"})
   public VariantContextBuilder source(String source) {
      this.source = source;
      return this;
   }

   @Requires({"contig != null", "start >= 0", "stop >= 0"})
   public VariantContextBuilder loc(String contig, long start, long stop) {
      this.contig = contig;
      this.start = start;
      this.stop = stop;
      this.toValidate.add(VariantContext.Validation.ALLELES);
      this.toValidate.add(VariantContext.Validation.REF_PADDING);
      return this;
   }

   @Requires({"loc.getContig() != null", "loc.getStart() >= 0", "loc.getStop() >= 0"})
   public VariantContextBuilder loc(GenomeLoc loc) {
      this.contig = loc.getContig();
      this.start = (long)loc.getStart();
      this.stop = (long)loc.getStop();
      this.toValidate.add(VariantContext.Validation.ALLELES);
      this.toValidate.add(VariantContext.Validation.REF_PADDING);
      return this;
   }

   @Requires({"contig != null"})
   public VariantContextBuilder chr(String contig) {
      this.contig = contig;
      return this;
   }

   @Requires({"start >= 0"})
   public VariantContextBuilder start(long start) {
      this.start = start;
      this.toValidate.add(VariantContext.Validation.ALLELES);
      this.toValidate.add(VariantContext.Validation.REF_PADDING);
      return this;
   }

   @Requires({"stop >= 0"})
   public VariantContextBuilder stop(long stop) {
      this.stop = stop;
      return this;
   }

   public VariantContext make() {
      return new VariantContext(this.source, this.ID, this.contig, this.start, this.stop, this.alleles, this.genotypes, this.log10PError, this.filters, this.attributes, this.referenceBaseForIndel, this.toValidate);
   }
}
