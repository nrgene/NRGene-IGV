package org.broadinstitute.sting.utils.variantcontext;

import com.google.java.contract.Ensures;
import com.google.java.contract.Requires;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LazyGenotypesContext extends GenotypesContext {
   final LazyGenotypesContext.LazyParser parser;
   Object unparsedGenotypeData;
   final int nUnparsedGenotypes;
   boolean loaded = false;
   private static final ArrayList EMPTY = new ArrayList(0);

   @Requires({"parser != null", "unparsedGenotypeData != null", "nUnparsedGenotypes >= 0"})
   public LazyGenotypesContext(LazyGenotypesContext.LazyParser parser, Object unparsedGenotypeData, int nUnparsedGenotypes) {
      super(EMPTY);
      this.parser = parser;
      this.unparsedGenotypeData = unparsedGenotypeData;
      this.nUnparsedGenotypes = nUnparsedGenotypes;
   }

   @Ensures({"result != null"})
   protected ArrayList getGenotypes() {
      this.decode();
      return this.notToBeDirectlyAccessedGenotypes;
   }

   public void decode() {
      if (!this.loaded) {
         LazyGenotypesContext.LazyData parsed = this.parser.parse(this.unparsedGenotypeData);
         this.notToBeDirectlyAccessedGenotypes = parsed.genotypes;
         this.sampleNamesInOrder = parsed.sampleNamesInOrder;
         this.sampleNameToOffset = parsed.sampleNameToOffset;
         this.loaded = true;
         this.unparsedGenotypeData = null;
      }

   }

   protected synchronized void ensureSampleNameMap() {
      if (!this.loaded) {
         this.decode();
      } else {
         super.ensureSampleNameMap();
      }

   }

   protected synchronized void ensureSampleOrdering() {
      if (!this.loaded) {
         this.decode();
      } else {
         super.ensureSampleOrdering();
      }

   }

   protected void invalidateSampleNameMap() {
      if (!this.loaded) {
         this.decode();
      }

      super.invalidateSampleNameMap();
   }

   protected void invalidateSampleOrdering() {
      if (!this.loaded) {
         this.decode();
      }

      super.invalidateSampleOrdering();
   }

   public boolean isEmpty() {
      return this.loaded ? super.isEmpty() : this.nUnparsedGenotypes == 0;
   }

   public int size() {
      return this.loaded ? super.size() : this.nUnparsedGenotypes;
   }

   public Object getUnparsedGenotypeData() {
      return this.unparsedGenotypeData;
   }

   public static class LazyData {
      final ArrayList genotypes;
      final Map sampleNameToOffset;
      final List sampleNamesInOrder;

      @Requires({"genotypes != null", "sampleNamesInOrder != null", "sampleNameToOffset != null", "sameSamples(genotypes, sampleNamesInOrder)", "sameSamples(genotypes, sampleNameToOffset.keySet())"})
      public LazyData(ArrayList genotypes, List sampleNamesInOrder, Map sampleNameToOffset) {
         this.genotypes = genotypes;
         this.sampleNamesInOrder = sampleNamesInOrder;
         this.sampleNameToOffset = sampleNameToOffset;
      }
   }

   public interface LazyParser {
//      @Requires({"data != null"})
      @Ensures({"result != null"})
      LazyGenotypesContext.LazyData parse(Object var1);
   }
}
