package org.broadinstitute.sting.utils.variantcontext;

import java.util.EnumMap;
import org.broad.tribble.TribbleException;
import org.broadinstitute.sting.utils.Utils;
import org.broadinstitute.sting.utils.exceptions.ReviewedStingException;
import org.broadinstitute.sting.utils.exceptions.UserException;

public class GenotypeLikelihoods {
   public static final boolean CAP_PLS = false;
   public static final int PL_CAP = 255;
   private double[] log10Likelihoods = null;
   private String likelihoodsAsString_PLs = null;
   public static final int MAX_ALT_ALLELES_THAT_CAN_BE_GENOTYPED = 50;
   private static final GenotypeLikelihoods.GenotypeLikelihoodsAllelePair[] PLIndexToAlleleIndex = calculatePLcache(50);
   protected static int[] PLindexConversion = new int[]{0, 1, 3, 6, 2, 4, 7, 5, 8, 9};

   public static final GenotypeLikelihoods fromPLField(String PLs) {
      return new GenotypeLikelihoods(PLs);
   }

   public static final GenotypeLikelihoods fromGLField(String GLs) {
      return new GenotypeLikelihoods(parseDeprecatedGLString(GLs));
   }

   public static final GenotypeLikelihoods fromLog10Likelihoods(double[] log10Likelihoods) {
      return new GenotypeLikelihoods(log10Likelihoods);
   }

   protected GenotypeLikelihoods(String asString) {
      this.likelihoodsAsString_PLs = asString;
   }

   protected GenotypeLikelihoods(double[] asVector) {
      this.log10Likelihoods = asVector;
   }

   public double[] getAsVector() {
      if (this.log10Likelihoods == null) {
         this.log10Likelihoods = parsePLsIntoLikelihoods(this.likelihoodsAsString_PLs);
      }

      return this.log10Likelihoods;
   }

   public String toString() {
      return this.getAsString();
   }

   public String getAsString() {
      if (this.likelihoodsAsString_PLs == null) {
         if (this.log10Likelihoods == null) {
            throw new TribbleException("BUG: Attempted to get likelihoods as strings and neither the vector nor the string is set!");
         }

         this.likelihoodsAsString_PLs = convertLikelihoodsToPLString(this.log10Likelihoods);
      }

      return this.likelihoodsAsString_PLs;
   }

   public EnumMap getAsMap(boolean normalizeFromLog10) {
      double[] likelihoods = normalizeFromLog10 ? Utils.normalizeFromLog10(this.getAsVector()) : this.getAsVector();
      if (likelihoods == null) {
         return null;
      } else {
         EnumMap likelihoodsMap = new EnumMap(Genotype.Type.class);
         likelihoodsMap.put(Genotype.Type.HOM_REF, likelihoods[Genotype.Type.HOM_REF.ordinal() - 1]);
         likelihoodsMap.put(Genotype.Type.HET, likelihoods[Genotype.Type.HET.ordinal() - 1]);
         likelihoodsMap.put(Genotype.Type.HOM_VAR, likelihoods[Genotype.Type.HOM_VAR.ordinal() - 1]);
         return likelihoodsMap;
      }
   }

   public double getLog10GQ(Genotype.Type genotype) {
      return getQualFromLikelihoods(genotype.ordinal() - 1, this.getAsVector());
   }

   public static double getQualFromLikelihoods(int iOfChoosenGenotype, double[] likelihoods) {
      if (likelihoods == null) {
         return Double.NEGATIVE_INFINITY;
      } else {
         double qual = Double.NEGATIVE_INFINITY;

         for(int i = 0; i < likelihoods.length; ++i) {
            if (i != iOfChoosenGenotype && likelihoods[i] >= qual) {
               qual = likelihoods[i];
            }
         }

         qual = likelihoods[iOfChoosenGenotype] - qual;
         if (qual < 0.0D) {
            double[] normalized = Utils.normalizeFromLog10(likelihoods);
            double chosenGenotype = normalized[iOfChoosenGenotype];
            return Math.log10(1.0D - chosenGenotype);
         } else {
            return -1.0D * qual;
         }
      }
   }

