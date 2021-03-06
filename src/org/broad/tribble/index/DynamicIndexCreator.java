/*
 * Copyright (c) 2010, The Broad Institute
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.broad.tribble.index;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.broad.tribble.Feature;
import org.broad.tribble.TribbleException;
import org.broad.tribble.index.interval.IntervalIndexCreator;
import org.broad.tribble.index.linear.LinearIndexCreator;
import org.broad.tribble.util.MathUtils;

/**
 * Factory class for creating indexes, either new instances, or .  It is the responsibility of this class to determine and create the
 * correct index type from the input file or stream
 */

public class DynamicIndexCreator implements IndexCreator {
    private static Logger log = Logger.getLogger(DynamicIndexCreator.class);

    IndexFactory.IndexBalanceApproach iba;
    Map<IndexFactory.IndexType,IndexCreator> creators;

    // we're interested in two stats; the longest feature,
    int longestFeatureLength = 0;
    MathUtils.RunningStat stats = new MathUtils.RunningStat();
    // and the density of features
    long featureCount = 0;
    long basesSeen = 0;
    Feature lastFeature = null;
    File inputFile;

    public DynamicIndexCreator(IndexFactory.IndexBalanceApproach iba) {
        this.iba = iba;
    }

    public void initialize(File inputFile, int binSize) {
        // get a list of index creators
        this.inputFile = inputFile;
        creators = getIndexCreators(inputFile,iba);
    }

    public int defaultBinSize() { return -1; }
    public int getBinSize() { return -1; }

    /**
     * create an index, given an input file, codec,
     * @return a index
     */
    public Index finalizeIndex(long finalFilePosition) {
        // finalize all of the indexes
        log.debug("feature count == " + featureCount + ", number of bases seen == " + basesSeen);

        // return the score of the indexes we've generated
        Map<Double,IndexCreator> mapping = scoreIndexes((double)featureCount/(double)basesSeen, creators, longestFeatureLength, iba);
        IndexCreator creator = getMinIndex(mapping, this.iba);

        // Now let's finalize and create the index itself
        Index idx = creator.finalizeIndex(finalFilePosition);
        idx.finalizeIndex();

        // add our statistics to the file
        idx.addProperty("FEATURE_LENGTH_MEAN",String.valueOf(stats.mean()));
        idx.addProperty("FEATURE_LENGTH_STD_DEV",String.valueOf(stats.standardDeviation()));
        idx.addProperty("MEAN_FEATURE_VARIANCE",String.valueOf(stats.variance()));

        // add the feature count
        idx.addProperty("FEATURE_COUNT",String.valueOf(featureCount));

        return idx;
    }

    /**
     * create a list of index creators (initialized) representing the common index types we'd suspect they'd like to use
     * @param inputFile the input file to use to create the indexes
     * @return a map of index type to the best index for that balancing approach
     */
    private Map<IndexFactory.IndexType,IndexCreator> getIndexCreators(File inputFile, IndexFactory.IndexBalanceApproach iba) {
        Map<IndexFactory.IndexType,IndexCreator> creators = new HashMap<IndexFactory.IndexType,IndexCreator>();

        if (iba == IndexFactory.IndexBalanceApproach.FOR_SIZE) {
            // add a linear index with the default bin size
            LinearIndexCreator linearNormal = new LinearIndexCreator();
            linearNormal.initialize(inputFile, linearNormal.defaultBinSize());
            creators.put(IndexFactory.IndexType.LINEAR,linearNormal);

            // create a tree index with the default size
            IntervalIndexCreator treeNormal = new IntervalIndexCreator();
            treeNormal.initialize(inputFile, treeNormal.defaultBinSize());
            creators.put(IndexFactory.IndexType.INTERVAL_TREE,treeNormal);
        }

        // this section is a little more arbitrary; we're creating indexes with a bin size that's a portion of the default; these
        // values were determined experimentally
        if (iba == IndexFactory.IndexBalanceApproach.FOR_SEEK_TIME) {
            // create a linear index with a small bin size
            LinearIndexCreator linearSmallBin = new LinearIndexCreator();
            linearSmallBin.initialize(inputFile, Math.max(200, linearSmallBin.defaultBinSize() / 4));
            creators.put(IndexFactory.IndexType.LINEAR,linearSmallBin);

            // create a tree index with a small index size
            IntervalIndexCreator treeSmallBin = new IntervalIndexCreator();
            treeSmallBin.initialize(inputFile, Math.max(20, treeSmallBin.defaultBinSize() / 8));
            creators.put(IndexFactory.IndexType.INTERVAL_TREE,treeSmallBin);
        }

        return creators;
    }


