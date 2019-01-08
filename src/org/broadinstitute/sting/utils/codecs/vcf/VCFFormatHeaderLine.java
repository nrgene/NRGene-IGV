package org.broadinstitute.sting.utils.codecs.vcf;

public class VCFFormatHeaderLine extends VCFCompoundHeaderLine {
   public VCFFormatHeaderLine(String name, int count, VCFHeaderLineType type, String description) {
      super(name, count, type, description, VCFCompoundHeaderLine.SupportedHeaderLineType.FORMAT);
      if (type == VCFHeaderLineType.Flag) {
         throw new IllegalArgumentException("Flag is an unsupported type for format fields");
      }
   }

   public VCFFormatHeaderLine(String name, VCFHeaderLineCount count, VCFHeaderLineType type, String description) {
      super(name, count, type, description, VCFCompoundHeaderLine.SupportedHeaderLineType.FORMAT);
   }

   protected VCFFormatHeaderLine(String line, VCFHeaderVersion version) {
      super(line, version, VCFCompoundHeaderLine.SupportedHeaderLineType.FORMAT);
   }

   boolean allowFlagValues() {
      return false;
   }
}
