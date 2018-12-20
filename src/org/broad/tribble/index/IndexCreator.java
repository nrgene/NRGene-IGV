package org.broad.tribble.index;

import java.io.File;

import org.broad.tribble.Feature;

/**
 * User: jrobinso
 *
 * an interface for creating indexes
 */                                                                           
public interface IndexCreator {
    /**
     * initialize the index creator with the input file and the bin size.  Be warned, the bin size
     * is HIGHLY dependent on the index implementation; in one implementation 100 may result in excessively
     * large files, and other this may be too small for effective discernment between bins.  It's recommended to
     * use the defaultBinSize() function to get an appropriately sized bin.
     *
     * @param inputFile the input file
     * @param binSize the bin size
     */
    public void initialize(File inputFile, int binSize);

    /**
     * add a feature to the index
     * @param feature the feature, of which start, end, and contig must be filled in
     * @param filePosition the current file position, at the beginning of the specified feature
     */
    public void addFeature(Feature feature, long filePosition);

    /**
     * create the index, given the stream of features passed in to this point
     * @param finalFilePosition the final file position, for indexes that have to close out with the final position
     * @return an index object
     */
    public Index finalizeIndex(long finalFilePosition);

    /**
     * the default bin size for this index type; use this unless you're aware of the nuances of the particular index type.
     * @return the default bin size appropriate for this index type
     */
    public int defaultBinSize();

    /**
     * return the bin size of associated with the index with are creating
     * @return the index bin size
     */
    public int getBinSize();
}


