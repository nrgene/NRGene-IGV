/*
package org.broadinstitute.sting.utils.variantcontext;

import com.google.java.contract.Ensures;
import com.google.java.contract.Requires;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import org.apache.commons.jexl2.Expression;
import org.apache.commons.jexl2.JexlEngine;
import org.apache.log4j.Logger;
import org.broad.tribble.util.popgen.HardyWeinbergCalculation;
import org.broadinstitute.sting.commandline.Hidden;
import org.broadinstitute.sting.utils.BaseUtils;
import org.broadinstitute.sting.utils.GenomeLoc;
import org.broadinstitute.sting.utils.GenomeLocParser;
import org.broadinstitute.sting.utils.Utils;
import org.broadinstitute.sting.utils.BaseUtils.BaseSubstitutionType;
import org.broadinstitute.sting.utils.codecs.vcf.AbstractVCFCodec;
import org.broadinstitute.sting.utils.exceptions.ReviewedStingException;
import org.broadinstitute.sting.utils.exceptions.UserException;

public class VariantContextUtils {
   private static Logger logger = Logger.getLogger(VariantContextUtils.class);
   public static final String MERGE_INTERSECTION = "Intersection";
   public static final String MERGE_FILTER_IN_ALL = "FilteredInAll";
   public static final String MERGE_REF_IN_ALL = "ReferenceInAll";
   public static final String MERGE_FILTER_PREFIX = "filterIn";
   public static final JexlEngine engine = new JexlEngine();
   public static final int DEFAULT_PLOIDY = 2;
   private static final List NO_CALL_ALLELES;
   public static final double SUM_GL_THRESH_NOCALL = -0.1D;

   public static Map calculateChromosomeCounts(VariantContext vc, Map attributes, boolean removeStaleValues) {
      return calculateChromosomeCounts(vc, attributes, removeStaleValues, new HashSet(0));
   }

   public static Map calculateChromosomeCounts(VariantContext vc, Map attributes, boolean removeStaleValues, Set founderIds) {
      int AN = vc.getCalledChrCount();
      if (AN == 0 && removeStaleValues) {
         if (attributes.containsKey("AC")) {
            attributes.remove("AC");
         }

         if (attributes.containsKey("AF")) {
            attributes.remove("AF");
         }

         if (attributes.containsKey("AN")) {
            attributes.remove("AN");
         }

         return attributes;
      } else {
         if (vc.hasGenotypes()) {
            attributes.put("AN", AN);
            if (vc.getAlternateAlleles().size() > 0) {
               ArrayList alleleFreqs = new ArrayList();
               ArrayList alleleCounts = new ArrayList();
               ArrayList foundersAlleleCounts = new ArrayList();
               double totalFoundersChromosomes = (double)vc.getCalledChrCount(founderIds);
               Iterator j = vc.getAlternateAlleles().iterator();

               while(j.hasNext()) {
                  Allele allele = (Allele)j.next();
                  int foundersAltChromosomes = vc.getCalledChrCount(allele, founderIds);
                  alleleCounts.add(vc.getCalledChrCount(allele));
                  foundersAlleleCounts.add(foundersAltChromosomes);
                  if (AN == 0) {
                     alleleFreqs.add("0.0");
                  } else {
                     String freq = String.format(makePrecisionFormatStringFromDenominatorValue(totalFoundersChromosomes), (double)foundersAltChromosomes / totalFoundersChromosomes);
                     alleleFreqs.add(freq);
                  }
               }

               attributes.put("AC", alleleCounts.size() == 1 ? (Serializable)alleleCounts.get(0) : alleleCounts);
               attributes.put("AF", alleleFreqs.size() == 1 ? (Serializable)alleleFreqs.get(0) : alleleFreqs);
            } else {
               attributes.put("AC", 0);
               attributes.put("AF", 0.0D);
            }
         }

         return attributes;
      }
   }

   public static void calculateChromosomeCounts(VariantContextBuilder builder, boolean removeStaleValues) {
      VariantContext vc = builder.make();
      builder.attributes(calculateChromosomeCounts(vc, new HashMap(vc.getAttributes()), removeStaleValues, new HashSet(0)));
   }

   public static void calculateChromosomeCounts(VariantContextBuilder builder, boolean removeStaleValues, Set founderIds) {
      VariantContext vc = builder.make();
      builder.attributes(calculateChromosomeCounts(vc, new HashMap(vc.getAttributes()), removeStaleValues, founderIds));
   }

   public static String makePrecisionFormatStringFromDenominatorValue(double maxValue) {
      int precision;
      for(precision = 1; maxValue > 1.0D; maxValue /= 10.0D) {
         ++precision;
      }

      return "%." + precision + "f";
   }

   public static Genotype removePLs(Genotype g) {
      Map attrs = new HashMap(g.getAttributes());
      attrs.remove("PL");
      attrs.remove("GL");
      return new Genotype(g.getSampleName(), g.getAlleles(), g.getLog10PError(), g.filtersWereApplied() ? g.getFilters() : null, attrs, g.isPhased());
   }

   public static VariantContext createVariantContextWithPaddedAlleles(VariantContext inputVC, boolean refBaseShouldBeAppliedToEndOfAlleles) {
      boolean padVC = false;
      int recordLength = inputVC.getEnd() - inputVC.getStart() + 1;
      int referenceLength = inputVC.getReference().length();
      if (referenceLength == recordLength) {
         padVC = false;
      } else if (referenceLength == recordLength - 1) {
         padVC = true;
      } else if (!inputVC.hasSymbolicAlleles()) {
         throw new IllegalArgumentException("Badly formed variant context at location " + String.valueOf(inputVC.getStart()) + " in contig " + inputVC.getChr() + ". Reference length must be at most one base shorter than location size");
      }

      if (!padVC) {
         return inputVC;
      } else if (!inputVC.hasReferenceBaseForIndel()) {
         throw new ReviewedStingException("Badly formed variant context at location " + inputVC.getChr() + ":" + inputVC.getStart() + "; no padded reference base is available.");
      } else {
         Byte refByte = inputVC.getReferenceBaseForIndel();
         List alleles = new ArrayList();
         Iterator j = inputVC.getAlleles().iterator();

         while(j.hasNext()) {
            Allele a = (Allele)j.next();
            if (a.isSymbolic()) {
               alleles.add(a);
            } else {
               String newBases;
               if (refBaseShouldBeAppliedToEndOfAlleles) {
                  newBases = a.getBaseString() + new String(new byte[]{refByte});
               } else {
                  newBases = new String(new byte[]{refByte}) + a.getBaseString();
               }

               alleles.add(Allele.create(newBases, a.isReference()));
            }
         }

         GenotypesContext genotypes = GenotypesContext.create(inputVC.getNSamples());
         Iterator k = inputVC.getGenotypes().iterator();

         while(k.hasNext()) {
            Genotype g = (Genotype)j.next();
            List inAlleles = g.getAlleles();
            List newGenotypeAlleles = new ArrayList(g.getAlleles().size());

            for (Object inAllele : inAlleles) {
               Allele a = (Allele) inAllele;
               if (a.isCalled()) {
                  if (a.isSymbolic()) {
                     newGenotypeAlleles.add(a);
                  } else {
                     String newBases;
                     if (refBaseShouldBeAppliedToEndOfAlleles) {
                        newBases = a.getBaseString() + new String(new byte[]{refByte});
                     } else {
                        newBases = new String(new byte[]{refByte}) + a.getBaseString();
                     }

                     newGenotypeAlleles.add(Allele.create(newBases, a.isReference()));
                  }
               } else {
                  newGenotypeAlleles.add(Allele.NO_CALL);
               }
            }

            genotypes.add(new Genotype(g.getSampleName(), newGenotypeAlleles, g.getLog10PError(), g.getFilters(), g.getAttributes(), g.isPhased()));
         }

         return (new VariantContextBuilder(inputVC)).alleles(alleles).genotypes(genotypes).make();
      }
   }

   public static List initializeMatchExps(String[] names, String[] exps) {
      if (names != null && exps != null) {
         if (names.length != exps.length) {
            throw new UserException("Inconsistent number of provided filter names and expressions: names=" + Arrays.toString(names) + " exps=" + Arrays.toString(exps));
         } else {
            Map map = new HashMap();

            for(int i = 0; i < names.length; ++i) {
               map.put(names[i], exps[i]);
            }

            return initializeMatchExps(map);
         }
      } else {
         throw new ReviewedStingException("BUG: neither names nor exps can be null: names " + Arrays.toString(names) + " exps=" + Arrays.toString(exps));
      }
   }

   public static List initializeMatchExps(ArrayList names, ArrayList exps) {
      String[] nameArray = new String[names.size()];
      String[] expArray = new String[exps.size()];
      return initializeMatchExps((String[])names.toArray(nameArray), (String[])exps.toArray(expArray));
   }

   public static List initializeMatchExps(Map names_and_exps) {
      List exps = new ArrayList();

      for (Object o : names_and_exps.entrySet()) {
         Entry elt = (Entry) o;
         String name = (String) elt.getKey();
         String expStr = (String) elt.getValue();
         if (name == null || expStr == null) {
            throw new IllegalArgumentException("Cannot create null expressions : " + name + " " + expStr);
         }

         try {
            Expression exp = engine.createExpression(expStr);
            exps.add(new JexlVCMatchExp(name, exp));
         } catch (Exception var7) {
            throw new UserException.BadArgumentValue(name, "Invalid expression used (" + expStr + "). Please see the JEXL docs for correct syntax.");
         }
      }

      return exps;
   }

   public static boolean match(VariantContext vc, VariantContextUtils.JexlVCMatchExp exp) {
      return (Boolean)match(vc, (Collection)Arrays.asList(exp)).get(exp);
   }

   public static Map match(VariantContext vc, Collection exps) {
      return new JEXLMap(exps, vc);
   }

   public static boolean match(VariantContext vc, Genotype g, VariantContextUtils.JexlVCMatchExp exp) {
      return (Boolean)match(vc, g, (Collection)Arrays.asList(exp)).get(exp);
   }

   public static Map match(VariantContext vc, Genotype g, Collection exps) {
      return new JEXLMap(exps, vc, g);
   }

   public static double computeHardyWeinbergPvalue(VariantContext vc) {
      return vc.getCalledChrCount() == 0 ? 0.0D : HardyWeinbergCalculation.hwCalculate(vc.getHomRefCount(), vc.getHetCount(), vc.getHomVarCount());
   }

   @Requires({"vc != null"})
   @Ensures({"result != null"})
   public static VariantContext sitesOnlyVariantContext(VariantContext vc) {
      return (new VariantContextBuilder(vc)).noGenotypes().make();
   }

   @Requires({"vcs != null"})
   @Ensures({"result != null"})
   public static Collection sitesOnlyVariantContexts(Collection vcs) {
      List r = new ArrayList();

      for (Object vc1 : vcs) {
         VariantContext vc = (VariantContext) vc1;
         r.add(sitesOnlyVariantContext(vc));
      }

      return r;
   }

   private static final Map subsetAttributes(CommonInfo igc, Collection keysToPreserve) {
      Map attributes = new HashMap(keysToPreserve.size());

      for (Object aKeysToPreserve : keysToPreserve) {
         String key = (String) aKeysToPreserve;
         if (igc.hasAttribute(key)) {
            attributes.put(key, igc.getAttribute(key));
         }
      }

      return attributes;
   }

   */
