package org.broadinstitute.sting.utils.codecs.vcf;

import java.util.Arrays;

public class VCFFilterHeaderLine extends VCFSimpleHeaderLine {
   public VCFFilterHeaderLine(String name, String description) {
      super("FILTER", name, description);
   }

   protected VCFFilterHeaderLine(String line, VCFHeaderVersion version) {
      super(line, version, "FILTER", Arrays.asList("ID", "Description"));
   }
}
