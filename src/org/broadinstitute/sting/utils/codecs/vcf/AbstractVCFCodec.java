package org.broadinstitute.sting.utils.codecs.vcf;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import org.apache.log4j.Logger;
import org.broad.igv.feature.genome.Genome;
import org.broad.tribble.Feature;
import org.broad.tribble.FeatureCodec;
import org.broad.tribble.NameAwareCodec;
import org.broad.tribble.TribbleException.InternalCodecException;
import org.broad.tribble.TribbleException.InvalidHeader;
import org.broad.tribble.readers.LineReader;
import org.broad.tribble.util.BlockCompressedInputStream;
import org.broad.tribble.util.ParsingUtils;
import org.broadinstitute.sting.utils.exceptions.ReviewedStingException;
import org.broadinstitute.sting.utils.exceptions.UserException;
import org.broadinstitute.sting.utils.variantcontext.Allele;
import org.broadinstitute.sting.utils.variantcontext.LazyGenotypesContext;
import org.broadinstitute.sting.utils.variantcontext.VariantContext;
import org.broadinstitute.sting.utils.variantcontext.VariantContextBuilder;

public abstract class AbstractVCFCodec implements FeatureCodec, NameAwareCodec {
   public static final int MAX_ALLELE_SIZE_BEFORE_WARNING = (int)Math.pow(2.0D, 20.0D);
   protected static final Logger log = Logger.getLogger(VCFCodec.class);
   protected static final int NUM_STANDARD_FIELDS = 8;
   protected VCFHeaderVersion version;
   protected VCFHeader header = null;
   protected Map alleleMap = new HashMap(3);
   protected String[] GTValueArray = new String[100];
   protected String[] genotypeKeyArray = new String[100];
   protected String[] infoFieldArray = new String[1000];
   protected String[] infoValueArray = new String[1000];
   public static boolean validate = true;
   protected String[] parts = null;
   protected String[] genotypeParts = null;
   protected final String[] locParts = new String[6];
   protected HashMap filterHash = new HashMap();
   TreeMap<String, VCFHeaderLineType> infoFields = new TreeMap<>();
   TreeMap formatFields = new TreeMap();
   Set filterFields = new HashSet();
   protected String name = "Unknown";
   protected int lineNo = 0;
   protected Map stringCache = new HashMap();

   public abstract Object readHeader(LineReader var1);

   public abstract LazyGenotypesContext.LazyData createGenotypeMap(String var1, List var2, String var3, int var4);

   protected abstract Set parseFilters(String var1);

   protected Object createHeader(List<String> headerStrings, String line) {
      headerStrings.add(line);
      Set metaData = new TreeSet();
      Set<String> sampleNames = new LinkedHashSet<>();
      Iterator k = headerStrings.iterator();

      boolean sawFormatTag;
      label82:
      do {
         while(k.hasNext()) {
            String str = (String)k.next();
            if (!str.startsWith("##")) {
               String[] strings = str.substring(1).split("\t");
               if (strings.length < VCFHeader.HEADER_FIELDS.values().length) {
                  throw new InvalidHeader("there are not enough columns present in the header line: " + str);
               }

               int arrayIndex = 0;

               for(int j = 0; j < VCFHeader.HEADER_FIELDS.values().length; ++j) {
                  VCFHeader.HEADER_FIELDS field = VCFHeader.HEADER_FIELDS.values()[j];

                  try {
                     if (field != VCFHeader.HEADER_FIELDS.valueOf(strings[arrayIndex])) {
                        throw new InvalidHeader("we were expecting column name '" + field + "' but we saw '" + strings[arrayIndex] + "'");
                     }
                  } catch (IllegalArgumentException var14) {
                     throw new InvalidHeader("unknown column name '" + strings[arrayIndex] + "'; it does not match a legal column header name.");
                  }

                  ++arrayIndex;
               }

               sawFormatTag = false;
               if (arrayIndex < strings.length) {
                  if (!strings[arrayIndex].equals("FORMAT")) {
                     throw new InvalidHeader("we were expecting column name 'FORMAT' but we saw '" + strings[arrayIndex] + "'");
                  }

                  sawFormatTag = true;
                  ++arrayIndex;
               }

               while(arrayIndex < strings.length) {
                  sampleNames.add(strings[arrayIndex++]);
               }
               continue label82;
            }

            if (str.startsWith("##INFO")) {
               VCFInfoHeaderLine info = new VCFInfoHeaderLine(str.substring(7), this.version);
               metaData.add(info);
               this.infoFields.put(info.getID(), info.getType());
            } else if (str.startsWith("##FILTER")) {
               VCFFilterHeaderLine filter = new VCFFilterHeaderLine(str.substring(9), this.version);
               metaData.add(filter);
               this.filterFields.add(filter.getID());
            } else if (str.startsWith("##FORMAT")) {
               VCFFormatHeaderLine format = new VCFFormatHeaderLine(str.substring(9), this.version);
               metaData.add(format);
               this.formatFields.put(format.getID(), format.getType());
            } else {
               VCFSimpleHeaderLine alt;
               if (str.startsWith("##contig")) {
                  alt = new VCFSimpleHeaderLine(str.substring(9), this.version, "##contig".substring(2), (List)null);
                  metaData.add(alt);
               } else if (str.startsWith("##ALT")) {
                  alt = new VCFSimpleHeaderLine(str.substring(6), this.version, "##ALT".substring(2), Arrays.asList("ID", "Description"));
                  metaData.add(alt);
               } else {
                  int equals = str.indexOf("=");
                  if (equals != -1) {
                     metaData.add(new VCFHeaderLine(str.substring(2, equals), str.substring(equals + 1)));
                  }
               }
            }
         }

         this.header = new VCFHeader(metaData, sampleNames);
         this.header.buildVCFReaderMaps(new ArrayList(sampleNames));
         return this.header;
      } while(!sawFormatTag || sampleNames.size() != 0);

      throw new UserException.MalformedVCFHeader("The FORMAT field was provided but there is no genotype/sample data");
   }

