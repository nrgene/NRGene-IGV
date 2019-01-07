package org.broadinstitute.sting.utils.codecs.vcf;

public class ManualSortingVCFWriter extends SortingVCFWriterBase {
   public ManualSortingVCFWriter(VCFWriter innerWriter, boolean takeOwnershipOfInner) {
      super(innerWriter, takeOwnershipOfInner);
   }

   public ManualSortingVCFWriter(VCFWriter innerWriter) {
      super(innerWriter);
   }

   public void setmostUpstreamWritableLocus(Integer mostUpstreamWritableLoc) {
      this.mostUpstreamWritableLoc = mostUpstreamWritableLoc;
      this.emitSafeRecords();
   }
}
