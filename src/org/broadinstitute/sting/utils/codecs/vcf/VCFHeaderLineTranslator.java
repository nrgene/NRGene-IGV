package org.broadinstitute.sting.utils.codecs.vcf;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VCFHeaderLineTranslator {
   private static Map mapping = new HashMap();

   public static Map parseLine(VCFHeaderVersion version, String valueLine, List expectedTagOrder) {
      return ((VCFLineParser)mapping.get(version)).parseLine(valueLine, expectedTagOrder);
   }

   static {
      mapping.put(VCFHeaderVersion.VCF4_0, new VCF4Parser());
      mapping.put(VCFHeaderVersion.VCF4_1, new VCF4Parser());
      mapping.put(VCFHeaderVersion.VCF4_2, new VCF4Parser());
      mapping.put(VCFHeaderVersion.VCF3_3, new VCF3Parser());
      mapping.put(VCFHeaderVersion.VCF3_2, new VCF3Parser());
   }
}
