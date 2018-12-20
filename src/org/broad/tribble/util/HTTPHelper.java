/*
 * Copyright (c) 2007-2011 by The Broad Institute of MIT and Harvard.  All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */

package org.broad.tribble.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

import org.apache.log4j.Logger;
import org.broad.igv.util.HttpUtils;

/**
 * Simple implementation of URLHelper based on the JDK URL and HttpURLConnection classes.  This
 * version optionally takes a proxy, but does not support authentication.
 *
 * @author jrobinso
 * @date Jun 28, 2011
 */
public class HTTPHelper implements URLHelper {

    private static Logger log = Logger.getLogger(HTTPHelper.class);

    /**
     * Global proxy setting -- shared by all instances
     */
    static Proxy proxy;

    private URL url;

    public HTTPHelper(URL url) {
        this.url = url;
        this.proxy = null;
    }

    public static synchronized void setProxy(Proxy p) {
        proxy = p;
    }

    public URL getUrl() {
        return url;
    }

    /**
     * @return content length of the resource
     * @throws IOException
     */
    public long getContentLength() throws IOException {

        HttpURLConnection con = null;
        try {
            con = openConnection();
            con.setRequestMethod("HEAD");
            if((con.getResponseCode() != HttpURLConnection.HTTP_OK)) {
                log.error("Error (" + con.getResponseMessage() + " ) fetching content length: " + url);
                return -1;
            }
            else {
                String contentLength = HttpUtils.getContentLengthFromConnection(con);
                return contentLength == null ? -1 : Long.parseLong(contentLength);
            }
        }
        finally {
            if (con != null) con.disconnect();
        }
    }


    /**
     * Open an input stream for the requested byte range.  Its the client's responsibility to close the stream.
     *
     * @param start start of range in bytes
     * @param end   end of range ni bytes
     * @return
     * @throws IOException
     */
    public InputStream openInputStreamForRange(long start, long end, boolean addAcceptOctetStreamHeader) throws IOException {
        HttpURLConnection connection = openConnection();
        String byteRange = "bytes=" + start + "-" + end;
        connection.setRequestProperty("Range", byteRange);
        if (addAcceptOctetStreamHeader) {
            connection.addRequestProperty("Accept", "application/octet-stream");
        }
        return new WrapperInputStream(connection, connection.getInputStream());
    }

    private HttpURLConnection openConnection() throws IOException {
        HttpURLConnection connection;
        if (proxy == null) {
            connection = (HttpURLConnection) url.openConnection();
        } else {
            connection = (HttpURLConnection) url.openConnection(proxy);
        }
        return connection;
    }

    public  boolean exists() {
        HttpURLConnection con = null;
        try {
            con = openConnection();
            con.setRequestMethod("HEAD");
            return (con.getResponseCode() == HttpURLConnection.HTTP_OK);
        }
        catch (Exception e) {
            log.error("Error connecting to url: " + url, e);
            return false;
        }
        finally {
            if (con != null) con.disconnect();
        }
    }
    
    class WrapperInputStream extends FilterInputStream {

        HttpURLConnection connection;

        protected WrapperInputStream(HttpURLConnection connection, InputStream inputStream) {
            super(inputStream);
            this.connection = connection;
        }

        @Override
        public void close() throws IOException {
            super.close();
            connection.disconnect();
        }
    }


}
