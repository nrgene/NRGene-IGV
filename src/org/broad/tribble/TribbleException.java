package org.broad.tribble;


/**
 * @author Aaron
 *
 * The base Tribble exception; this allows external libraries to catch any exception Tribble generates
 * 
 */
public class TribbleException extends RuntimeException {
    // what file or input source we are working from
    String source;

    public TribbleException(String msg) {
        super(msg);
    }

    public TribbleException(String message, Throwable throwable) {
        super(message, throwable);
    }

    /**
     * set the source for the file; where we got lines from
     * @param source the source location, usually a file though it could be a http link or other source
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * override the default message with ours, which attaches the source file in question
     * @return a string with our internal error, along with the causitive source file (or other input source)
     */
    public String getMessage() {
        String ret = super.getMessage();
        if ( source != null )
            ret = ret + ", for input source: " + source;
        return ret;
    }

    // //////////////////////////////////////////////////////////////////////
    // other more specific exceptions generated in Tribble
    // //////////////////////////////////////////////////////////////////////


    // //////////////////////////////////////////////////////////////////////
    // Codec exception
    // //////////////////////////////////////////////////////////////////////
    // if the line to decode is incorrect
    public static class InvalidDecodeLine extends TribbleException {
        public InvalidDecodeLine(String message, String line) { super (message + ", line = " + line); }

        public InvalidDecodeLine(String message, int lineNo) { super (message + ", at line number " + lineNo); }
    }

    public static class InvalidHeader extends TribbleException {
        public InvalidHeader(String message) { super ("Your input file has a malformed header: " + message); }
    }

    // capture other internal codec exceptions
    public static class InternalCodecException extends TribbleException {
        public InternalCodecException(String message) { super (message); }
    }

    // //////////////////////////////////////////////////////////////////////
    // Index exceptions
    // //////////////////////////////////////////////////////////////////////
    public static class UnableToCreateCorrectIndexType extends TribbleException {
        public UnableToCreateCorrectIndexType(String message, Exception e) {
            super(message,e);
        }
        public UnableToCreateCorrectIndexType(String message) {
            super(message);
        }
    }

    // //////////////////////////////////////////////////////////////////////
    // Source exceptions
    // //////////////////////////////////////////////////////////////////////
    public static class FeatureFileDoesntExist extends TribbleException {
        public FeatureFileDoesntExist(String message, String file) {
            super(message);
            setSource(file);
        }
    }

    public static class MalformedFeatureFile extends TribbleException {
        public MalformedFeatureFile(String message, String f, Exception e) {
            super(message,e);
            setSource(f);
        }
        public MalformedFeatureFile(String message, String f) {
            super(message);
            setSource(f);
        }
    }

    public static class UnableToReadIndexFile extends TribbleException {
        public UnableToReadIndexFile(String message, String f, Exception e) {
            super(message,e);
            setSource(f);
        }
    }

    public static class TabixReaderFailure extends TribbleException {
        public TabixReaderFailure(String message, String f, Exception e) {
            super(message,e);
            setSource(f);
        }

        public TabixReaderFailure(String message, String f) {
            super(message);
            setSource(f);
        }
    }
}
