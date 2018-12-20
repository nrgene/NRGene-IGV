package org.broad.tribble.util;

import java.io.BufferedInputStream;
import java.io.IOException;

/**
 * A wrapper class to provide buffered read access to a SeekableStream.  Just wrapping such a stream with
 * a BufferedInputStream will not work as it does not support seeking.  In this implementation a
 * seek call is delegated to the wrapped stream, and the buffer reset.
 */
public class SeekableBufferedStream extends SeekableStream {

    public static final int DEFAULT_BUFFER_SIZE = 5120000;
    final private int bufferSize;
    BufferedInputStream bufferedStream;
    SeekableStream wrappedStream;
    long position;

    public SeekableBufferedStream(SeekableStream wrappedStream) {
        this(wrappedStream, DEFAULT_BUFFER_SIZE);
    }


    public SeekableBufferedStream(SeekableStream wrappedStream, int bufferSize) {
        this.bufferSize = bufferSize;
        this.wrappedStream = wrappedStream;
        this.position = 0;
        bufferedStream = new BufferedInputStream(wrappedStream, bufferSize);
    }

    public long length() {
        return wrappedStream.length();
    }

    public void seek(long position) throws IOException {
        this.position = position;
        wrappedStream.seek(position);
        bufferedStream = new BufferedInputStream(wrappedStream, bufferSize);
    }

    @Override
    public long position() throws IOException {
        return position;
    }


    public int read() throws IOException {
        int b = bufferedStream.read();
        position++;
        return b;
    }

    public int read(byte[] buffer, int offset, int length) throws IOException {
        int nBytesRead = bufferedStream.read(buffer, offset, length);
        if (nBytesRead > 0) {
            position += nBytesRead;
        }
        return nBytesRead;
    }

    public void close() throws IOException {
        wrappedStream.close();
    }

    public boolean eof() throws IOException {
        return position >= wrappedStream.length();
    }
}
