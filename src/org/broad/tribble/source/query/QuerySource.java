package org.broad.tribble.source.query;

import java.io.IOException;
import java.util.List;

import org.broad.tribble.readers.LineReader;

/**
 * Created by IntelliJ IDEA.
 * User: jrobinso
 * Date: May 17, 2010
 * Time: 10:06:25 PM
 * To change this template use File | Settings | File Templates.
 */
public interface QuerySource {
    LineReader iterate() throws IOException;

    /**
     * Marks the current position in the input stream for return by reset().
     */
    void mark() throws IOException;

    /**
     * Returns true if mark/reset pairing is supported; returns false otherwise.
     * @return
     */
    boolean markSupported();

    /**
     * Resets the current position in the stream to that from whence mark was originally called.
     */
    void reset() throws IOException;

    LineReader query(String chr, int start, int end);

    void close()  throws IOException;

    List<String> getSequenceNames();
}
