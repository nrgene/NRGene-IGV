/*
package org.broadinstitute.sting.utils.codecs.vcf;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;
import net.sf.samtools.SAMSequenceDictionary;
import org.broad.tribble.TribbleException.InternalCodecException;
import org.broad.tribble.util.ParsingUtils;
import org.broadinstitute.sting.utils.exceptions.ReviewedStingException;
import org.broadinstitute.sting.utils.variantcontext.Allele;
import org.broadinstitute.sting.utils.variantcontext.Genotype;
import org.broadinstitute.sting.utils.variantcontext.GenotypesContext;
import org.broadinstitute.sting.utils.variantcontext.LazyGenotypesContext;
import org.broadinstitute.sting.utils.variantcontext.VariantContext;
import org.broadinstitute.sting.utils.variantcontext.VariantContextBuilder;
import org.broadinstitute.sting.utils.variantcontext.VariantContextUtils;

public class StandardVCFWriter extends IndexingVCFWriter {
   protected final BufferedWriter mWriter;
   protected final boolean doNotWriteGenotypes;
   protected VCFHeader mHeader;
   protected boolean filtersWereAppliedToContext;

   public StandardVCFWriter(File location, SAMSequenceDictionary refDict) {
      this(location, openOutputStream(location), refDict, true, false);
   }

   public StandardVCFWriter(File location, SAMSequenceDictionary refDict, boolean enableOnTheFlyIndexing) {
      this(location, openOutputStream(location), refDict, enableOnTheFlyIndexing, false);
   }

   public StandardVCFWriter(OutputStream output, SAMSequenceDictionary refDict, boolean doNotWriteGenotypes) {
      this((File)null, output, refDict, false, doNotWriteGenotypes);
   }

   public StandardVCFWriter(File location, OutputStream output, SAMSequenceDictionary refDict, boolean enableOnTheFlyIndexing, boolean doNotWriteGenotypes) {
      super(writerName(location, output), location, output, refDict, enableOnTheFlyIndexing);
      this.mHeader = null;
      this.filtersWereAppliedToContext = false;
      this.mWriter = new BufferedWriter(new OutputStreamWriter(this.getOutputStream()));
      this.doNotWriteGenotypes = doNotWriteGenotypes;
   }

   public void writeHeader(VCFHeader header) {
      this.mHeader = this.doNotWriteGenotypes ? new VCFHeader(header.getMetaData()) : header;

      try {
         this.mWriter.write("##" + VCFHeaderVersion.VCF4_1.getFormatString() + "=" + VCFHeaderVersion.VCF4_1.getVersionString() + "\n");
         Iterator j = this.mHeader.getMetaData().iterator();

         while(j.hasNext()) {
            VCFHeaderLine line = (VCFHeaderLine)j.next();
            if (!VCFHeaderVersion.isFormatString(line.getKey())) {
               if (line instanceof VCFFilterHeaderLine) {
                  this.filtersWereAppliedToContext = true;
               }

               this.mWriter.write("##");
               this.mWriter.write(line.toString());
               this.mWriter.write("\n");
            }
         }

         this.mWriter.write("#");
         j = this.mHeader.getHeaderFields().iterator();

         while(j.hasNext()) {
            VCFHeader.HEADER_FIELDS field = (VCFHeader.HEADER_FIELDS)j.next();
            this.mWriter.write(field.toString());
            this.mWriter.write("\t");
         }

         if (this.mHeader.hasGenotypingData()) {
            this.mWriter.write("FORMAT");
            j = this.mHeader.getGenotypeSamples().iterator();

            while(j.hasNext()) {
               String sample = (String)j.next();
               this.mWriter.write("\t");
               this.mWriter.write(sample);
            }
         }

         this.mWriter.write("\n");
         this.mWriter.flush();
      } catch (IOException var4) {
         throw new ReviewedStingException("IOException writing the VCF header to " + this.getStreamName(), var4);
      }
   }

   public void close() {
      try {
         this.mWriter.flush();
         this.mWriter.close();
      } catch (IOException var2) {
         throw new ReviewedStingException("Unable to close " + this.getStreamName(), var2);
      }

      super.close();
   }

   public void add(VariantContext vc) {
      if (this.mHeader == null) {
         throw new IllegalStateException("The VCF Header must be written before records can be added: " + this.getStreamName());
      } else {
         if (this.doNotWriteGenotypes) {
            vc = (new VariantContextBuilder(vc)).noGenotypes().make();
         }

         try {
            vc = VariantContextUtils.createVariantContextWithPaddedAlleles(vc, false);
            super.add(vc);
            Map alleleMap = new HashMap(vc.getAlleles().size());
            alleleMap.put(Allele.NO_CALL, ".");
            this.mWriter.write(vc.getChr());
            this.mWriter.write("\t");
            this.mWriter.write(String.valueOf(vc.getStart()));
            this.mWriter.write("\t");
            String ID = vc.getID();
            this.mWriter.write(ID);
            this.mWriter.write("\t");
            alleleMap.put(vc.getReference(), "0");
            String refString = vc.getReference().getDisplayString();
            this.mWriter.write(refString);
            this.mWriter.write("\t");
            if (vc.isVariant()) {
               Allele altAllele = vc.getAlternateAllele(0);
               alleleMap.put(altAllele, "1");
               String alt = altAllele.getDisplayString();
               this.mWriter.write(alt);

               for(int i = 1; i < vc.getAlternateAlleles().size(); ++i) {
                  altAllele = vc.getAlternateAllele(i);
                  alleleMap.put(altAllele, String.valueOf(i + 1));
                  alt = altAllele.getDisplayString();
                  this.mWriter.write(",");
                  this.mWriter.write(alt);
               }
            } else {
               this.mWriter.write(".");
            }

            this.mWriter.write("\t");
            if (!vc.hasLog10PError()) {
               this.mWriter.write(".");
            } else {
               this.mWriter.write(this.getQualValue(vc.getPhredScaledQual()));
            }

            this.mWriter.write("\t");
            String filters = getFilterString(vc, this.filtersWereAppliedToContext);
            this.mWriter.write(filters);
            this.mWriter.write("\t");
            Map infoFields = new TreeMap();
            Iterator j = vc.getAttributes().entrySet().iterator();

            String genotypeFormatString;
            while(j.hasNext()) {
               Entry field = (Entry)j.next();
               genotypeFormatString = (String)field.getKey();
               String outputValue = formatVCFField(field.getValue());
               if (outputValue != null) {
                  infoFields.put(genotypeFormatString, outputValue);
               }
            }

            this.writeInfoString(infoFields);
            GenotypesContext gc = vc.getGenotypes();
            if (gc instanceof LazyGenotypesContext && ((LazyGenotypesContext)gc).getUnparsedGenotypeData() != null) {
               this.mWriter.write("\t");
               this.mWriter.write(((LazyGenotypesContext)gc).getUnparsedGenotypeData().toString());
            } else {
               List genotypeAttributeKeys = new ArrayList();
               if (vc.hasGenotypes()) {
                  genotypeAttributeKeys.addAll(calcVCFGenotypeKeys(vc));
               } else if (this.mHeader.hasGenotypingData()) {
                  genotypeAttributeKeys.add("GT");
               }

               if (genotypeAttributeKeys.size() > 0) {
                  genotypeFormatString = ParsingUtils.join(":", genotypeAttributeKeys);
                  this.mWriter.write("\t");
                  this.mWriter.write(genotypeFormatString);
                  this.addGenotypeData(vc, alleleMap, genotypeAttributeKeys);
               }
            }

            this.mWriter.write("\n");
            this.mWriter.flush();
         } catch (IOException var11) {
            throw new RuntimeException("Unable to write the VCF object to " + this.getStreamName());
         }
      }
   }

   public static final String getFilterString(VariantContext vc) {
      return getFilterString(vc, false);
   }

   public static final String getFilterString(VariantContext vc, boolean forcePASS) {
      return vc.isFiltered() ? ParsingUtils.join(";", ParsingUtils.sortList(vc.getFilters())) : (!forcePASS && !vc.filtersWereApplied() ? "." : "PASS");
   }

   private String getQualValue(double qual) {
      String s = String.format("%.2f", qual);
      if (s.endsWith(".00")) {
         s = s.substring(0, s.length() - ".00".length());
      }

      return s;
   }

   private void writeInfoString(Map infoFields) throws IOException {
      if (infoFields.isEmpty()) {
         this.mWriter.write(".");
      } else {
         boolean isFirst = true;
         Iterator j = infoFields.entrySet().iterator();

         while(true) {
            Entry entry;
            VCFInfoHeaderLine metaData;
            do {
               String key;
               do {
                  if (!j.hasNext()) {
                     return;
                  }

                  entry = (Entry)j.next();
                  if (isFirst) {
                     isFirst = false;
                  } else {
                     this.mWriter.write(";");
                  }

                  key = (String)entry.getKey();
                  this.mWriter.write(key);
               } while(((String)entry.getValue()).equals(""));

               metaData = this.mHeader.getInfoHeaderLine(key);
            } while(metaData != null && metaData.getCountType() == VCFHeaderLineCount.INTEGER && metaData.getCount() == 0);

            this.mWriter.write("=");
            this.mWriter.write((String)entry.getValue());
         }
      }
   }

   private void addGenotypeData(VariantContext vc, Map alleleMap, List genotypeFormatKeys) throws IOException {
      Iterator j = this.mHeader.getGenotypeSamples().iterator();

      while(true) {
         label116:
         while(j.hasNext()) {
            String sample = (String)j.next();
            this.mWriter.write("\t");
            Genotype g = vc.getGenotype(sample);
            if (g == null) {
               this.mWriter.write("./.");
            } else {
               List attrs = new ArrayList(genotypeFormatKeys.size());
               Iterator k = genotypeFormatKeys.iterator();

               while(true) {
                  while(k.hasNext()) {
                     String key = (String)k.next();
                     if (key.equals("GT")) {
                        if (!g.isAvailable()) {
                           throw new ReviewedStingException("GTs cannot be missing for some samples if they are available for others in the record");
                        }

                        this.writeAllele(g.getAllele(0), alleleMap);

                        for(int i = 1; i < g.getPloidy(); ++i) {
                           this.mWriter.write(g.isPhased() ? "|" : "/");
                           this.writeAllele(g.getAllele(i), alleleMap);
                        }
                     } else {
                        Object val = g.hasAttribute(key) ? g.getAttribute(key) : ".";
                        if (key.equals("GQ")) {
                           if (!g.hasLog10PError()) {
                              val = ".";
                           } else {
                              val = this.getQualValue(Math.min(g.getPhredScaledQual(), 99.0D));
                           }
                        } else if (key.equals("FT")) {
                           val = g.isFiltered() ? ParsingUtils.join(";", ParsingUtils.sortList(g.getFilters())) : (g.filtersWereApplied() ? "PASS" : ".");
                        }

                        VCFFormatHeaderLine metaData = this.mHeader.getFormatHeaderLine(key);
                        if (metaData != null) {
                           int numInFormatField = metaData.getCount(vc.getAlternateAlleles().size());
                           if (numInFormatField > 1 && val.equals(".")) {
                              StringBuilder sb = new StringBuilder(".");

                              for(int i = 1; i < numInFormatField; ++i) {
                                 sb.append(",");
                                 sb.append(".");
                              }

                              val = sb.toString();
                           }
                        }

                        String outputValue = formatVCFField(val);
                        if (outputValue != null) {
                           attrs.add(outputValue);
                        }
                     }
                  }

                  int i;
                  for(i = attrs.size() - 1; i >= 0 && this.isMissingValue((String)attrs.get(i)); --i) {
                     attrs.remove(i);
                  }

                  i = 0;

                  while(true) {
                     if (i >= attrs.size()) {
                        continue label116;
                     }

                     if (i > 0 || genotypeFormatKeys.contains("GT")) {
                        this.mWriter.write(":");
                     }

                     this.mWriter.write((String)attrs.get(i));
                     ++i;
                  }
               }
            }
         }

         return;
      }
   }

   private boolean isMissingValue(String s) {
      return countOccurrences(".".charAt(0), s) + countOccurrences(',', s) == s.length();
   }

   private void writeAllele(Allele allele, Map alleleMap) throws IOException {
      String encoding = (String)alleleMap.get(allele);
      if (encoding == null) {
         throw new InternalCodecException("Allele " + allele + " is not an allele in the variant context");
      } else {
         this.mWriter.write(encoding);
      }
   }

   public static String formatVCFField(Object val) {
      String result;
      if (val == null) {
         result = ".";
      } else if (val instanceof Double) {
         result = String.format("%.2f", (Double)val);
      } else if (val instanceof Boolean) {
         result = (Boolean)val ? "" : null;
      } else if (val instanceof List) {
         result = formatVCFField(((List)val).toArray());
      } else if (val.getClass().isArray()) {
         int length = Array.getLength(val);
         if (length == 0) {
            return formatVCFField((Object)null);
         }

         StringBuffer sb = new StringBuffer(formatVCFField(Array.get(val, 0)));

         for(int i = 1; i < length; ++i) {
            sb.append(",");
            sb.append(formatVCFField(Array.get(val, i)));
         }

         result = sb.toString();
      } else {
         result = val.toString();
      }

      return result;
   }

   private static List calcVCFGenotypeKeys(VariantContext vc) {
      Set keys = new HashSet();
      boolean sawGoodGT = false;
      boolean sawGoodQual = false;
      boolean sawGenotypeFilter = false;
      Iterator j = vc.getGenotypes().iterator();

      while(j.hasNext()) {
         Genotype g = (Genotype)j.next();
         keys.addAll(g.getAttributes().keySet());
         if (g.isAvailable()) {
            sawGoodGT = true;
         }

         if (g.hasLog10PError()) {
            sawGoodQual = true;
         }

         if (g.isFiltered() && g.isCalled()) {
            sawGenotypeFilter = true;
         }
      }

      if (sawGoodQual) {
         keys.add("GQ");
      }

      if (sawGenotypeFilter) {
         keys.add("FT");
      }

      List sortedList = ParsingUtils.sortList(new ArrayList(keys));
      if (sawGoodGT) {
         List newList = new ArrayList(((List)sortedList).size() + 1);
         newList.add("GT");
         newList.addAll((Collection)sortedList);
         sortedList = newList;
      }

      return (List)sortedList;
   }

   private static int countOccurrences(char c, String s) {
      int count = 0;

      for(int i = 0; i < s.length(); ++i) {
         count += s.charAt(i) == c ? 1 : 0;
      }

      return count;
   }
}
*/