   public Feature decodeLoc(String line) {
      if (line.startsWith("#")) {
         return null;
      } else if (this.header == null) {
         throw new ReviewedStingException("VCF Header cannot be null when decoding a record");
      } else {
         int nParts = ParsingUtils.split(line, this.locParts, '\t', true);
         if (nParts != 6) {
            throw new UserException.MalformedVCF("there aren't enough columns for line " + line, this.lineNo);
         } else {
            String ref = this.getCachedString(this.locParts[3].toUpperCase());
            String alts = this.getCachedString(this.locParts[4].toUpperCase());
            List alleles = parseAlleles(ref, alts, this.lineNo);
            int start = 0;

            try {
               start = Integer.valueOf(this.locParts[1]);
            } catch (Exception var13) {
               generateException("the value in the POS field must be an integer but it was " + this.locParts[1], this.lineNo);
            }

            int stop = start;
            if (alleles.size() == 1) {
               stop = start + ((Allele)alleles.get(0)).length() - 1;
            } else if (alleles.size() == 2 && ((Allele)alleles.get(1)).isSymbolic()) {
               String[] extraParts = new String[4];
               int nExtraParts = ParsingUtils.split(this.locParts[5], extraParts, '\t', true);
               if (nExtraParts < 3) {
                  throw new UserException.MalformedVCF("there aren't enough columns for line " + line, this.lineNo);
               }

               Map attrs = this.parseInfo(extraParts[2]);

               try {
                  stop = attrs.containsKey("END") ? Integer.valueOf(attrs.get("END").toString()) : start;
               } catch (Exception var12) {
                  throw new UserException.MalformedVCF("the END value in the INFO field is not valid for line " + line, this.lineNo);
               }
            } else if (!isSingleNucleotideEvent(alleles)) {
               stop = clipAlleles(start, ref, alleles, (List)null, this.lineNo);
            }

            return new AbstractVCFCodec.VCFLocFeature(this.locParts[0], start, stop);
         }
      }
   }

   public Feature decode(String line) {
      if (line.startsWith("#")) {
         return null;
      } else if (this.header == null) {
         throw new ReviewedStingException("VCF Header cannot be null when decoding a record");
      } else {
         if (this.parts == null) {
            this.parts = new String[Math.min(this.header.getColumnCount(), 9)];
         }

         int nParts = ParsingUtils.split(line, this.parts, '\t', true);
         if ((this.header == null || !this.header.hasGenotypingData()) && nParts != 8 || this.header != null && this.header.hasGenotypingData() && nParts != 9) {
            throw new UserException.MalformedVCF("there aren't enough columns for line " + line + " (we expected " + (this.header == null ? 8 : 9) + " tokens, and saw " + nParts + " )", this.lineNo);
         } else {
            return this.parseVCFLine(this.parts);
         }
      }
   }

   @Override
   public Feature decode(String line, Genome genome) {
      return decode(line);
   }

