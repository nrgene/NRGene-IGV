package org.broad.tribble.exception;


/**
 * 
 * @author aaron 
 * 
 * Class CodecLineParsingException
 *
 * a generic exception we use if the codec has trouble parsing the line its given
 */
public class CodecLineParsingException extends RuntimeException {

    public CodecLineParsingException(Throwable cause) {
        super(cause);
    }

    public CodecLineParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public CodecLineParsingException(String message) {
        super(message);
    }

    public CodecLineParsingException() {
    }
}