/** @deprecated *//*

   @Deprecated
   public static VariantContext pruneVariantContext(VariantContext vc, Collection keysToPreserve) {
      return pruneVariantContext(new VariantContextBuilder(vc), keysToPreserve).make();
   }

   public static VariantContextBuilder pruneVariantContext(VariantContextBuilder builder, Collection keysToPreserve) {
      VariantContext vc = builder.make();
      if (keysToPreserve == null) {
         keysToPreserve = Collections.emptyList();
      }

      Map attributes = subsetAttributes(vc.commonInfo, (Collection)keysToPreserve);
      GenotypesContext genotypes = GenotypesContext.create(vc.getNSamples());
      Iterator j = vc.getGenotypes().iterator();

      while(j.hasNext()) {
         Genotype g = (Genotype)j.next();
         Map genotypeAttributes = subsetAttributes(g.commonInfo, (Collection)keysToPreserve);
         genotypes.add(new Genotype(g.getSampleName(), g.getAlleles(), g.getLog10PError(), g.getFilters(), genotypeAttributes, g.isPhased()));
      }

      return builder.genotypes(genotypes).attributes(attributes);
   }

   public static VariantContext simpleMerge(GenomeLocParser genomeLocParser, Collection unsortedVCs, List priorityListOfVCs, VariantContextUtils.FilteredRecordMergeType filteredRecordMergeType, VariantContextUtils.GenotypeMergeType genotypeMergeOptions, boolean annotateOrigin, boolean printMessages, String setKey, boolean filteredAreUncalled, boolean mergeInfoWithMaxAC) {
      if (unsortedVCs != null && unsortedVCs.size() != 0) {
         if (annotateOrigin && priorityListOfVCs == null) {
            throw new IllegalArgumentException("Cannot merge calls and annotate their origins without a complete priority list of VariantContexts");
         } else {
            if (genotypeMergeOptions == VariantContextUtils.GenotypeMergeType.REQUIRE_UNIQUE) {
               verifyUniqueSampleNames(unsortedVCs);
            }

            List prepaddedVCs = sortVariantContextsByPriority(unsortedVCs, priorityListOfVCs, genotypeMergeOptions);
            List VCs = new ArrayList();
            Iterator j = prepaddedVCs.iterator();

            while(true) {
               VariantContext vc;
               do {
                  if (!j.hasNext()) {
                     if (VCs.size() == 0) {
                        return null;
                     }

                     VariantContext first = (VariantContext)VCs.get(0);
                     String name = first.getSource();
                     Allele refAllele = determineReferenceAllele(VCs);
                     Byte referenceBaseForIndel = null;
                     Set alleles = new LinkedHashSet();
                     Set filters = new TreeSet();
                     Map attributes = new TreeMap();
                     Set inconsistentAttributes = new HashSet();
                     Set variantSources = new HashSet();
                     Set rsIDs = new LinkedHashSet(1);
                     GenomeLoc loc = getLocation(genomeLocParser, first);
                     int depth = 0;
                     int maxAC = -1;
                     Map attributesWithMaxAC = new TreeMap();
                     double log10PError = 1.0D;
                     VariantContext vcWithMaxAC = null;
                     GenotypesContext genotypes = GenotypesContext.create();
                     int nFiltered = 0;
                     boolean remapped = false;
                     Iterator k = VCs.iterator();

                     label245:
                     while(k.hasNext()) {
                        vc = (VariantContext)k.next();
                        if (loc.getStart() != vc.getStart()) {
                           throw new ReviewedStingException("BUG: attempting to merge VariantContexts with different start sites: first=" + first.toString() + " second=" + vc.toString());
                        }

                        if (getLocation(genomeLocParser, vc).size() > loc.size()) {
                           loc = getLocation(genomeLocParser, vc);
                        }

                        nFiltered += vc.isFiltered() ? 1 : 0;
                        if (vc.isVariant()) {
                           variantSources.add(vc.getSource());
                        }

                        VariantContextUtils.AlleleMapper alleleMapping = resolveIncompatibleAlleles(refAllele, vc, alleles);
                        remapped = remapped || alleleMapping.needsRemapping();
                        alleles.addAll(alleleMapping.values());
                        mergeGenotypes(genotypes, vc, alleleMapping, genotypeMergeOptions == VariantContextUtils.GenotypeMergeType.UNIQUIFY);
                        log10PError = Math.min(log10PError, vc.isVariant() ? vc.getLog10PError() : 1.0D);
                        filters.addAll(vc.getFilters());
                        if (referenceBaseForIndel == null) {
                           referenceBaseForIndel = vc.getReferenceBaseForIndel();
                        }

                        if (vc.hasAttribute("DP")) {
                           depth += vc.getAttributeAsInt("DP", 0);
                        }

                        if (vc.hasID()) {
                           rsIDs.add(vc.getID());
                        }

                        if (mergeInfoWithMaxAC && vc.hasAttribute("AC")) {
                           String rawAlleleCounts = vc.getAttributeAsString("AC", (String)null);
                           if (rawAlleleCounts.contains(",")) {
                              List alleleCountArray = Arrays.asList(rawAlleleCounts.substring(1, rawAlleleCounts.length() - 1).split(","));

                              for (Object anAlleleCountArray : alleleCountArray) {
                                 String alleleCount = (String) anAlleleCountArray;
                                 int ac = Integer.valueOf(alleleCount.trim());
                                 if (ac > maxAC) {
                                    maxAC = ac;
                                    vcWithMaxAC = vc;
                                 }
                              }
                           } else {
                              int ac = Integer.valueOf(rawAlleleCounts);
                              if (ac > maxAC) {
                                 maxAC = ac;
                                 vcWithMaxAC = vc;
                              }
                           }
                        }

                        Iterator j1 = vc.getAttributes().entrySet().iterator();

                        while(true) {
                           while(true) {
                              Entry p;
                              String key;
                              do {
                                 if (!j1.hasNext()) {
                                    continue label245;
                                 }

                                 p = (Entry)j1.next();
                                 key = (String)p.getKey();
                              } while(inconsistentAttributes.contains(key));

                              boolean alreadyFound = attributes.containsKey(key);
                              Object boundValue = attributes.get(key);
                              boolean boundIsMissingValue = alreadyFound && boundValue.equals(".");
                              if (alreadyFound && !boundValue.equals(p.getValue()) && !boundIsMissingValue) {
                                 inconsistentAttributes.add(key);
                                 attributes.remove(key);
                              } else if (!alreadyFound || boundIsMissingValue) {
                                 attributes.put(key, p.getValue());
                              }
                           }
                        }
                     }

                     j = VCs.iterator();

                     while(j.hasNext()) {
                        vc = (VariantContext)j.next();
                        if (vc.alleles.size() != 1 && hasPLIncompatibleAlleles(alleles, vc.alleles)) {
                           if (!genotypes.isEmpty()) {
                              logger.debug(String.format("Stripping PLs at %s due incompatible alleles merged=%s vs. single=%s", genomeLocParser.createGenomeLoc(vc), alleles, vc.alleles));
                           }

                           genotypes = stripPLs(genotypes);
                           calculateChromosomeCounts(vc, attributes, true);
                           break;
                        }
                     }

                     if (mergeInfoWithMaxAC && vcWithMaxAC != null) {
                        attributesWithMaxAC.putAll(vcWithMaxAC.getAttributes());
                     }

                     if (filteredRecordMergeType == VariantContextUtils.FilteredRecordMergeType.KEEP_IF_ANY_UNFILTERED && nFiltered != VCs.size() || filteredRecordMergeType == VariantContextUtils.FilteredRecordMergeType.KEEP_UNCONDITIONAL) {
                        filters.clear();
                     }

                     String setValue;
                     if (annotateOrigin) {
                        if (nFiltered == 0 && variantSources.size() == priorityListOfVCs.size()) {
                           setValue = "Intersection";
                        } else if (nFiltered == VCs.size()) {
                           setValue = "FilteredInAll";
                        } else if (variantSources.isEmpty()) {
                           setValue = "ReferenceInAll";
                        } else {
                           LinkedHashSet s = new LinkedHashSet();

                           for (Object VC : VCs) {
                              VariantContext vc1 = (VariantContext) VC;
                              if (vc1.isVariant()) {
                                 s.add(vc1.isFiltered() ? "filterIn" + vc1.getSource() : vc1.getSource());
                              }
                           }

                           setValue = Utils.join("-", s);
                        }

                        if (setKey != null) {
                           attributes.put(setKey, setValue);
                           if (mergeInfoWithMaxAC && vcWithMaxAC != null) {
                              attributesWithMaxAC.put(setKey, vcWithMaxAC.getSource());
                           }
                        }
                     }

                     if (depth > 0) {
                        attributes.put("DP", String.valueOf(depth));
                     }

                     setValue = rsIDs.isEmpty() ? "." : Utils.join(",", rsIDs);
                     VariantContextBuilder builder = (new VariantContextBuilder()).source(name).id(setValue);
                     builder.loc(loc.getContig(), (long)loc.getStart(), (long)loc.getStop());
                     builder.alleles(alleles);
                     builder.genotypes(genotypes);
                     builder.log10PError(log10PError);
                     builder.filters((Set)filters).attributes(mergeInfoWithMaxAC ? attributesWithMaxAC : attributes);
                     builder.referenceBaseForIndel(referenceBaseForIndel);
                     VariantContext merged = createVariantContextWithTrimmedAlleles(builder.make());
                     if (printMessages && remapped) {
                        System.out.printf("Remapped => %s%n", merged);
                     }

                     return merged;
                  }

                  vc = (VariantContext)j.next();
               } while(filteredAreUncalled && !vc.isNotFiltered());

               VCs.add(createVariantContextWithPaddedAlleles(vc, false));
            }
         }
      } else {
         return null;
      }
   }

   private static final boolean hasPLIncompatibleAlleles(Collection alleleSet1, Collection alleleSet2) {
      Iterator it1 = alleleSet1.iterator();
      Iterator it2 = alleleSet2.iterator();

      while(it1.hasNext() && it2.hasNext()) {
         Allele a1 = (Allele)it1.next();
         Allele a2 = (Allele)it2.next();
         if (!a1.equals(a2)) {
            return true;
         }
      }

      return it1.hasNext() || it2.hasNext();
   }

   public static boolean allelesAreSubset(VariantContext vc1, VariantContext vc2) {
      if (!vc1.getReference().equals(vc2.getReference())) {
         return false;
      } else {
         Iterator j = vc1.getAlternateAlleles().iterator();

         Allele a;
         do {
            if (!j.hasNext()) {
               return true;
            }

            a = (Allele)j.next();
         } while(vc2.getAlternateAlleles().contains(a));

         return false;
      }
   }

   public static VariantContext createVariantContextWithTrimmedAlleles(VariantContext inputVC) {
      Allele refAllele = inputVC.getReference();
      boolean trimVC;
      if (!inputVC.isVariant()) {
         trimVC = false;
      } else if (refAllele.isNull()) {
         trimVC = false;
      } else {
         trimVC = AbstractVCFCodec.computeForwardClipping(inputVC.getAlternateAlleles(), (byte)inputVC.getReference().getDisplayString().charAt(0)) > 0;
      }

      if (!trimVC) {
         return inputVC;
      } else {
         List alleles = new ArrayList();
         GenotypesContext genotypes = GenotypesContext.create();
         Map originalToTrimmedAlleleMap = new HashMap();
         Iterator j = inputVC.getAlleles().iterator();

         while(j.hasNext()) {
            Allele a = (Allele)j.next();
            if (a.isSymbolic()) {
               alleles.add(a);
               originalToTrimmedAlleleMap.put(a, a);
            } else {
               byte[] newBases = Arrays.copyOfRange(a.getBases(), 1, a.length());
               Allele trimmedAllele = Allele.create(newBases, a.isReference());
               alleles.add(trimmedAllele);
               originalToTrimmedAlleleMap.put(a, trimmedAllele);
            }
         }

         boolean hasNullAlleles = false;

         for (Object o : originalToTrimmedAlleleMap.values()) {
            Allele a = (Allele) o;
            if (a.isNull()) {
               hasNullAlleles = true;
            }
         }

         if (!hasNullAlleles) {
            return inputVC;
         } else {
            j = inputVC.getGenotypes().iterator();

            while(j.hasNext()) {
               Genotype genotype = (Genotype)j.next();
               List originalAlleles = genotype.getAlleles();
               List trimmedAlleles = new ArrayList();

               for (Object originalAllele : originalAlleles) {
                  Allele a = (Allele) originalAllele;
                  if (a.isCalled()) {
                     trimmedAlleles.add(originalToTrimmedAlleleMap.get(a));
                  } else {
                     trimmedAlleles.add(Allele.NO_CALL);
                  }
               }

               genotypes.add(Genotype.modifyAlleles(genotype, trimmedAlleles));
            }

            VariantContextBuilder builder = new VariantContextBuilder(inputVC);
            return builder.alleles(alleles).genotypes(genotypes).referenceBaseForIndel(new Byte(inputVC.getReference().getBases()[0])).make();
         }
      }
   }

   public static VariantContext reverseTrimAlleles(VariantContext inputVC) {
      int trimExtent = AbstractVCFCodec.computeReverseClipping(inputVC.getAlleles(), inputVC.getReference().getDisplayString().getBytes(), 0, true, -1);
      if (trimExtent > 0 && inputVC.getAlleles().size() > 1) {
         List alleles = new ArrayList();
         GenotypesContext genotypes = GenotypesContext.create();
         Map originalToTrimmedAlleleMap = new HashMap();
         Iterator j = inputVC.getAlleles().iterator();

         while(j.hasNext()) {
            Allele a = (Allele)j.next();
            if (a.isSymbolic()) {
               alleles.add(a);
               originalToTrimmedAlleleMap.put(a, a);
            } else {
               byte[] newBases = Arrays.copyOfRange(a.getBases(), 0, a.length() - trimExtent);
               Allele trimmedAllele = Allele.create(newBases, a.isReference());
               alleles.add(trimmedAllele);
               originalToTrimmedAlleleMap.put(a, trimmedAllele);
            }
         }

         j = inputVC.getGenotypes().iterator();

         while(j.hasNext()) {
            Genotype genotype = (Genotype)j.next();
            List originalAlleles = genotype.getAlleles();
            List trimmedAlleles = new ArrayList();

            for (Object originalAllele : originalAlleles) {
               Allele a = (Allele) originalAllele;
               if (a.isCalled()) {
                  trimmedAlleles.add(originalToTrimmedAlleleMap.get(a));
               } else {
                  trimmedAlleles.add(Allele.NO_CALL);
               }
            }

            genotypes.add(Genotype.modifyAlleles(genotype, trimmedAlleles));
         }

         return (new VariantContextBuilder(inputVC)).stop((long)(inputVC.getStart() + ((Allele)alleles.get(0)).length() + (inputVC.isMixed() ? -1 : 0))).alleles(alleles).genotypes(genotypes).make();
      } else {
         return inputVC;
      }
   }

   public static GenotypesContext stripPLs(GenotypesContext genotypes) {
      GenotypesContext newGs = GenotypesContext.create(genotypes.size());
      Iterator j = genotypes.iterator();

      while(j.hasNext()) {
         Genotype g = (Genotype)j.next();
         newGs.add(g.hasLikelihoods() ? removePLs(g) : g);
      }

      return newGs;
   }

   public static Map separateVariantContextsByType(Collection VCs) {
      HashMap mappedVCs = new HashMap();

      for (Object VC : VCs) {
         VariantContext vc = (VariantContext) VC;
         boolean addtoOwnList = true;

         for (int j1 = 0; j1 < VariantContext.Type.values().length; ++j1) {
            VariantContext.Type type = VariantContext.Type.values()[j1];
            if (!type.equals(vc.getType()) && mappedVCs.containsKey(type)) {
               List vcList = (List) mappedVCs.get(type);

               for (int k = 0; k < vcList.size(); ++k) {
                  VariantContext otherVC = (VariantContext) vcList.get(k);
                  if (allelesAreSubset(otherVC, vc)) {
                     vcList.remove(k);
                     if (vcList.size() == 0) {
                        mappedVCs.remove(vcList);
                     }

                     if (!mappedVCs.containsKey(vc.getType())) {
                        mappedVCs.put(vc.getType(), new ArrayList());
                     }

                     ((List) mappedVCs.get(vc.getType())).add(otherVC);
                     break;
                  }

                  if (allelesAreSubset(vc, otherVC)) {
                     ((List) mappedVCs.get(type)).add(vc);
                     addtoOwnList = false;
                     break;
                  }
               }
            }
         }

         if (addtoOwnList) {
            if (!mappedVCs.containsKey(vc.getType())) {
               mappedVCs.put(vc.getType(), new ArrayList());
            }

            ((List) mappedVCs.get(vc.getType())).add(vc);
         }
      }

      return mappedVCs;
   }

   private static void verifyUniqueSampleNames(Collection unsortedVCs) {
      Set names = new HashSet();

      for (Object unsortedVC : unsortedVCs) {
         VariantContext vc = (VariantContext) unsortedVC;
         for (Object o : vc.getSampleNames()) {
            String name = (String) o;
            if (names.contains(name)) {
               throw new UserException("REQUIRE_UNIQUE sample names is true but duplicate names were discovered " + name);
            }
         }

         names.addAll(vc.getSampleNames());
      }

   }

   private static Allele determineReferenceAllele(List VCs) {
      Allele ref = null;
      Iterator j = VCs.iterator();

      VariantContext vc;
      Allele myRef;
      label24:
      do {
         while(j.hasNext()) {
            vc = (VariantContext)j.next();
            myRef = vc.getReference();
            if (ref != null && ref.length() >= myRef.length()) {
               continue label24;
            }

            ref = myRef;
         }

         return ref;
      } while(ref.length() != myRef.length() || ref.equals(myRef));

      throw new UserException.BadInput(String.format("The provided variant file(s) have inconsistent references for the same position(s) at %s:%d, %s vs. %s", vc.getChr(), vc.getStart(), ref, myRef));
   }

   private static VariantContextUtils.AlleleMapper resolveIncompatibleAlleles(Allele refAllele, VariantContext vc, Set allAlleles) {
      if (refAllele.equals(vc.getReference())) {
         return new VariantContextUtils.AlleleMapper(vc);
      } else {
         Allele myRef = vc.getReference();
         if (refAllele.length() <= myRef.length()) {
            throw new ReviewedStingException("BUG: myRef=" + myRef + " is longer than refAllele=" + refAllele);
         } else {
            byte[] extraBases = Arrays.copyOfRange(refAllele.getBases(), myRef.length(), refAllele.length());
            Map map = new HashMap();
            Iterator j = vc.getAlleles().iterator();

            while(true) {
               while(j.hasNext()) {
                  Allele a = (Allele)j.next();
                  if (a.isReference()) {
                     map.put(a, refAllele);
                  } else {
                     Allele extended = Allele.extend(a, extraBases);

                     for (Object allAllele : allAlleles) {
                        Allele b = (Allele) allAllele;
                        if (extended.equals(b)) {
                           extended = b;
                        }
                     }

                     map.put(a, extended);
                  }
               }

               return new VariantContextUtils.AlleleMapper(map);
            }
         }
      }
   }

   public static List sortVariantContextsByPriority(Collection unsortedVCs, List priorityListOfVCs, VariantContextUtils.GenotypeMergeType mergeOption) {
      if (mergeOption == VariantContextUtils.GenotypeMergeType.PRIORITIZE && priorityListOfVCs == null) {
         throw new IllegalArgumentException("Cannot merge calls by priority with a null priority list");
      } else if (priorityListOfVCs != null && mergeOption != VariantContextUtils.GenotypeMergeType.UNSORTED) {
         ArrayList sorted = new ArrayList(unsortedVCs);
         Collections.sort(sorted, new VariantContextUtils.CompareByPriority(priorityListOfVCs));
         return sorted;
      } else {
         return new ArrayList(unsortedVCs);
      }
   }

   private static void mergeGenotypes(GenotypesContext mergedGenotypes, VariantContext oneVC, VariantContextUtils.AlleleMapper alleleMapping, boolean uniqifySamples) {
      Iterator j = oneVC.getGenotypes().iterator();

      while(true) {
         Genotype g;
         String name;
         do {
            if (!j.hasNext()) {
               return;
            }

            g = (Genotype)j.next();
            name = mergedSampleName(oneVC.getSource(), g.getSampleName(), uniqifySamples);
         } while(mergedGenotypes.containsSample(name));

         Genotype newG = g;
         if (uniqifySamples || alleleMapping.needsRemapping()) {
            List alleles = alleleMapping.needsRemapping() ? alleleMapping.remap(g.getAlleles()) : g.getAlleles();
            newG = new Genotype(name, alleles, g.getLog10PError(), g.getFilters(), g.getAttributes(), g.isPhased());
         }

         mergedGenotypes.add(newG);
      }
   }

   public static String mergedSampleName(String trackName, String sampleName, boolean uniqify) {
      return uniqify ? sampleName + "." + trackName : sampleName;
   }

   public static VariantContext reverseComplement(VariantContext vc) {
      HashMap alleleMap = new HashMap(vc.getAlleles().size());

      Allele originalAllele;
      Allele newAllele;
      for(Iterator j = vc.getAlleles().iterator(); j.hasNext(); alleleMap.put(originalAllele, newAllele)) {
         originalAllele = (Allele)j.next();
         if (!originalAllele.isNoCall() && !originalAllele.isNull()) {
            newAllele = Allele.create(BaseUtils.simpleReverseComplement(originalAllele.getBases()), originalAllele.isReference());
         } else {
            newAllele = originalAllele;
         }
      }

      GenotypesContext newGenotypes = GenotypesContext.create(vc.getNSamples());

      for (Object o : vc.getGenotypes()) {
         Genotype genotype = (Genotype) o;
         List newAlleles = new ArrayList();

         Allele newAllele;
         for (Iterator j = genotype.getAlleles().iterator(); j.hasNext(); newAlleles.add(newAllele)) {
            Allele allele = (Allele) j.next();
            newAllele = (Allele) alleleMap.get(allele);
            if (newAllele == null) {
               newAllele = Allele.NO_CALL;
            }
         }

         newGenotypes.add(Genotype.modifyAlleles(genotype, newAlleles));
      }

      return (new VariantContextBuilder(vc)).alleles(alleleMap.values()).genotypes(newGenotypes).make();
   }

   public static VariantContext purgeUnallowedGenotypeAttributes(VariantContext vc, Set allowedAttributes) {
      if (allowedAttributes == null) {
         return vc;
      } else {
         GenotypesContext newGenotypes = GenotypesContext.create(vc.getNSamples());

         for (Object o : vc.getGenotypes()) {
            Genotype genotype = (Genotype) o;
            Map attrs = new HashMap();

            for (Object o1 : genotype.getAttributes().entrySet()) {
               Entry attr = (Entry) o1;
               if (allowedAttributes.contains(attr.getKey())) {
                  attrs.put(attr.getKey(), attr.getValue());
               }
            }

            newGenotypes.add(Genotype.modifyAttributes(genotype, attrs));
         }

         return (new VariantContextBuilder(vc)).genotypes(newGenotypes).make();
      }
   }

   public static BaseSubstitutionType getSNPSubstitutionType(VariantContext context) {
      if (context.isSNP() && context.isBiallelic()) {
         return BaseUtils.SNPSubstitutionType(context.getReference().getBases()[0], context.getAlternateAllele(0).getBases()[0]);
      } else {
         throw new IllegalStateException("Requested SNP substitution type for bialleic non-SNP " + context);
      }
   }

   public static boolean isTransition(VariantContext context) {
      return getSNPSubstitutionType(context) == BaseSubstitutionType.TRANSITION;
   }

   public static boolean isTransversion(VariantContext context) {
      return getSNPSubstitutionType(context) == BaseSubstitutionType.TRANSVERSION;
   }

   public static boolean isTransition(Allele ref, Allele alt) {
      return BaseUtils.SNPSubstitutionType(ref.getBases()[0], alt.getBases()[0]) == BaseSubstitutionType.TRANSITION;
   }

   public static boolean isTransversion(Allele ref, Allele alt) {
      return BaseUtils.SNPSubstitutionType(ref.getBases()[0], alt.getBases()[0]) == BaseSubstitutionType.TRANSVERSION;
   }

   public static final GenomeLoc getLocation(GenomeLocParser genomeLocParser, VariantContext vc) {
      return genomeLocParser.createGenomeLoc(vc.getChr(), vc.getStart(), vc.getEnd(), true);
   }

   public static final Set genotypeNames(Collection genotypes) {
      Set names = new HashSet(genotypes.size());
      Iterator j = genotypes.iterator();

      while(j.hasNext()) {
         Genotype g = (Genotype)j.next();
         names.add(g.getSampleName());
      }

      return names;
   }

   public static GenotypesContext assignDiploidGenotypes(VariantContext vc) {
      return subsetDiploidAlleles(vc, vc.getAlleles(), true);
   }

   public static GenotypesContext subsetDiploidAlleles(VariantContext vc, List allelesToUse, boolean assignGenotypes) {
      GenotypesContext oldGTs = vc.getGenotypes();
      List sampleIndices = oldGTs.getSampleNamesOrderedByName();
      GenotypesContext newGTs = GenotypesContext.create();
      int numOriginalAltAlleles = vc.getAlternateAlleles().size();
      int numNewAltAlleles = allelesToUse.size() - 1;
      ArrayList likelihoodIndexesToUse = null;
      if (numNewAltAlleles != numOriginalAltAlleles && numNewAltAlleles > 0) {
         likelihoodIndexesToUse = new ArrayList(30);
         boolean[] altAlleleIndexToUse = new boolean[numOriginalAltAlleles];

         int numLikelihoods;
         for(numLikelihoods = 0; numLikelihoods < numOriginalAltAlleles; ++numLikelihoods) {
            if (allelesToUse.contains(vc.getAlternateAllele(numLikelihoods))) {
               altAlleleIndexToUse[numLikelihoods] = true;
            }
         }

         numLikelihoods = GenotypeLikelihoods.calculateNumLikelihoods(1 + numOriginalAltAlleles, 2);

         for(int PLindex = 0; PLindex < numLikelihoods; ++PLindex) {
            GenotypeLikelihoods.GenotypeLikelihoodsAllelePair alleles = GenotypeLikelihoods.getAllelePair(PLindex);
            if ((alleles.alleleIndex1 == 0 || altAlleleIndexToUse[alleles.alleleIndex1 - 1]) && (alleles.alleleIndex2 == 0 || altAlleleIndexToUse[alleles.alleleIndex2 - 1])) {
               likelihoodIndexesToUse.add(PLindex);
            }
         }
      }

      for(int k = 0; k < oldGTs.size(); ++k) {
         Genotype g = oldGTs.get((String)sampleIndices.get(k));
         if (!g.hasLikelihoods()) {
            newGTs.add(new Genotype(g.getSampleName(), NO_CALL_ALLELES, 1.0D, (Set)null, (Map)null, false));
         } else {
            double[] originalLikelihoods = g.getLikelihoods().getAsVector();
            double[] newLikelihoods;
            if (likelihoodIndexesToUse == null) {
               newLikelihoods = originalLikelihoods;
            } else {
               newLikelihoods = new double[likelihoodIndexesToUse.size()];
               int newIndex = 0;

               int oldIndex;
               for(Iterator j = likelihoodIndexesToUse.iterator(); j.hasNext(); newLikelihoods[newIndex++] = originalLikelihoods[oldIndex]) {
                  oldIndex = (Integer)j.next();
               }

               newLikelihoods = Utils.normalizeFromLog10(newLikelihoods, false, true);
            }

            if (Utils.sum(newLikelihoods) > -0.1D) {
               newGTs.add(new Genotype(g.getSampleName(), NO_CALL_ALLELES, 1.0D, (Set)null, (Map)null, false));
            } else {
               Map attrs = new HashMap(g.getAttributes());
               if (numNewAltAlleles == 0) {
                  attrs.remove("PL");
               } else {
                  attrs.put("PL", GenotypeLikelihoods.fromLog10Likelihoods(newLikelihoods));
               }

               if (assignGenotypes && Utils.sum(newLikelihoods) <= -0.1D) {
                  newGTs.add(assignDiploidGenotype(g, newLikelihoods, allelesToUse, attrs));
               } else {
                  newGTs.add(new Genotype(g.getSampleName(), NO_CALL_ALLELES, 1.0D, (Set)null, attrs, false));
               }
            }
         }
      }

      return newGTs;
   }

   private static Genotype assignDiploidGenotype(Genotype originalGT, double[] newLikelihoods, List allelesToUse, Map attrs) {
      int numNewAltAlleles = allelesToUse.size() - 1;
      int PLindex = numNewAltAlleles == 0 ? 0 : Utils.maxElementIndex(newLikelihoods);
      GenotypeLikelihoods.GenotypeLikelihoodsAllelePair alleles = GenotypeLikelihoods.getAllelePair(PLindex);
      ArrayList myAlleles = new ArrayList();
      myAlleles.add(allelesToUse.get(alleles.alleleIndex1));
      myAlleles.add(allelesToUse.get(alleles.alleleIndex2));
      double qual = numNewAltAlleles == 0 ? 1.0D : GenotypeLikelihoods.getQualFromLikelihoods(PLindex, newLikelihoods);
      return new Genotype(originalGT.getSampleName(), myAlleles, qual, (Set)null, attrs, false);
   }

   @Requires({"vc != null", "refBasesStartingAtVCWithPad != null && refBasesStartingAtVCWithPad.length > 0"})
   public static boolean isTandemRepeat(VariantContext vc, byte[] refBasesStartingAtVCWithPad) {
      String refBasesStartingAtVCWithoutPad = (new String(refBasesStartingAtVCWithPad)).substring(1);
      if (!vc.isIndel()) {
         return false;
      } else {
         Allele ref = vc.getReference();
         Iterator j = vc.getAlternateAlleles().iterator();

         Allele allele;
         do {
            if (!j.hasNext()) {
               return true;
            }

            allele = (Allele)j.next();
         } while(isRepeatAllele(ref, allele, refBasesStartingAtVCWithoutPad));

         return false;
      }
   }

   protected static boolean isRepeatAllele(Allele ref, Allele alt, String refBasesStartingAtVCWithoutPad) {
      if (!Allele.oneIsPrefixOfOther(ref, alt)) {
         return false;
      } else {
         return ref.length() > alt.length() ? basesAreRepeated(ref.getBaseString(), alt.getBaseString(), refBasesStartingAtVCWithoutPad, 2) : basesAreRepeated(alt.getBaseString(), ref.getBaseString(), refBasesStartingAtVCWithoutPad, 1);
      }
   }

   protected static boolean basesAreRepeated(String l, String s, String ref, int minNumberOfMatches) {
      String potentialRepeat = l.substring(s.length());

      for(int i = 0; i < minNumberOfMatches; ++i) {
         int start = i * potentialRepeat.length();
         int end = (i + 1) * potentialRepeat.length();
         if (ref.length() < end) {
            return false;
         }

         String refSub = ref.substring(start, end);
         if (!refSub.equals(potentialRepeat)) {
            return false;
         }
      }

      return true;
   }

   static {
      engine.setSilent(false);
      engine.setLenient(false);
      NO_CALL_ALLELES = Arrays.asList(Allele.NO_CALL, Allele.NO_CALL);
   }

   static class CompareByPriority implements Comparator, Serializable {
      List priorityListOfVCs;

      public CompareByPriority(List priorityListOfVCs) {
         this.priorityListOfVCs = priorityListOfVCs;
      }

      private int getIndex(VariantContext vc) {
         int i = this.priorityListOfVCs.indexOf(vc.getSource());
         if (i == -1) {
            throw new UserException.BadArgumentValue(Utils.join(",", this.priorityListOfVCs), "Priority list " + this.priorityListOfVCs + " doesn't contain variant context " + vc.getSource());
         } else {
            return i;
         }
      }

      public int compare(VariantContext vc1, VariantContext vc2) {
         return Integer.valueOf(this.getIndex(vc1)).compareTo(this.getIndex(vc2));
      }
   }

   private static class AlleleMapper {
      private VariantContext vc = null;
      private Map map = null;

      public AlleleMapper(VariantContext vc) {
         this.vc = vc;
      }

      public AlleleMapper(Map map) {
         this.map = map;
      }

      public boolean needsRemapping() {
         return this.map != null;
      }

      public Collection values() {
         return (Collection)(this.map != null ? this.map.values() : this.vc.getAlleles());
      }

      public Allele remap(Allele a) {
         return this.map != null && this.map.containsKey(a) ? (Allele)this.map.get(a) : a;
      }

      public List remap(List as) {
         List newAs = new ArrayList();
         Iterator j = as.iterator();

         while(j.hasNext()) {
            Allele a = (Allele)j.next();
            newAs.add(this.remap(a));
         }

         return newAs;
      }
   }

   @Hidden
   public static enum MultipleAllelesMergeType {
      BY_TYPE,
      MIX_TYPES;
   }

   public static enum FilteredRecordMergeType {
      KEEP_IF_ANY_UNFILTERED,
      KEEP_IF_ALL_UNFILTERED,
      KEEP_UNCONDITIONAL;
   }

   public static enum GenotypeMergeType {
      UNIQUIFY,
      PRIORITIZE,
      UNSORTED,
      REQUIRE_UNIQUE;
   }

   public static class JexlVCMatchExp {
      public String name;
      public Expression exp;

      public JexlVCMatchExp(String name, Expression exp) {
         this.name = name;
         this.exp = exp;
      }
   }
}
*/
