package org.broad.tribble.annotation;

/**
 * a public enum for Strand encoding
 */
public enum Strand {
    POSITIVE("+"), NEGATIVE("-"), NONE("!");  // not really sure what we should do for the NONE Enum

    // how we encode the strand information as text
    private String encoding;
    Strand(String str) {
        encoding = str;
    }

    /**
     * provide a way to take an encoding string, and produce a Strand
     * @param encoding the encoding string
     * @return a Strand object, if an appropriate one cannot be located an IllegalArg exception
     */
    public static Strand toStrand(String encoding) {
        for (Strand st : Strand.values())
            if (st.encoding.equals(encoding))
                return st;
        throw new IllegalArgumentException("Unable to match encoding to Strand enum for encoding string " + encoding);
    }

}