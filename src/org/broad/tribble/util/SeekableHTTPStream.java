/*
 * Copyright (c) 2007-2009 by The Broad Institute, Inc. and the Massachusetts Institute of Technology.
 * All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL), Version 2.1 which
 * is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR WARRANTIES OF
 * ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING, WITHOUT LIMITATION, WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT
 * OR OTHER DEFECTS, WHETHER OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR
 * RESPECTIVE TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES OF
 * ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES, ECONOMIC
 * DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER THE BROAD OR MIT SHALL
 * BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT SHALL KNOW OF THE POSSIBILITY OF THE
 * FOREGOING.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.broad.tribble.util;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.log4j.Logger;

/**
 * @author jrobinso
 */
public class SeekableHTTPStream extends SeekableStream {

    static Logger log = Logger.getLogger(SeekableHTTPStream.class);
    private final boolean addAcceptOctetStreamHeader;

    private long position = 0;
    private long contentLength = -1;
    URLHelper helper;

    public SeekableHTTPStream(URLHelper helper) {
        this(helper, false);
    }

    public SeekableHTTPStream(URLHelper helper, boolean addAcceptOctetStreamHeader) {
        this.helper = helper;
        this.addAcceptOctetStreamHeader = addAcceptOctetStreamHeader;
        try {
            contentLength = helper.getContentLength();
        } catch (IOException e) {
            log.error("Error fetching content length for URL: " + helper.getUrl(), e);
        }
    }

    public void seek(long position) {
        this.position = position;
    }

    public long position() {
        return position;
    }

    public long length() {
        return contentLength;
    }


    @Override
    public long skip(long n) throws IOException {
        long bytesToSkip = Math.min(n, contentLength - position);
        position += bytesToSkip;
        return bytesToSkip;
    }

    
    @Override
    /**
     * Reads up to "len" bytes of data.  Tee the general contract for this method in class InputStream
     *
     * @param buffer - the array into which the data is read
     * @param offset - the start offset in buffer at which the data is writen
     * @param len - the maximum number of bytes to read
     * @return
     * @throws IOException
     */
    public int read(byte[] buffer, int offset, int len) throws IOException {

        if (log.isDebugEnabled()) {
            log.debug("read: " + offset + " " + len);
        }
        if (offset < 0 || len < 0 || (offset + len) > buffer.length) {
            throw new IndexOutOfBoundsException();
        }

        if (len == 0) {
            return 0;
        }


        InputStream is = null;
        int n = 0;
        try {

            long end = position + len - 1;

            // If we know the total content length, limit the byte-range requested
            if (contentLength > 0) {
                if (position >= contentLength) {
                    log.info("Warning: Unexpected position value.  position=" + position + " contentLength=" +
                             contentLength);
                    return -1;
                }
                // position is <= contentLength
                if (end >= contentLength) {
                    end = contentLength - 1;
                    len = (int) (end - position + 1);
                }
            }

            if(len <= 0) {
                return -1;
            }

            is = helper.openInputStreamForRange(position, end, addAcceptOctetStreamHeader);

            while (n < len) {
                int count = is.read(buffer, offset + n, len - n);
                if (count < 0) {
                    if (n == 0) {
                        return -1;
                    } else {
                        break;
                    }
                }
                n += count;
            }

            position += n;

            return n;

        }

        catch (IOException e) {
            // THis is a bit of a hack, but its not clear how else to handle this.  If a byte range is specified
            // that goes past the end of the file the response code will be 416.  The MAC os translates this to
            // an IOException with the 416 code in the message.  Windows translates the error to an EOFException.
            //
            //  The BAM file iterator  uses the return value to detect end of file (specifically looks for n == 0).
            if (e.getMessage().contains("416") || (e instanceof EOFException)) {
                log.error("Error: " + e.getMessage() + " encountered reading " + this.helper.getUrl()   +
                        " content-length=" + contentLength);
                if (n < 0) {
                    return -1;
                } else {
                    position += n;
                    return n;
                }
            } else {
                throw e;
            }
        }

        finally {
            if (is != null) {
                is.close();
            }
        }
    }


    public void close() throws IOException {
        BufferedInputStream bis;
        // Nothing to do
    }

    public int read() throws IOException {
        throw new UnsupportedOperationException("read() is not supported on SeekableHTTPStream.  Must read in blocks.");
    }

    /**
     * If we know the content length compare the position, otherwise return false (we can't know)
     *
     * @return
     * @throws IOException
     */
    public boolean eof() throws IOException {
        return contentLength > 0 && position >= contentLength;
    }


}
