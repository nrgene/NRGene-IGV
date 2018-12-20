package org.broad.tribble.source.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.broad.tribble.Tribble;
import org.broad.tribble.index.Block;
import org.broad.tribble.index.Index;
import org.broad.tribble.index.IndexFactory;
import org.broad.tribble.readers.AsciiLineReader;
import org.broad.tribble.readers.LineReader;
import org.broad.tribble.source.BasicFeatureSource;
import org.broad.tribble.util.SeekableStream;
import org.broad.tribble.util.SeekableStreamFactory;

/**
 * Created by IntelliJ IDEA.
 * User: jrobinso
 * Date: May 17, 2010
 * Time: 3:26:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class AsciiQuerySource implements QuerySource {

    private static Logger log = Logger.getLogger(BasicFeatureSource.class);

    protected SeekableStream seekableStream;
    protected Index index;
    long markPosition;

    public AsciiQuerySource(String featureFile, String indexFile) throws IOException {
        seekableStream = SeekableStreamFactory.getStreamFor(featureFile);
        index = IndexFactory.loadIndex(indexFile);
    }

    public AsciiQuerySource(String featureFile) throws IOException {
        String indexFile = Tribble.indexFile(featureFile);
        seekableStream = SeekableStreamFactory.getStreamFor(featureFile);
        index = IndexFactory.loadIndex(indexFile);
    }

    public AsciiQuerySource(String featureFile, Index indexInstance) throws IOException {
        seekableStream = SeekableStreamFactory.getStreamFor(featureFile);
        this.index = indexInstance;
    }


    public void close() throws IOException {
        if (seekableStream != null) {
            seekableStream.close();
        }
    }

    public LineReader iterate() throws IOException {
        seekableStream.seek(0);
        return new AsciiLineReader(seekableStream);
    }

    /**
     * Mark the current stream position
     */
    public void mark() {
        try {
            markPosition = seekableStream.position();
        } catch (IOException e) {
            log.error("Error setting mark", e);
        }
    }

    /**
     * AsciiQuerySource does not support mark/reset.  Returns false always.
     * @return false.
     */
    public boolean markSupported() {
        return true;
    }

    /**
     * AsciiQuerySource does not support mark/reset.  Throws UnsupportedOperationException.
     */
    public void reset()  {
        try {
            seekableStream.seek(markPosition);
        } catch (IOException e) {
            log.error("Error resetting position", e);
        }
    }


    public LineReader query(String chr, int start, int end) {
        return new IndexedReader(chr, start, end);
    }

    public List<String> getSequenceNames() {
        return new ArrayList<String>(index.getSequenceNames());
    }

    public class IndexedReader implements LineReader {
        String chr;
        int start;
        int end;
        String path;

        AsciiLineReader reader;
        List<Block> blocks;

        // Return a reader to loop over the whole file
        public IndexedReader() {
            reader = new AsciiLineReader(seekableStream);
        }

        // Return a reader to loop over to stream over a query interval
        public IndexedReader(String chr, int start, int end) {
            this.chr = chr;
            this.start = start;
            this.end = end;
            init();
        }

        public String readLine() throws IOException {
            return reader == null ? null : reader.readLine();
        }

        public void close() {
            if ( reader != null )
                reader.close();
        }

        /**
         * Initialize the reader
         */
        private void init() {
            if (index == null)
                throw new UnsupportedOperationException("Files must be indexed to support query methods");

            if ( index.containsChromosome(chr) ) {
                blocks = index.getBlocks(chr, start, end);
                //log.info(String.format("Query %s %d-%d resulting in %d blocks (first start is %d) for index %s",
                //        chr, start, end, blocks != null ? blocks.size() : -1, blocks != null ? blocks.get(0).getStartPosition() : -1, index));

                if ( ! blocks.isEmpty() ) {
                    Block firstBlock = blocks.get(0);
                    try {
                        seekableStream.seek(firstBlock.getStartPosition());
                        reader = new AsciiLineReader(seekableStream);
                    } catch (IOException ex) {
                        log.error("Error seeking to position: " + firstBlock.getStartPosition(), ex);
                        // TODO -- throw application exception?
                    }
                }

            }
        }

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}
    }
}
