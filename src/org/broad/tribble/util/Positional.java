package org.broad.tribble.util;

/**
 * User: depristo
 * Date: Oct 7, 2010
 * Time: 10:53:20 AM
 *
 * Minimal interface for an object at support getting the current position in the stream / writer / file.
 *
 * The constrain here is simple.  If you are a output stream / writer, and you've written 50 bytes to the stream,
 * then getFilePointer() should return 50 bytes.  If you are an input stream or file reader, and you've read
 * 25 bytes from the object, then getFilePointer() should return 25.
 */
public interface Positional {
    /**
     * @return the current offset, in bytes, in the stream / writer / file.
     */
    public long getPosition();
}
