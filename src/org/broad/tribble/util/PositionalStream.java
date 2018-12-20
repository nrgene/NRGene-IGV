package org.broad.tribble.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by IntelliJ IDEA.
 * User: depristo
 * Date: Oct 7, 2010
 * Time: 2:05:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class PositionalStream extends OutputStream implements Positional {
    OutputStream out = null;
    private long position = 0;

    public PositionalStream(OutputStream out) {
        this.out = out;
    }

//    public void	write(char[] cbuf) throws IOException {
//
//    }
//
//    public void	write(char[] cbuf, int off, int len) throws IOException {
//
//    }

    public void write(final byte[] bytes) throws IOException {
        write(bytes, 0, bytes.length);
    }

    public void write(final byte[] bytes, int startIndex, int numBytes) throws IOException {
        //System.out.println("write: " + bytes + " " + numBytes);
        position += numBytes;
        out.write(bytes, startIndex, numBytes);
    }

    public void write(int c)  throws IOException {
        System.out.println("write byte: " + c);
        //System.out.printf("Position %d for %c\n", position, (char)c);
        position++;
        out.write(c);
    }

//    public void write(String str)  throws IOException {
//
//    }
//
//    public void write(String str, int off, int len) throws IOException {
//        super.write(str, off, len);
//    }

    public long getPosition() { return position; }
}
