package org.broadinstitute.sting.utils;

import com.google.java.contract.Ensures;
import com.google.java.contract.Requires;
import java.io.Serializable;
import org.broadinstitute.sting.utils.exceptions.ReviewedStingException;

public class GenomeLoc implements Comparable<GenomeLoc>, Serializable, HasGenomeLocation {
   protected final int contigIndex;
   protected final int start;
   protected final int stop;
   protected final String contigName;
   public static final GenomeLoc UNMAPPED = new GenomeLoc((String)null);
   public static final GenomeLoc WHOLE_GENOME = new GenomeLoc("all");

   public static final boolean isUnmapped(GenomeLoc loc) {
      return loc == UNMAPPED;
   }

   @Requires({"contig != null", "contigIndex >= 0", "start <= stop"})
   protected GenomeLoc(String contig, int contigIndex, int start, int stop) {
      this.contigName = contig;
      this.contigIndex = contigIndex;
      this.start = start;
      this.stop = stop;
   }

   private GenomeLoc(String contig) {
      this.contigName = contig;
      this.contigIndex = -1;
      this.start = 0;
      this.stop = 0;
   }

   @Ensures({"result != null"})
   public final GenomeLoc getLocation() {
      return this;
   }

   public final GenomeLoc getStartLocation() {
      return new GenomeLoc(this.getContig(), this.getContigIndex(), this.getStart(), this.getStart());
   }

   public final GenomeLoc getStopLocation() {
      return new GenomeLoc(this.getContig(), this.getContigIndex(), this.getStop(), this.getStop());
   }

   public final String getContig() {
      return this.contigName;
   }

   public final int getContigIndex() {
      return this.contigIndex;
   }

   public final int getStart() {
      return this.start;
   }

   public final int getStop() {
      return this.stop;
   }

   @Ensures({"result != null"})
   public final String toString() {
      if (isUnmapped(this)) {
         return "unmapped";
      } else if (this.throughEndOfContigP() && this.atBeginningOfContigP()) {
         return this.getContig();
      } else {
         return !this.throughEndOfContigP() && this.getStart() != this.getStop() ? String.format("%s:%d-%d", this.getContig(), this.getStart(), this.getStop()) : String.format("%s:%d", this.getContig(), this.getStart());
      }
   }

   private boolean throughEndOfContigP() {
      return this.stop == Integer.MAX_VALUE;
   }

   private boolean atBeginningOfContigP() {
      return this.start == 1;
   }

   @Requires({"that != null"})
   public final boolean disjointP(GenomeLoc that) {
      return this.contigIndex != that.contigIndex || this.start > that.stop || that.start > this.stop;
   }

   @Requires({"that != null"})
   public final boolean discontinuousP(GenomeLoc that) {
      return this.contigIndex != that.contigIndex || this.start - 1 > that.stop || that.start - 1 > this.stop;
   }

   @Requires({"that != null"})
   public final boolean overlapsP(GenomeLoc that) {
      return !this.disjointP(that);
   }

   @Requires({"that != null"})
   public final boolean contiguousP(GenomeLoc that) {
      return !this.discontinuousP(that);
   }

   @Requires({"that != null", "isUnmapped(this) == isUnmapped(that)"})
   @Ensures({"result != null"})
   public GenomeLoc merge(GenomeLoc that) throws ReviewedStingException {
      if (!isUnmapped(this) && !isUnmapped(that)) {
         if (!this.contiguousP(that)) {
            throw new ReviewedStingException("The two genome loc's need to be contigous");
         } else {
            return new GenomeLoc(this.getContig(), this.contigIndex, Math.min(this.getStart(), that.getStart()), Math.max(this.getStop(), that.getStop()));
         }
      } else if (isUnmapped(this) && isUnmapped(that)) {
         return UNMAPPED;
      } else {
         throw new ReviewedStingException("Tried to merge a mapped and an unmapped genome loc");
      }
   }

   @Requires({"that != null", "isUnmapped(this) == isUnmapped(that)"})
   @Ensures({"result != null"})
   public GenomeLoc endpointSpan(GenomeLoc that) throws ReviewedStingException {
      if (!isUnmapped(this) && !isUnmapped(that)) {
         if (!this.getContig().equals(that.getContig())) {
            throw new ReviewedStingException("Cannot get endpoint span for genome locs on different contigs");
         } else {
            return new GenomeLoc(this.getContig(), this.contigIndex, Math.min(this.getStart(), that.getStart()), Math.max(this.getStop(), that.getStop()));
         }
      } else {
         throw new ReviewedStingException("Cannot get endpoint span for unmerged genome locs");
      }
   }

   public GenomeLoc[] split(int splitPoint) {
      if (splitPoint >= this.getStart() && splitPoint <= this.getStop()) {
         return new GenomeLoc[]{new GenomeLoc(this.getContig(), this.contigIndex, this.getStart(), splitPoint - 1), new GenomeLoc(this.getContig(), this.contigIndex, splitPoint, this.getStop())};
      } else {
         throw new ReviewedStingException(String.format("Unable to split contig %s at split point %d; split point is not contained in region.", this, splitPoint));
      }
   }