   private static final double[] parsePLsIntoLikelihoods(String likelihoodsAsString_PLs) {
      if (!likelihoodsAsString_PLs.equals(".")) {
         String[] strings = likelihoodsAsString_PLs.split(",");
         double[] likelihoodsAsVector = new double[strings.length];

         try {
            for(int i = 0; i < strings.length; ++i) {
               likelihoodsAsVector[i] = (double)Integer.parseInt(strings[i]) / -10.0D;
            }

            return likelihoodsAsVector;
         } catch (NumberFormatException var4) {
            throw new UserException.MalformedVCF("The GL/PL tag contains non-integer values: " + likelihoodsAsString_PLs);
         }
      } else {
         return null;
      }
   }

   private static final double[] parseDeprecatedGLString(String GLString) {
      if (GLString.equals(".")) {
         return null;
      } else {
         String[] strings = GLString.split(",");
         double[] likelihoodsAsVector = new double[strings.length];

         for(int i = 0; i < strings.length; ++i) {
            likelihoodsAsVector[i] = Double.parseDouble(strings[i]);
         }

         return likelihoodsAsVector;
      }
   }

   private static final String convertLikelihoodsToPLString(double[] GLs) {
      if (GLs == null) {
         return ".";
      } else {
         StringBuilder s = new StringBuilder();
         double adjust = Double.NEGATIVE_INFINITY;

         for (double l : GLs) {
            adjust = Math.max(adjust, l);
         }

         boolean first = true;
         for (double l : GLs) {
            if (!first) {
               s.append(",");
            } else {
               first = false;
            }

            long PL = Math.round(-10.0D * (l - adjust));
            s.append(Long.toString(PL));
         }

         return s.toString();
      }
   }

   private static GenotypeLikelihoods.GenotypeLikelihoodsAllelePair[] calculatePLcache(int altAlleles) {
      int numLikelihoods = calculateNumLikelihoods(1 + altAlleles, 2);
      GenotypeLikelihoods.GenotypeLikelihoodsAllelePair[] cache = new GenotypeLikelihoods.GenotypeLikelihoodsAllelePair[numLikelihoods];

      int i;
      for(i = 0; i <= altAlleles; ++i) {
         for(int allele2 = i; allele2 <= altAlleles; ++allele2) {
            cache[calculatePLindex(i, allele2)] = new GenotypeLikelihoods.GenotypeLikelihoodsAllelePair(i, allele2);
         }
      }

      for(i = 0; i < cache.length; ++i) {
         if (cache[i] == null) {
            throw new ReviewedStingException("BUG: cache entry " + i + " is unexpected null");
         }
      }

      return cache;
   }

   public static int calculateNumLikelihoods(int numAlleles, int ploidy) {
      if (ploidy == 2) {
         return numAlleles * (numAlleles + 1) / 2;
      } else if (numAlleles == 1) {
         return 1;
      } else if (ploidy == 1) {
         return numAlleles;
      } else {
         int acc = 0;

         for(int k = 0; k <= ploidy; ++k) {
            acc += calculateNumLikelihoods(numAlleles - 1, ploidy - k);
         }

         return acc;
      }
   }

   public static int calculatePLindex(int allele1Index, int allele2Index) {
      return allele2Index * (allele2Index + 1) / 2 + allele1Index;
   }

   public static GenotypeLikelihoods.GenotypeLikelihoodsAllelePair getAllelePair(int PLindex) {
      if (PLindex >= PLIndexToAlleleIndex.length) {
         throw new ReviewedStingException("GATK limitation: cannot genotype more than 50 alleles");
      } else {
         return PLIndexToAlleleIndex[PLindex];
      }
   }

   /** @deprecated */
   @Deprecated
   public static GenotypeLikelihoods.GenotypeLikelihoodsAllelePair getAllelePairUsingDeprecatedOrdering(int PLindex) {
      return getAllelePair(PLindexConversion[PLindex]);
   }

   public static int[] getPLIndecesOfAlleles(int allele1Index, int allele2Index) {
      int[] indexes = new int[]{calculatePLindex(allele1Index, allele1Index), calculatePLindex(allele1Index, allele2Index), calculatePLindex(allele2Index, allele2Index)};
      return indexes;
   }

   public static class GenotypeLikelihoodsAllelePair {
      public final int alleleIndex1;
      public final int alleleIndex2;

      public GenotypeLikelihoodsAllelePair(int alleleIndex1, int alleleIndex2) {
         this.alleleIndex1 = alleleIndex1;
         this.alleleIndex2 = alleleIndex2;
      }
   }
}