    /**
     * create an index, given an iterator of features, and a index balancing approach.
     *
     * This method is split off to make testing a lot easier
     */
    public void addFeature(Feature f, long filePosition) {
        // protected static Map<Double,Index> createIndex(FileBasedFeatureIterator<Feature> iterator, Map<IndexType,IndexCreator> creators, IndexBalanceApproach iba) {
        // feed each feature to the indexes we've created
        // first take care of the stats
        featureCount++;

        // calculate the number of bases seen - we have to watch out for the situation where the last record was on the previous chromosome
        basesSeen = (lastFeature == null) ? basesSeen + f.getStart() :
                ((f.getStart() - lastFeature.getStart() >= 0) ? basesSeen + (f.getStart() - lastFeature.getStart()) : basesSeen + f.getStart());

        longestFeatureLength = Math.max(longestFeatureLength,(f.getEnd()-f.getStart()) + 1);

        // push the longest feature to the running stats
        stats.push(longestFeatureLength);

        // now feed the feature to each of our creators
        for (IndexCreator creator : creators.values()) {
            creator.addFeature(f,filePosition);
        }

        // if the last feature is after the current feature, exception out
        if (lastFeature != null && f.getStart() < lastFeature.getStart() && lastFeature.getChr().equals(f.getChr()))
            throw new TribbleException.MalformedFeatureFile("We saw a record with a start of " + f.getChr() + ":" + f.getStart() +
                    " after a record with a start of " + lastFeature.getChr() + ":" + lastFeature.getStart(), inputFile.getAbsolutePath());

        // save the last feature
        lastFeature = f;
    }

    /**
     * score the available indexes for the specified density and feature lengths
     *
     * The scoring method is trying to determine how many features would be returned for a sample one base query; or:
     * (features/seek).  For the interval index this is clear: it's the bin size (interval is binned by feature count).
     * for Linear indexes it's the density of features X the number of bins we need to retrieve (which is determined
     * by the bin size X the longest feature).
     *
     * @param densityOfFeatures the density of features (features/base)
     * @param indexes the list of indexes we've seen
     * @param longestFeature the longest feature we've found
     * @param iba the index balancing approach
     * @return the best index available for the target indexes
     */
    protected static LinkedHashMap<Double,IndexCreator> scoreIndexes(double densityOfFeatures, Map<IndexFactory.IndexType,IndexCreator> indexes, int longestFeature, IndexFactory.IndexBalanceApproach iba) {
        if (indexes.size() < 1) throw new IllegalArgumentException("Please specify at least one index to evaluate");

        LinkedHashMap<Double,IndexCreator> scores = new LinkedHashMap<Double,IndexCreator>();
        log.debug("density == " + densityOfFeatures + " longest feature == " + longestFeature);

        for (Map.Entry<IndexFactory.IndexType,IndexCreator> entry : indexes.entrySet()) {
            // we have different scoring
            if (entry.getValue() instanceof LinearIndexCreator) {
                double binSize = entry.getValue().getBinSize();
                scores.put(binSize*densityOfFeatures*Math.ceil((double)longestFeature/binSize),entry.getValue() );
                log.debug("score for Linear == " + (binSize*densityOfFeatures*Math.ceil((double)longestFeature/binSize)));
            } else if (entry.getValue() instanceof IntervalIndexCreator) {
                scores.put((double)entry.getValue().getBinSize(), entry.getValue() );
                log.debug("score for Interval == " + ((double)((IntervalIndexCreator)entry.getValue()).getBinSize()));
            } else {
                throw new TribbleException.UnableToCreateCorrectIndexType("Unknown index type, we don't have a scoring method for " + entry.getValue().getClass());
            }
        }
        return scores;
    }

    /**
     * utility function to find the min of a list
     * @param scores the list of scaled features/bin scores for each index type
     * @return the best score <b>index value</b>
     */
    private IndexCreator getMinIndex(Map<Double,IndexCreator> scores, IndexFactory.IndexBalanceApproach iba) {
        TreeMap<Double,IndexCreator> map = new TreeMap<Double,IndexCreator>();
        map.putAll(scores);
        
        // if we are optimizing for seek time, choose the lowest score (adjusted features/bin value), if for storage size, choose the opposite
        IndexCreator idx = (iba != IndexFactory.IndexBalanceApproach.FOR_SEEK_TIME) ? map.get(map.lastKey()) : map.get(map.firstKey());
        log.debug("creating index of type " + idx.getClass().getSimpleName() + " with bin size " + idx.getBinSize());
        return idx;
    }
}
