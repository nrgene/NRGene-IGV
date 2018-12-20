package org.broad.tribble.util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.URL;

import org.apache.log4j.Logger;

/**
 * @author jrobinso
 * @date Nov 30, 2009
 */
public class SeekableStreamFactory {

    private static Logger log = Logger.getLogger(SeekableStreamFactory.class);
    private static Class httpHelperClass = HTTPHelper.class;

    public static SeekableStream getStreamFor(String path) throws IOException {
        return getStreamFor(path, false);
    }

    public static SeekableStream getStreamFor(String path, boolean addAcceptOctetStreamHeader) throws IOException {
        // todo -- add support for SeekableBlockInputStream

        if (path.startsWith("http:") || path.startsWith("https:")) {
            final URL url = new URL(path);
            return getHttpStream(url, addAcceptOctetStreamHeader);

        } else if (path.startsWith("ftp:")) {
            return new SeekableFTPStream(new URL(path));
        } else {
            return new SeekableFileStream(new File(path));
        }
    }

    public static SeekableStream getHttpStream(URL url) {
        return getHttpStream(url, false);
    }

    public static SeekableStream getHttpStream(URL url, boolean addAcceptOctetStreamHeader) {
        try {
            URLHelper helper = getURLHelper(url);
            return new SeekableHTTPStream(helper, addAcceptOctetStreamHeader);
        } catch (Exception e) {
            log.error("Error creating URL helper: ", e);
            throw new RuntimeException("Error creating URL helper: " + e.toString());
        }
    }

    public static void registerHelperClass(Class helperClass) {
        if (!helperClass.isAssignableFrom(URLHelper.class)) {
            // TODO -- throw exception here.  Also check that class implements the required constructor
        }
        httpHelperClass = helperClass;

    }

    public static URLHelper getURLHelper(URL url) {
        try {
            Constructor constr = httpHelperClass.getConstructor(URL.class);
            URLHelper helper = (URLHelper) constr.newInstance(url);
            return helper;
        } catch (Exception e) {
            log.error("Error instantiating url helper for class: " + httpHelperClass, e);
            return new HTTPHelper(url);
        }
    }
    
    public static SeekableStream getBufferedStream(SeekableStream stream){
    	return getBufferedStream(stream, SeekableBufferedStream.DEFAULT_BUFFER_SIZE);
    }

      public static SeekableStream getBufferedStream(SeekableStream stream, int bufferSize){
         return new SeekableBufferedStream(stream, bufferSize);
     }

}
