package org.broad.tribble.source.query;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.broad.igv.nrgene.api.ApiRequest;
import org.broad.tribble.TribbleException;
import org.broad.tribble.readers.AsciiLineReader;
import org.broad.tribble.readers.LineReader;
import org.broad.tribble.util.SeekableStreamFactory;

/**
 * a class that implements a plain iterator over the feature file; this allows
 * us to iterator over files that do not or cannot have an index.
 */
public class IndexFreeAsciiQuerySource implements QuerySource {

    // our input file - we keep this so that we're able to split off files later
    // NOTE -- this is not neccessarily a File (could be a URL).
    private final String inputFile;

    /**
     * A buffering line reader, used to control access to the index-free input stream.
     * Do NOT access this variable directly; use factory method getLineReader() to make
     * sure the line reader is lazy-loaded.
     */
    private BufferingLineReader lineReader;

    public IndexFreeAsciiQuerySource(String featureFile) throws IOException {
        this.inputFile = featureFile;
    }

    // create an iterator from the input file

    public LineReader iterate() throws IOException {
        return getLineReader();
    }

    /**
     * AsciiQuerySource does not support mark/reset.  Returns false always.
     *
     * @return false.
     */
    public boolean markSupported() {
        return true;
    }

    /**
     * Mark the position in the input stream to which to return.
     */
    public void mark() throws IOException {
        getLineReader().mark();
    }

    /**
     * AsciiQuerySource does not support mark/reset.  Throws UnsupportedOperationException.
     */
    public void reset() throws IOException {
        getLineReader().reset();
    }

    public LineReader query(String s, int i, int i1) {
    	if ( !ApiRequest.isApiUrl(inputFile) )
    		throw new UnsupportedOperationException("Unable to query from IndexFreeAsciiQuerySource");
    	else
    	{
    		return new LineReader() {
				
				@Override
				public String readLine() throws IOException {
					return null;
				}
				
				@Override
				public String getPath() {
					return inputFile;
				}
				
				@Override
				public void close() {
				}
			};
    	}
    }

    public void close() throws IOException {
	// Using lineReader variable directly to avoid the scenario where we lazy-load
	// the file just to turn around and close it later on.
	if(lineReader != null)
	    lineReader.close();
    }

    public List<String> getSequenceNames() {
    	if ( !ApiRequest.isNiu() )
    		throw new UnsupportedOperationException("Unable to query sequence names from IndexFreeAsciiQuerySource");
    	else
    		return new LinkedList<String>();
    }

    private BufferingLineReader getLineReader() throws IOException {
        if (lineReader == null) {
            InputStream is = SeekableStreamFactory.getStreamFor(inputFile);
            lineReader = new BufferingLineReader(new AsciiLineReader(is));
        }
        return lineReader;
    }

    private class BufferingLineReader implements LineReader {
        
    	String path;
    	
    	/**
         * The wrapped reader; source of line reader data.
         */
        private final LineReader wrappedReader;

        /**
         * A buffer for existing lines.
         */
        private final LinkedList<String> lineBuffer = new LinkedList<String>();

        /**
         * Whether the line reader is currently in a state where it's buffering.
         */
        private boolean isBuffering = false;

        /**
         * Create a new buffering reader wrapping the given line reader.
         *
         * @param wrappedReader The reader to wrap.
         */
        public BufferingLineReader(final LineReader wrappedReader) {
            this.wrappedReader = wrappedReader;
        }

        /**
         * Reads the next line from the input stream.
         *
         * @return Next available line from the input stream.
         * @throws IOException On error reading file / stream.
         */
        public String readLine() throws IOException {
            String currentLine;
            if (isBuffering) {
                currentLine = wrappedReader.readLine();
                lineBuffer.add(currentLine);
            } else
                currentLine = !lineBuffer.isEmpty() ? lineBuffer.remove() : wrappedReader.readLine();

            return currentLine;

        }

        public void close() {
            wrappedReader.close();
            lineBuffer.clear();
        }

        /**
         * Mark the position in the input stream to which to return.
         */
        public void mark() {
            isBuffering = true;
            lineBuffer.clear();
        }

        /**
         * Return to the previously marked point.
         */
        public void reset() {
            if (!isBuffering)
                throw new TribbleException("Reset called on BufferingLineReader without corresponding mark().");
            isBuffering = false;
        }

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}
    }
}