   protected void generateException(String message) {
      throw new UserException.MalformedVCF(message, this.lineNo);
   }

   protected static void generateException(String message, int lineNo) {
      throw new UserException.MalformedVCF(message, lineNo);
   }

   private VariantContext parseVCFLine(String[] parts) {
      VariantContextBuilder builder = new VariantContextBuilder();
      builder.source(this.getName());
      ++this.lineNo;
      String chr = this.getCachedString(parts[0]);
      builder.chr(chr);
      int pos = Integer.valueOf(parts[1]);
      builder.start((long)pos);
      if (parts[2].length() == 0) {
         this.generateException("The VCF specification requires a valid ID field");
      } else if (parts[2].equals(".")) {
         builder.noID();
      } else {
         builder.id(parts[2]);
      }

      String ref = this.getCachedString(parts[3].toUpperCase());
      String alts = this.getCachedString(parts[4].toUpperCase());
      builder.log10PError(parseQual(parts[5]));
      builder.filters(this.parseFilters(this.getCachedString(parts[6])));
      Map attrs = this.parseInfo(parts[7]);
      builder.attributes(attrs);

      if ( attrs.containsKey(VCFConstants.END_KEY) ) {
         // update stop with the end key if provided
         try {
            builder.stop(Integer.valueOf(attrs.get(VCFConstants.END_KEY).toString()));
         } catch (Exception e) {
            generateException("the END value in the INFO field is not valid");
         }
      } else {
         builder.stop(pos + ref.length() - 1);
      }

      List alleles = parseAlleles(ref, alts, this.lineNo);

      builder.alleles((Collection)alleles);
      if (parts.length > 8) {
         LazyGenotypesContext.LazyParser lazyParser = new AbstractVCFCodec.LazyVCFGenotypesParser((List)alleles, chr, pos);
         int nGenotypes = this.header.getGenotypeSamples().size();
         LazyGenotypesContext lazy = new LazyGenotypesContext(lazyParser, parts[8], nGenotypes);
         if (!this.header.samplesWereAlreadySorted()) {
            lazy.decode();
         }

         builder.genotypesNoValidation(lazy);
      }

      VariantContext vc = null;

      try {
         builder.referenceBaseForIndel(ref.getBytes()[0]);
         vc = builder.make();
      } catch (Exception var13) {
         this.generateException(var13.getMessage());
      }

      return vc;
   }

   public Class getFeatureType() {
      return VariantContext.class;
   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      this.name = name;
   }

   protected String getCachedString(String str) {
      String internedString = (String)this.stringCache.get(str);
      if (internedString == null) {
         internedString = new String(str);
         this.stringCache.put(internedString, internedString);
      }

      return internedString;
   }

   private Map parseInfo(String infoField) {
      Map attributes = new HashMap();
      if (infoField.length() == 0) {
         this.generateException("The VCF specification requires a valid info field");
      }

      if (!infoField.equals(".")) {
         if (infoField.indexOf("\t") != -1 || infoField.indexOf(" ") != -1) {
            this.generateException("The VCF specification does not allow for whitespace in the INFO field");
         }

         int infoFieldSplitSize = ParsingUtils.split(infoField, this.infoFieldArray, ';', false);

         for(int i = 0; i < infoFieldSplitSize; ++i) {
            int eqI = this.infoFieldArray[i].indexOf("=");
            String key;
            Object value;
            if (eqI == -1) {
               key = this.infoFieldArray[i];
               value = true;
            } else {
               key = this.infoFieldArray[i].substring(0, eqI);
               String str = this.infoFieldArray[i].substring(eqI + 1);
               int infoValueSplitSize = ParsingUtils.split(str, this.infoValueArray, ',', false);
               if (infoValueSplitSize == 1) {
                  value = this.infoValueArray[0];
               } else {
                  ArrayList valueList = new ArrayList(infoValueSplitSize);

                  for(int j = 0; j < infoValueSplitSize; ++j) {
                     valueList.add(this.infoValueArray[j]);
                  }

                  value = valueList;
               }
            }

            attributes.put(key, value);
         }
      }

      return attributes;
   }

   protected static Allele oneAllele(String index, List alleles) {
      if (index.equals(".")) {
         return Allele.NO_CALL;
      } else {
         int i;
         try {
            i = Integer.valueOf(index);
         } catch (NumberFormatException var4) {
            throw new InternalCodecException("The following invalid GT allele index was encountered in the file: " + index);
         }

         if (i >= alleles.size()) {
            throw new InternalCodecException("The allele with index " + index + " is not defined in the REF/ALT columns in the record");
         } else {
            return (Allele)alleles.get(i);
         }
      }
   }

