package org.broad.tribble.index;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

import org.apache.log4j.Logger;
import org.broad.tribble.Feature;
import org.broad.tribble.FeatureCodec;
import org.broad.tribble.TribbleException;
import org.broad.tribble.index.interval.IntervalIndexCreator;
import org.broad.tribble.index.interval.IntervalTreeIndex;
import org.broad.tribble.index.linear.LinearIndex;
import org.broad.tribble.index.linear.LinearIndexCreator;
import org.broad.tribble.readers.AsciiLineReader;
import org.broad.tribble.util.LittleEndianInputStream;
import org.broad.tribble.util.Positional;
import org.broad.tribble.util.SeekableStream;
import org.broad.tribble.util.SeekableStreamFactory;

/**
 * Factory class for creating indexes, either new instances, or .  It is the responsibility of this class to determine and create the
 * correct index type from the input file or stream
 */

public class IndexFactory {
    private static Logger log = Logger.getLogger(IndexFactory.class);

    public enum IndexBalanceApproach {
        FOR_SIZE,
        FOR_SEEK_TIME
    }

    /**
     * an enum that contains all of the information about the index types, and how to create them
     */
    public enum IndexType {
        LINEAR(1, new LinearIndexCreator(), LinearIndex.class),
        INTERVAL_TREE(2, new IntervalIndexCreator(), IntervalTreeIndex.class);
        private final int indexValue;
        private final IndexCreator indexCreator;

        private final Class indexType;

        IndexType(int headerValue, IndexCreator creator, Class indexClass) {
            indexValue = headerValue;
            indexCreator = creator;
            indexType = indexClass;
        }

        public int getHeaderValue() {
            return indexValue;
        }

        public IndexCreator getIndexCreator() {
            return indexCreator;
        }

        public Class getIndexType() {
            return indexType;
        }

        public static IndexType getIndexType(int headerValue) {
            for (IndexType type : IndexType.values())
                if (type.indexValue == headerValue) return type;
            throw new TribbleException.UnableToCreateCorrectIndexType("Unknown index type value" + headerValue);
        }
    }


    /**
     * Load in index from the specified file.   The type of index (LinearIndex or IntervalTreeIndex) is determined
     * at run time by reading the type flag in the file.
     *
     * @param indexFile
     * @return
     */
    public static Index loadIndex(String indexFile) {
        Index idx = null;
        InputStream is = null;
        LittleEndianInputStream dis = null;
        try {
            SeekableStream seekableStream = SeekableStreamFactory.getStreamFor(indexFile, true);
            if (indexFile.endsWith(".gz")) {
                is = new BufferedInputStream(new GZIPInputStream(seekableStream));
            } else {
                is = new BufferedInputStream(seekableStream);
            }

            dis = new LittleEndianInputStream(is);

            // Read the type and version,  then create the appropriate type
            int magicNumber = dis.readInt();
            int type = dis.readInt();
            Class indexClass = IndexType.getIndexType(type).getIndexType();

            idx = (Index) indexClass.newInstance();
            idx.read(dis);
        } catch (IOException ex) {
            throw new TribbleException.UnableToReadIndexFile("Unable to read index file", indexFile, ex);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            try {
                if (is != null) is.close();
                if (dis != null) dis.close();
                //log.info(String.format("Closed %s and %s", is, dis));
            } catch (IOException e) {
                log.error("Error closing indexFile: " + indexFile, e);
            }
        }
        return idx;
    }

    /**
     * create an index, given an input file, codec,
     *
     * @param inputFile the input file to load features from
     * @param codec     the codec to use for decoding records
     * @param iba       the index balancing approach
     * @return a index
     */
    public static Index createIndex(File inputFile, FeatureCodec codec, IndexBalanceApproach iba) {
        // get a list of index creators
        return createIndex(inputFile, new FeatureIterator(inputFile, codec), new DynamicIndexCreator(iba));
    }

    public static Index createIndex(File inputFile, FeatureCodec codec) {
        // get a list of index creators
        return createIndex(inputFile, codec, IndexBalanceApproach.FOR_SEEK_TIME);
    }

    public static Index createIndex(File inputFile, FeatureIterator iterator, IndexCreator creator) {
        creator.initialize(inputFile, creator.defaultBinSize());

        while (iterator.hasNext()) {
            long position = iterator.getPosition();
            creator.addFeature(iterator.next(), position);
        }

        return creator.finalizeIndex(iterator.getPosition());
    }

