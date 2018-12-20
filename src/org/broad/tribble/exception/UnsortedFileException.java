package org.broad.tribble.exception;

/**
 * an exception for when we've discovered that an input file is unsorted; sorted files are required by Tribble
 */
public class UnsortedFileException extends RuntimeException {

    public UnsortedFileException(Throwable cause) {
        super(cause);
    }

    public UnsortedFileException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsortedFileException(String message) {
        super(message);
    }

    public UnsortedFileException() {
    }
}