   protected static List parseGenotypeAlleles(String GT, List alleles, Map cache) {
      List GTAlleles = (List)cache.get(GT);
      if (GTAlleles == null) {
         StringTokenizer st = new StringTokenizer(GT, "/|\\");
         GTAlleles = new ArrayList(st.countTokens());

         while(st.hasMoreTokens()) {
            String genotype = st.nextToken();
            ((List)GTAlleles).add(oneAllele(genotype, alleles));
         }

         cache.put(GT, GTAlleles);
      }

      return (List)GTAlleles;
   }

   protected static Double parseQual(String qualString) {
      if (qualString.equals(".")) {
         return 1.0D;
      } else {
         Double val = Double.valueOf(qualString);
         return val < 0.0D && Math.abs(val - VCFConstants.MISSING_QUALITY_v3_DOUBLE) < VCFConstants.VCF_ENCODING_EPSILON ? 1.0D : val / -10.0D;
      }
   }

   /**
    * parse out the alleles
    * @param ref the reference base
    * @param alts a string of alternates to break into alleles
    * @param lineNo  the line number for this record
    * @return a list of alleles, and a pair of the shortest and longest sequence
    */
   protected static List<Allele> parseAlleles(String ref, String alts, int lineNo) {
      List<Allele> alleles = new ArrayList<Allele>(2); // we are almost always biallelic
      // ref
      checkAllele(ref, true, lineNo);
      Allele refAllele = Allele.create(ref, true);
      alleles.add(refAllele);

      if ( alts.indexOf(",") == -1 ) // only 1 alternatives, don't call string split
         parseSingleAltAllele(alleles, alts, lineNo);
      else
         for ( String alt : alts.split(",") )
            parseSingleAltAllele(alleles, alt, lineNo);

      return alleles;
   }

   private static void checkAllele(String allele, boolean isRef, int lineNo) {
      if (allele == null || allele.length() == 0) {
         generateException("Empty alleles are not permitted in VCF records", lineNo);
      }

      if (MAX_ALLELE_SIZE_BEFORE_WARNING != -1 && allele.length() > MAX_ALLELE_SIZE_BEFORE_WARNING) {
         log.warn(String.format("Allele detected with length %d exceeding max size %d at approximately line %d, likely resulting in degraded VCF processing performance", allele.length(), MAX_ALLELE_SIZE_BEFORE_WARNING, lineNo));
      }

      if (isSymbolicAllele(allele)) {
         if (isRef) {
            generateException("Symbolic alleles not allowed as reference allele: " + allele, lineNo);
         }
      } else {
         if (allele.charAt(0) == 'D' || allele.charAt(0) == 'I') {
            generateException("Insertions/Deletions are not supported when reading 3.x VCF's. Please convert your file to VCF4 using VCFTools, available at http://vcftools.sourceforge.net/index.html", lineNo);
         }

         if (!Allele.acceptableAlleleBases(allele, isRef)) {
            generateException("Unparsable vcf record with allele " + allele, lineNo);
         }

         if (isRef && allele.equals(".")) {
            generateException("The reference allele cannot be missing", lineNo);
         }
      }

   }

   private static boolean isSymbolicAllele(String allele) {
      return allele != null && allele.length() > 2 && (allele.startsWith("<") && allele.endsWith(">") || allele.contains("[") || allele.contains("]"));
   }

   private static void parseSingleAltAllele(List alleles, String alt, int lineNo) {
      checkAllele(alt, false, lineNo);
      Allele allele = Allele.create(alt, false);
      if (!allele.isNoCall()) {
         alleles.add(allele);
      }

   }

   protected static boolean isSingleNucleotideEvent(List alleles) {
      Iterator j = alleles.iterator();

      Allele a;
      do {
         if (!j.hasNext()) {
            return true;
         }

         a = (Allele)j.next();
      } while(a.length() == 1);

      return false;
   }

   public static int computeForwardClipping(List unclippedAlleles, byte ref0) {
      boolean clipping = true;
      int symbolicAlleleCount = 0;
      Iterator j = unclippedAlleles.iterator();

      while(j.hasNext()) {
         Allele a = (Allele)j.next();
         if (a.isSymbolic()) {
            ++symbolicAlleleCount;
         } else if (a.length() < 1 || a.getBases()[0] != ref0) {
            clipping = false;
            break;
         }
      }

      return clipping && symbolicAlleleCount != unclippedAlleles.size() ? 1 : 0;
   }

