package org.broadinstitute.sting.utils.codecs.vcf;

import java.util.Comparator;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.PriorityBlockingQueue;
import org.broadinstitute.sting.utils.variantcontext.VariantContext;

public abstract class SortingVCFWriterBase implements VCFWriter {
   private final VCFWriter innerWriter;
   private final Queue queue;
   protected Integer mostUpstreamWritableLoc;
   protected static final int BEFORE_MOST_UPSTREAM_LOC = 0;
   private final Set finishedChromosomes;
   private final boolean takeOwnershipOfInner;

   public SortingVCFWriterBase(VCFWriter innerWriter, boolean takeOwnershipOfInner) {
      this.innerWriter = innerWriter;
      this.finishedChromosomes = new TreeSet();
      this.takeOwnershipOfInner = takeOwnershipOfInner;
      this.queue = new PriorityBlockingQueue(50, new SortingVCFWriterBase.VariantContextComparator());
      this.mostUpstreamWritableLoc = 0;
   }

   public SortingVCFWriterBase(VCFWriter innerWriter) {
      this(innerWriter, false);
   }

   public void writeHeader(VCFHeader header) {
      this.innerWriter.writeHeader(header);
   }

   public void close() {
      this.stopWaitingToSort();
      if (this.takeOwnershipOfInner) {
         this.innerWriter.close();
      }

   }

   public synchronized void add(VariantContext vc) {
      SortingVCFWriterBase.VCFRecord firstRec = (SortingVCFWriterBase.VCFRecord)this.queue.peek();
      if (firstRec != null && !vc.getChr().equals(firstRec.vc.getChr())) {
         if (this.finishedChromosomes.contains(vc.getChr())) {
            throw new IllegalArgumentException("Added a record at " + vc.getChr() + ":" + vc.getStart() + ", but already finished with chromosome" + vc.getChr());
         }

         this.finishedChromosomes.add(firstRec.vc.getChr());
         this.stopWaitingToSort();
      }

      this.noteCurrentRecord(vc);
      this.queue.add(new SortingVCFWriterBase.VCFRecord(vc));
      this.emitSafeRecords();
   }

   public String toString() {
      return this.getClass().getName();
   }

   private synchronized void stopWaitingToSort() {
      this.emitRecords(true);
      this.mostUpstreamWritableLoc = 0;
   }

   protected synchronized void emitSafeRecords() {
      this.emitRecords(false);
   }

   protected void noteCurrentRecord(VariantContext vc) {
      if (this.mostUpstreamWritableLoc != null && vc.getStart() < this.mostUpstreamWritableLoc) {
         throw new IllegalArgumentException("Permitted to write any record upstream of position " + this.mostUpstreamWritableLoc + ", but a record at " + vc.getChr() + ":" + vc.getStart() + " was just added.");
      }
   }

   private synchronized void emitRecords(boolean emitUnsafe) {
      while(true) {
         if (!this.queue.isEmpty()) {
            SortingVCFWriterBase.VCFRecord firstRec = (SortingVCFWriterBase.VCFRecord)this.queue.peek();
            if (emitUnsafe || this.mostUpstreamWritableLoc == null || firstRec.vc.getStart() <= this.mostUpstreamWritableLoc) {
               this.queue.poll();
               this.innerWriter.add(firstRec.vc);
               continue;
            }
         }

         return;
      }
   }

   private static class VCFRecord {
      public VariantContext vc;

      public VCFRecord(VariantContext vc) {
         this.vc = vc;
      }
   }

   private static class VariantContextComparator implements Comparator<SortingVCFWriterBase.VCFRecord> {
      private VariantContextComparator() {
      }

      public int compare(SortingVCFWriterBase.VCFRecord r1, SortingVCFWriterBase.VCFRecord r2) {
         return r1.vc.getStart() - r2.vc.getStart();
      }

      // $FF: synthetic method
      VariantContextComparator(Object x0) {
         this();
      }
   }
}
