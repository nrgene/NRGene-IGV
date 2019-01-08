package org.broadinstitute.sting.utils.codecs.vcf;

import org.broadinstitute.sting.utils.variantcontext.VariantContext;

public class SortingVCFWriter extends SortingVCFWriterBase {
   private int maxCachingStartDistance;

   public SortingVCFWriter(VCFWriter innerWriter, int maxCachingStartDistance, boolean takeOwnershipOfInner) {
      super(innerWriter, takeOwnershipOfInner);
      this.maxCachingStartDistance = maxCachingStartDistance;
   }

   public SortingVCFWriter(VCFWriter innerWriter, int maxCachingStartDistance) {
      this(innerWriter, maxCachingStartDistance, false);
   }

   protected void noteCurrentRecord(VariantContext vc) {
      super.noteCurrentRecord(vc);
      int mostUpstreamWritableIndex = vc.getStart() - this.maxCachingStartDistance;
      this.mostUpstreamWritableLoc = Math.max(0, mostUpstreamWritableIndex);
   }
}