   public static int computeReverseClipping(List unclippedAlleles, byte[] ref, int forwardClipping, boolean allowFullClip, int lineNo) {
      int clipping = 0;
      boolean stillClipping = true;

      label54:
      while(stillClipping) {
         Iterator j = unclippedAlleles.iterator();

         while(true) {
            while(true) {
               Allele a;
               do {
                  if (!j.hasNext()) {
                     if (stillClipping) {
                        ++clipping;
                     }
                     continue label54;
                  }

                  a = (Allele)j.next();
               } while(a.isSymbolic());

               if (a.length() - clipping == 0) {
                  return clipping - (allowFullClip ? 0 : 1);
               }

               if (a.length() - clipping > forwardClipping && a.length() - forwardClipping != 0) {
                  if (ref.length == clipping) {
                     if (allowFullClip) {
                        stillClipping = false;
                     } else {
                        generateException("bad alleles encountered", lineNo);
                     }
                  } else if (a.getBases()[a.length() - clipping - 1] != ref[ref.length - clipping - 1]) {
                     stillClipping = false;
                  }
               } else {
                  stillClipping = false;
               }
            }
         }
      }

      return clipping;
   }

   protected static int clipAlleles(int position, String ref, List unclippedAlleles, List clippedAlleles, int lineNo) {
      int forwardClipping = computeForwardClipping(unclippedAlleles, (byte)ref.charAt(0));
      int reverseClipping = computeReverseClipping(unclippedAlleles, ref.getBytes(), forwardClipping, false, lineNo);
      if (clippedAlleles != null) {
         Iterator j = unclippedAlleles.iterator();

         while(j.hasNext()) {
            Allele a = (Allele)j.next();
            if (a.isSymbolic()) {
               clippedAlleles.add(a);
            } else {
               clippedAlleles.add(Allele.create(Arrays.copyOfRange(a.getBases(), forwardClipping, a.getBases().length - reverseClipping), a.isReference()));
            }
         }
      }

      int refLength = ref.length() - reverseClipping;
      return position + Math.max(refLength - 1, 0);
   }

   public static final boolean canDecodeFile(String potentialInput, String MAGIC_HEADER_LINE) {
      try {
         return isVCFStream(new FileInputStream(potentialInput), MAGIC_HEADER_LINE) || isVCFStream(new GZIPInputStream(new FileInputStream(potentialInput)), MAGIC_HEADER_LINE) || isVCFStream(new BlockCompressedInputStream(new FileInputStream(potentialInput)), MAGIC_HEADER_LINE);
      } catch (FileNotFoundException var3) {
         return false;
      } catch (IOException var4) {
         return false;
      }
   }

   private static final boolean isVCFStream(InputStream stream, String MAGIC_HEADER_LINE) {
      boolean var3;
      try {
         byte[] buff = new byte[MAGIC_HEADER_LINE.length()];
         stream.read(buff, 0, MAGIC_HEADER_LINE.length());
         boolean eq = Arrays.equals(buff, MAGIC_HEADER_LINE.getBytes());
         boolean var5 = eq;
         return var5;
      } catch (IOException var17) {
         var3 = false;
      } catch (RuntimeException var18) {
         var3 = false;
         return var3;
      } finally {
         try {
            stream.close();
         } catch (IOException var16) {
         }

      }

      return var3;
   }

   private static final class VCFLocFeature implements Feature {
      final String chr;
      final int start;
      final int stop;

      private VCFLocFeature(String chr, int start, int stop) {
         this.chr = chr;
         this.start = start;
         this.stop = stop;
      }

      public String getChr() {
         return this.chr;
      }

      public int getStart() {
         return this.start;
      }

      public int getEnd() {
         return this.stop;
      }

      // $FF: synthetic method
      VCFLocFeature(String x0, int x1, int x2, Object x3) {
         this(x0, x1, x2);
      }
   }

   class LazyVCFGenotypesParser implements LazyGenotypesContext.LazyParser {
      final List alleles;
      final String contig;
      final int start;

      LazyVCFGenotypesParser(List alleles, String contig, int start) {
         this.alleles = alleles;
         this.contig = contig;
         this.start = start;
      }

      public LazyGenotypesContext.LazyData parse(Object data) {
         return AbstractVCFCodec.this.createGenotypeMap((String)data, this.alleles, this.contig, this.start);
      }
   }
}