   @Requires({"that != null"})
   @Ensures({"result != null"})
   public GenomeLoc intersect(GenomeLoc that) throws ReviewedStingException {
      if (!isUnmapped(this) && !isUnmapped(that)) {
         if (!this.overlapsP(that)) {
            throw new ReviewedStingException("GenomeLoc::intersect(): The two genome loc's need to overlap");
         } else {
            return new GenomeLoc(this.getContig(), this.contigIndex, Math.max(this.getStart(), that.getStart()), Math.min(this.getStop(), that.getStop()));
         }
      } else if (isUnmapped(this) && isUnmapped(that)) {
         return UNMAPPED;
      } else {
         throw new ReviewedStingException("Tried to intersect a mapped and an unmapped genome loc");
      }
   }

   @Requires({"that != null"})
   public final boolean containsP(GenomeLoc that) {
      return this.onSameContig(that) && this.getStart() <= that.getStart() && this.getStop() >= that.getStop();
   }

   @Requires({"that != null"})
   public final boolean onSameContig(GenomeLoc that) {
      return this.contigIndex == that.contigIndex;
   }

   @Requires({"that != null"})
   public final int minus(GenomeLoc that) {
      return this.onSameContig(that) ? this.getStart() - that.getStart() : Integer.MAX_VALUE;
   }

   @Requires({"that != null"})
   @Ensures({"result >= 0"})
   public final int distance(GenomeLoc that) {
      return Math.abs(this.minus(that));
   }

   @Requires({"left != null", "right != null"})
   public final boolean isBetween(GenomeLoc left, GenomeLoc right) {
      return this.compareTo(left) > -1 && this.compareTo(right) < 1;
   }

   @Requires({"that != null"})
   public final boolean isBefore(GenomeLoc that) {
      int comparison = this.compareContigs(that);
      return comparison == -1 || comparison == 0 && this.getStop() < that.getStart();
   }

   @Requires({"that != null"})
   public final boolean startsBefore(GenomeLoc that) {
      int comparison = this.compareContigs(that);
      return comparison == -1 || comparison == 0 && this.getStart() < that.getStart();
   }

   @Requires({"that != null"})
   public final boolean isPast(GenomeLoc that) {
      int comparison = this.compareContigs(that);
      return comparison == 1 || comparison == 0 && this.getStart() > that.getStop();
   }

   @Requires({"that != null"})
   @Ensures({"result >= 0"})
   public final int minDistance(GenomeLoc that) {
      if (!this.onSameContig(that)) {
         return Integer.MAX_VALUE;
      } else {
         int minDistance;
         if (this.isBefore(that)) {
            minDistance = distanceFirstStopToSecondStart(this, that);
         } else if (that.isBefore(this)) {
            minDistance = distanceFirstStopToSecondStart(that, this);
         } else {
            minDistance = 0;
         }

         return minDistance;
      }
   }

   @Requires({"locFirst != null", "locSecond != null", "locSecond.isPast(locFirst)"})
   @Ensures({"result >= 0"})
   private static int distanceFirstStopToSecondStart(GenomeLoc locFirst, GenomeLoc locSecond) {
      return locSecond.getStart() - locFirst.getStop();
   }

   public boolean equals(Object other) {
      if (other == null) {
         return false;
      } else if (!(other instanceof GenomeLoc)) {
         return false;
      } else {
         GenomeLoc otherGenomeLoc = (GenomeLoc)other;
         return this.contigIndex == otherGenomeLoc.contigIndex && this.start == otherGenomeLoc.start && this.stop == otherGenomeLoc.stop;
      }
   }

   public int hashCode() {
      return this.start << 16 + this.stop << 4 + this.contigIndex;
   }

   @Requires({"that != null"})
   @Ensures({"result == 0 || result == 1 || result == -1"})
   public final int compareContigs(GenomeLoc that) {
      if (this.contigIndex == that.contigIndex) {
         return 0;
      } else {
         return this.contigIndex > that.contigIndex ? 1 : -1;
      }
   }

   @Requires({"that != null"})
   @Ensures({"result == 0 || result == 1 || result == -1"})
   public int compareTo(GenomeLoc that) {
      int result = 0;
      if (this == that) {
         result = 0;
      } else if (isUnmapped(this)) {
         result = 1;
      } else if (isUnmapped(that)) {
         result = -1;
      } else {
         int cmpContig = this.compareContigs(that);
         if (cmpContig != 0) {
            result = cmpContig;
         } else {
            if (this.getStart() < that.getStart()) {
               result = -1;
            }

            if (this.getStart() > that.getStart()) {
               result = 1;
            }
         }
      }

      return result;
   }

   @Requires({"that != null"})
   public boolean endsAt(GenomeLoc that) {
      return this.compareContigs(that) == 0 && this.getStop() == that.getStop();
   }

   @Ensures({"result > 0"})
   public long size() {
      return (long)(this.stop - this.start + 1);
   }
}
