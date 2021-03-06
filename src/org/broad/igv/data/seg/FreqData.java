/*
 * Copyright (c) 2007-2011 by The Broad Institute of MIT and Harvard.  All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */

package org.broad.igv.data.seg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.broad.igv.Globals;
import org.broad.igv.feature.Chromosome;
import org.broad.igv.feature.LocusScore;
import org.broad.igv.feature.LocusScoreBase;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.track.WindowFunction;

/**
 * @author jrobinso
 * @date Oct 13, 2010
 */
public class FreqData {

    private static final float AMP_THRESHOLD = 0.1f;
    private static final float DEL_THRESHOLD = -0.1f;

    private int numberOfSamples;
    boolean logNormalized;
    Map<String, List<LocusScore>> amp;
    Map<String, List<LocusScore>> del;



    public FreqData(SegmentedDataSet ds, Genome genome) {
        compute(ds, genome);

    }


    void compute(SegmentedDataSet ds, Genome genome) {

        this.logNormalized = ds.isLogNormalized();


        // 200 kb bin size
        int binSize = 200000;
        int sizeInKB = (int) (genome.getLength() / 1000);
        int wgBinSize = sizeInKB / 700;
        int wgBinCount = sizeInKB / wgBinSize + 1;

        numberOfSamples = ds.getSampleNames().size();

        amp = new HashMap();
        del = new HashMap();

        for (String chr : genome.getChromosomeNames()) {
            Chromosome c = genome.getChromosome(chr);
            int len = c.getLength();
            int nBins = len / binSize + 1;
            List<LocusScore> ampBins = new ArrayList(nBins);
            List<LocusScore> delBins = new ArrayList(nBins);
            for (int i = 0; i < nBins; i++) {
                int s = i * binSize;
                int e = s + binSize;
                ampBins.add(new Bin(chr, s, e));
                delBins.add(new Bin(chr, s, e));
            }
            amp.put(chr, ampBins);
            del.put(chr, delBins);
        }

        // Whole genome bins
        List<LocusScore> ampBins = new ArrayList(wgBinCount);
        List<LocusScore> delBins = new ArrayList(wgBinCount);
        for (int i = 0; i < wgBinCount; i++) {
            int s = i * wgBinSize;
            int e = s + wgBinSize;
            ampBins.add(new Bin(Globals.CHR_ALL, s, e));
            delBins.add(new Bin(Globals.CHR_ALL, s, e));
        }
        amp.put(Globals.CHR_ALL, ampBins);
        del.put(Globals.CHR_ALL, delBins);


        for (String sample : ds.getSampleNames()) {
            for (String chr : genome.getChromosomeNames()) {
                List<LocusScore> segments = ds.getSegments(sample, chr);
                if (segments != null) {

                    for (LocusScore seg : segments) {
                        final float segScore = logNormalized ? seg.getScore() :
                                (float) (Math.log(seg.getScore() / 2) / Globals.log2);

                        if (segScore > AMP_THRESHOLD || segScore < DEL_THRESHOLD) {


                            int startBin = seg.getStart() / binSize;
                            int endBin = seg.getEnd() / binSize;
                            for (int b = startBin; b <= endBin; b++) {
                                if (b >= amp.get(chr).size()) {
                                    break;
                                }
                                binCounts(chr, seg.getStart(), seg.getEnd(), segScore, b, binSize);
                            }

                            int gStart = genome.getGenomeCoordinate(chr, seg.getStart());
                            int gEnd = genome.getGenomeCoordinate(chr, seg.getEnd());
                            int wgStartBin = gStart / wgBinSize;
                            int wgEndBin = gEnd / wgBinSize;
                            for (int b = wgStartBin; b <= wgEndBin; b++) {
                                if (b >= amp.get(Globals.CHR_ALL).size()) {
                                    break;
                                }
                                binCounts(Globals.CHR_ALL, gStart, gEnd, segScore, b, wgBinSize);
                            }
                        }
                    }
                }
            }
        }
    }

    private void binCounts(String chr, int segStart, int segEnd, float segScore, int b, int binSize) {

        int binStart = b * binSize;
        int binEnd = binStart + binSize;

        // Weight by % overlap with bin
        float weight = 1.0f;
        if (segEnd < binEnd) {
            int s = Math.max(segStart, binStart);
            weight = ((float) (segEnd - s)) / binSize;

        } else if (segStart > binStart) {
            int e = Math.min(segEnd, binEnd);
            weight = ((float) (e - segStart)) / binSize;
        }


        if (segScore > AMP_THRESHOLD) {
            Bin bin = (Bin) amp.get(chr).get(b);
            bin.increment(weight, weight * segScore);
        }

        if (segScore < DEL_THRESHOLD) {
            Bin bin = (Bin) del.get(chr).get(b);
            bin.increment(-weight, weight * segScore);
        }
    }

    // For testing

    public void dumpData(String chr) {

        System.out.println("track name=Amplifications");
        for (Map.Entry<String, List<LocusScore>> entry : amp.entrySet()) {
            if (!entry.getKey().equals(Globals.CHR_ALL)) {
                for (LocusScore bin : entry.getValue()) {
                    System.out.println(bin.getChr() + "\t" + bin.getStart() + "\t" + bin.getEnd() + "\t" + bin.getScore());
                }
            }
        }

        //System.out.println("track name=Deletions");
        //for(Bin bin : del.get(chr)) {
        //    System.out.println(bin.getChr() + "\t" + bin.getStart() + "\t" + bin.getEnd() + "\t" + bin.getCount());
        //}


    }

    public int getNumberOfSamples() {
        return numberOfSamples;
    }

    public List<LocusScore> getAmpCounts(String chr) {
        return amp.get(chr);
    }

    public List<LocusScore> getDelCounts(String chr) {
        return del.get(chr);
    }


    public  class Bin extends LocusScoreBase implements LocusScore {
        String chr;
        int start;
        int end;
        float count;
        private float totalCN;

        Bin(String chr, int start, int end) {
            this.chr = chr;
            this.start = start;
            this.end = end;
        }

        void increment(float count, float score) {
            this.count += count;
            totalCN = getTotalCN() + score;
        }


        float getCount() {
            return count;
        }

        float getAvgCN() {
            return count == 0 ? 0 : getTotalCN() / count;
        }


        public String getChr() {
            return chr;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }


        public void setStart(int start) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public void setEnd(int end) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public float getScore() {
            return getCount();
        }

        public LocusScore copy() {
            return this;  //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getValueString(double position, WindowFunction windowFunction) {
            int cnt = Math.abs(Math.round(count));
            int percent =  ((cnt * 100) / numberOfSamples);
            return cnt + " (" + percent + "%)";
        }

        public float getTotalCN() {
            return totalCN;
        }
    }


}