    /**
     * a helper to make an index of a specified type, without balancing, and return
     *
     * @param inputFile the input file
     * @param codec     the codec
     * @param creator   the index creator to use
     * @return
     */
    private static Index createIndexOfType(File inputFile, FeatureCodec codec, IndexCreator creator) {
        return createIndex(inputFile, new FeatureIterator(inputFile, codec), creator);
    }

    /**
     * a helper method for creating the basic linear index
     *
     * @param inputFile the input file to load features from
     * @param codec     the codec to use for decoding records
     * @return a index
     */
    public static Index createLinearIndex(File inputFile, FeatureCodec codec) {
        LinearIndexCreator idx = new LinearIndexCreator();
        idx.initialize(inputFile, idx.defaultBinSize());
        return createIndexOfType(inputFile, codec, idx);
    }

    /**
     * a helper method for creating the basic linear index
     *
     * @param inputFile the input file to load features from
     * @param codec     the codec to use for decoding records
     * @return a index
     */
    public static Index createLinearIndex(File inputFile, FeatureCodec codec, int binSize) {
        LinearIndexCreator idx = new LinearIndexCreator();
        idx.initialize(inputFile, binSize);
        return createIndexOfType(inputFile, codec, idx);
    }

    /**
     * a helper method for creating the basic tree index
     *
     * @param inputFile the input file to load features from
     * @param codec     the codec to use for decoding records
     * @return a index
     */
    public static Index createIntervalIndex(File inputFile, FeatureCodec codec) {
        IntervalIndexCreator idx = new IntervalIndexCreator();
        idx.initialize(inputFile, idx.defaultBinSize());
        return createIndexOfType(inputFile, codec, idx);
    }

    /**
     * a helper method for creating the basic tree index
     *
     * @param inputFile the input file to load features from
     * @param codec     the codec to use for decoding records
     * @return a index
     */
    public static Index createIntervalIndex(File inputFile, FeatureCodec codec, int binSize) {
        IntervalIndexCreator idx = new IntervalIndexCreator();
        idx.initialize(inputFile, binSize);
        return createIndexOfType(inputFile, codec, idx);
    }
}


/**
 * a helper class, that lets us inject testing iterators into the processes for unit testing
 */
class FeatureIterator implements Iterator<Feature>, Iterable<Feature>, Positional {

    // the ascii line reader we use to get features
    private AsciiLineReader reader;
    // the next feature
    private Feature nextFeature;
    // our codec
    private final FeatureCodec codec;
    private final File inputFile;

    // we also need cache our position
    private long cachedPosition;

    public FeatureIterator(File inputFile, FeatureCodec codec) {
        FileInputStream is = null;
        this.codec = codec;
        this.inputFile = inputFile;
        try {
            is = new FileInputStream(inputFile);
        } catch (FileNotFoundException e) {
            throw new TribbleException.FeatureFileDoesntExist("Unable to open the input file, most likely the file doesn't exist.", inputFile.getAbsolutePath());
        }
        reader = new AsciiLineReader(is);

        // make sure to read the header off first
        codec.readHeader(reader);
        cachedPosition = 0;
        readNextFeature();
    }

    // the standard iterator methods

    public boolean hasNext() {
        return nextFeature != null;
    }

    public Feature next() {
        Feature ret = nextFeature;
        readNextFeature();
        return ret;
    }

    public void remove() {
        throw new UnsupportedOperationException("We cannot remove");
    }

    public Iterator<Feature> iterator() {
        return this;
    }

    // get the file position from the underlying reader

    public long getPosition() {
        return (hasNext()) ? cachedPosition : reader.getPosition();
    }

    // read the next feature from the ascii line reader

    private void readNextFeature() {
        cachedPosition = reader.getPosition();
        try {
            String nextLine = null;
            do {
                nextLine = reader.readLine();
                if (nextLine != null) {
                    nextFeature = codec.decodeLoc(nextLine);
                } else {
                    nextFeature = null;
                }
            } while (nextFeature == null && nextLine != null);
        } catch (IOException e) {
            throw new TribbleException.MalformedFeatureFile("Unable to read a line from the file", inputFile.getAbsolutePath(), e);
        }
    }
}
