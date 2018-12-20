package org.broad.tribble.source.tabix;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.broad.tribble.TribbleException;
import org.broad.tribble.readers.LineReader;
import org.broad.tribble.source.query.QuerySource;

/**
 * this class allows us to wrap calls to the untouched source code from Heng, which makes sending him bug reports easier
 */
public final class TabixLineReader implements LineReader, QuerySource {
    // the reader
    private final TabixReader reader;
    String path;

    // where we got the file from
    private final String source;
    /**
     * create a tabix line reader given an input source
     * @param source
     */
    public TabixLineReader(String source) {
        this.source = source;
        try {
            reader = new TabixReader(source);
        } catch (IOException e) {
            e.printStackTrace();
            throw new TribbleException.TabixReaderFailure("Unable to generate TabixReader given the input source ",source,e);
        }
    }

    public String getSource() {
        return source;
    }

    /**
     * read a line from the reader
     * @return a string, representing a line of text
     * @throws IOException an exception, thrown when we are unable to read a line from the underlying tabix reader
     */

    public String readLine() throws IOException {
        return reader.readLine();
    }

    public LineReader iterate() throws IOException {
        return this;
        //throw new UnsupportedOperationException("Unable iterate with TABIX, you must query the file");
    }

    /**
     * TABIX does not support mark/reset.  Throws UnsupportedOperationException.
     */
    public void mark() { throw new UnsupportedOperationException("Unable to mark/reset position in a TABIX line reader"); }

    /**
     * TABIX does not support mark/reset.  Returns false always.
     * @return false.
     */
    public boolean markSupported() {
        return false;
    }

    /**
     * TABIX does not support mark/reset.  Throws UnsupportedOperationException.
     */
    public void reset() { 
        throw new UnsupportedOperationException("Unable to mark/reset position in a TABIX line reader");
    }


    public LineReader query(String chr, int start, int end) {
        List<String> mp = getSequenceNames();
        if (mp == null) throw new TribbleException.TabixReaderFailure("Unable to find contig named " + chr + " in the tabix index",source);
        if (!mp.contains(chr)) {
            return null;
        }

        return new TabixIteratorToLineReader(reader.query(reader.mChr2tid.get(chr),start-1,end));
    }

    /**
     * close the TabixReader and any underlying files it has open
     */
    public void close() {
        try {
            reader.mFp.close();
        } catch (IOException e) {
            throw new TribbleException("Unable to close file source " + source,e);
        }
    }

    public List<String> getSequenceNames() {
        return new ArrayList<String>(reader.mChr2tid.keySet());
    }

    /**
     * get a list of the contig strings in this index
     * @return a List<String> with the current indexed contig names
     */
    public List<String> getContigNames() {
        return new ArrayList<String>(reader.mChr2tid.keySet());
    }

    class TabixIteratorToLineReader implements LineReader {
        private final TabixReader.Iterator it;
        String path;
        public TabixIteratorToLineReader(TabixReader.Iterator it) {
            this.it = it;
        }

        public String readLine() throws IOException {
            if (it == null) return null;
            return it.next();
        }

        public void close() {
            // nada
        }

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}
    }

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
