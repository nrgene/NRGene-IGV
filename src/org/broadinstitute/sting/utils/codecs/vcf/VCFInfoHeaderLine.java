package org.broadinstitute.sting.utils.codecs.vcf;

public class VCFInfoHeaderLine extends VCFCompoundHeaderLine {
   public VCFInfoHeaderLine(String name, int count, VCFHeaderLineType type, String description) {
      super(name, count, type, description, VCFCompoundHeaderLine.SupportedHeaderLineType.INFO);
   }

   public VCFInfoHeaderLine(String name, VCFHeaderLineCount count, VCFHeaderLineType type, String description) {
      super(name, count, type, description, VCFCompoundHeaderLine.SupportedHeaderLineType.INFO);
   }

   protected VCFInfoHeaderLine(String line, VCFHeaderVersion version) {
      super(line, version, VCFCompoundHeaderLine.SupportedHeaderLineType.INFO);
   }

   boolean allowFlagValues() {
      return true;
   }
}
