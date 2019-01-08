package org.broadinstitute.sting.utils.codecs.vcf;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.broad.tribble.TribbleException;
import org.broad.tribble.TribbleException.InternalCodecException;
import org.broad.tribble.TribbleException.InvalidHeader;
import org.broad.tribble.readers.LineReader;
import org.broad.tribble.util.ParsingUtils;
import org.broadinstitute.sting.utils.variantcontext.Genotype;
import org.broadinstitute.sting.utils.variantcontext.LazyGenotypesContext;

public class VCF3Codec extends AbstractVCFCodec {
   public static final String VCF3_MAGIC_HEADER = "##fileformat=VCFv3";

   public Object readHeader(LineReader reader) {
      ArrayList headerStrings = new ArrayList();

      String line;
      try {
         for(boolean foundHeaderVersion = false; (line = reader.readLine()) != null; headerStrings.add(line)) {
            ++this.lineNo;
            if (!line.startsWith("##")) {
               if (line.startsWith("#")) {
                  if (!foundHeaderVersion) {
                     throw new InvalidHeader("We never saw a header line specifying VCF version");
                  }

                  return this.createHeader(headerStrings, line);
               }

               throw new InvalidHeader("We never saw the required CHROM header line (starting with one #) for the input VCF file");
            }

            String[] lineFields = line.substring(2).split("=");
            if (lineFields.length == 2 && VCFHeaderVersion.isFormatString(lineFields[0])) {
               if (!VCFHeaderVersion.isVersionString(lineFields[1])) {
                  throw new InvalidHeader(lineFields[1] + " is not a supported version");
               }

               foundHeaderVersion = true;
               this.version = VCFHeaderVersion.toHeaderVersion(lineFields[1]);
               if (this.version != VCFHeaderVersion.VCF3_3 && this.version != VCFHeaderVersion.VCF3_2) {
                  throw new InvalidHeader("This codec is strictly for VCFv3 and does not support " + lineFields[1]);
               }
            }
         }
      } catch (IOException var6) {
         throw new RuntimeException("IO Exception ", var6);
      }

      throw new InvalidHeader("We never saw the required CHROM header line (starting with one #) for the input VCF file");
   }

   protected Set parseFilters(String filterString) {
      if (filterString.equals(".")) {
         return null;
      } else {
         LinkedHashSet fFields = new LinkedHashSet();
         if (filterString.equals("0")) {
            return fFields;
         } else {
            if (filterString.length() == 0) {
               this.generateException("The VCF specification requires a valid filter status");
            }

            if (this.filterHash.containsKey(filterString)) {
               return (Set)this.filterHash.get(filterString);
            } else {
               if (filterString.indexOf(";") == -1) {
                  fFields.add(filterString);
               } else {
                  fFields.addAll(Arrays.asList(filterString.split(";")));
               }

               this.filterHash.put(filterString, fFields);
               return fFields;
            }
         }
      }
   }

   public LazyGenotypesContext.LazyData createGenotypeMap(String str, List alleles, String chr, int pos) {
      if (this.genotypeParts == null) {
         this.genotypeParts = new String[this.header.getColumnCount() - 8];
      }

      int nParts = ParsingUtils.split(str, this.genotypeParts, '\t');
      if (nParts != this.genotypeParts.length) {
         generateException("there are " + (nParts - 1) + " genotypes while the header requires that " + (this.genotypeParts.length - 1) + " genotypes be present for all records", this.lineNo);
      }

      ArrayList genotypes = new ArrayList(nParts);
      int nGTKeys = ParsingUtils.split(this.genotypeParts[0], this.genotypeKeyArray, ':');
      Iterator sampleNameIterator = this.header.getGenotypeSamples().iterator();
      this.alleleMap.clear();

      for(int genotypeOffset = 1; genotypeOffset < nParts; ++genotypeOffset) {
         int GTValueSplitSize = ParsingUtils.split(this.genotypeParts[genotypeOffset], this.GTValueArray, ':');
         double GTQual = 1.0D;
         Set genotypeFilters = null;
         Map gtAttributes = null;
         String sampleName = (String)sampleNameIterator.next();
         if (nGTKeys < GTValueSplitSize) {
            this.generateException("There are too many keys for the sample " + sampleName + ", keys = " + this.parts[8] + ", values = " + this.parts[genotypeOffset]);
         }

         int genotypeAlleleLocation = -1;
         if (nGTKeys >= 1) {
            gtAttributes = new HashMap(nGTKeys - 1);

            for(int i = 0; i < nGTKeys; ++i) {
               String gtKey = new String(this.genotypeKeyArray[i]);
               boolean missing = i >= GTValueSplitSize;
               if (gtKey.equals("GT")) {
                  genotypeAlleleLocation = i;
               } else if (gtKey.equals("GQ")) {
                  GTQual = missing ? parseQual(".") : parseQual(this.GTValueArray[i]);
               } else if (gtKey.equals("FT")) {
                  genotypeFilters = missing ? this.parseFilters(".") : this.parseFilters(this.getCachedString(this.GTValueArray[i]));
               } else if (!missing && !this.GTValueArray[i].equals("-1")) {
                  gtAttributes.put(gtKey, new String(this.GTValueArray[i]));
               } else {
                  gtAttributes.put(gtKey, ".");
               }
            }
         }

         if (genotypeAlleleLocation < 0) {
            this.generateException("Unable to find the GT field for the record; the GT field is required");
         }

         if (genotypeAlleleLocation > 0) {
            this.generateException("Saw GT field at position " + genotypeAlleleLocation + ", but it must be at the first position for genotypes");
         }

         boolean phased = this.GTValueArray[genotypeAlleleLocation].indexOf("|") != -1;

         try {
            genotypes.add(new Genotype(sampleName, parseGenotypeAlleles(this.GTValueArray[genotypeAlleleLocation], alleles, this.alleleMap), GTQual, genotypeFilters, gtAttributes, phased));
         } catch (TribbleException var20) {
            throw new InternalCodecException(var20.getMessage() + ", at position " + chr + ":" + pos);
         }
      }

      return new LazyGenotypesContext.LazyData(genotypes, this.header.sampleNamesInOrder, this.header.sampleNameToOffset);
   }

   public boolean canDecode(String potentialInput) {
      return canDecodeFile(potentialInput, "##fileformat=VCFv3");
   }
}
