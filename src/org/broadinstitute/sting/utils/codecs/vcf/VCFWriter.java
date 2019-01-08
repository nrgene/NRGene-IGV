package org.broadinstitute.sting.utils.codecs.vcf;

import org.broadinstitute.sting.utils.variantcontext.VariantContext;

public interface VCFWriter {
   void writeHeader(VCFHeader var1);

   void close();

   void add(VariantContext var1);
}
