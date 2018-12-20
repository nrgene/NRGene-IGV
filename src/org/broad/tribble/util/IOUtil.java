package org.broad.tribble.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;

/**
 * Miscellaneous stateless static IO-oriented methods.
 */
public class IOUtil {
    /**
     * Wrap the given stream in a BufferedInputStream, if it isn't already wrapper
     * @param stream stream to be wrapped
     * @return A BufferedInputStream wrapping stream, or stream itself if stream instanceof BufferedInputStream.
     */
    public static BufferedInputStream toBufferedStream(final InputStream stream) {
        if (stream instanceof BufferedInputStream) {
            return (BufferedInputStream) stream;
        } else {
            return new BufferedInputStream(stream);
        }
    }

    /**
     * Delete a list of files, and write a warning message if one could not be deleted.
     * @param files Files to be deleted.
     */
    public static void deleteFiles(final File... files) {
        for (final File f : files) {
            if (!f.delete()) {
                System.err.println("Could not delete file " + f);
            }
        }
    }

    public static void deleteFiles(final Iterable<File> files) {
        for (final File f : files) {
            if (!f.delete()) {
                System.err.println("Could not delete file " + f);
            }
        }
    }
}