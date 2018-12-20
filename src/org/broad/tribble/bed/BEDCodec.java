package org.broad.tribble.bed;

import org.broad.tribble.AbstractFeatureCodec;
import org.broad.tribble.Feature;
import org.broad.tribble.annotation.Strand;
import org.broad.tribble.readers.LineReader;
import org.broad.tribble.util.ParsingUtils;

/**
 * @author jrobinso
 * Date: Dec 20, 2009
 */
public class BEDCodec extends AbstractFeatureCodec {

    // A buffer to hold parsed tokens
    String[] tokens = new String[15];

    public Feature decodeLoc(String line) {
        return decode(line);
    }

    /**
     * Convert a string to a BEDFeature.
     *
     * NOTE: this method is not thread safe due to the use of the shared "tokens" buffer.
     *
     * @param line the input line to decode
     * @return
     */
    public BEDFeature decode(String line) {

        if (line.trim().length() == 0 || line.startsWith("#") ||
                line.startsWith("track") || line.startsWith("browser")) {
            return null;
        }

        int tokenCount = ParsingUtils.splitWhitespace(line, tokens);

        // The first 3 columns are non optional for BED.  We will relax this
        // and only require 2.

        if (tokenCount < 2) {
            return null;
        }

        String chr = tokens[0];

        // The BED format uses a first-base-is-zero convention,  Tribble features use 1 => add 1.
        int start = Integer.parseInt(tokens[1]) + 1;

        int end = start;
        if (tokenCount > 2) {
            end = Integer.parseInt(tokens[2]);
        }

        FullBEDFeature feature = new FullBEDFeature(chr, start, end);

        // The rest of the columns are optional.  Stop parsing upon encountering
        // a non-expected value

        // Name
        if (tokenCount > 3) {
            String name = tokens[3].replaceAll("\"", "");
            feature.setName(name);
        }

        // Score
        if (tokenCount > 4) {
            try {
                float score = Float.parseFloat(tokens[4]);
                feature.setScore(score);
            } catch (NumberFormatException numberFormatException) {

                // Unexpected, but does not invalidate the previous values.
                // Stop parsing the line here but keep the feature
                // Don't log, would just slow parsing down.
                return feature;
            }
        }

        // Strand
        if (tokenCount > 5) {
            String strandString = tokens[5].trim();
            char strand = (strandString.length() == 0)
                    ? ' ' : strandString.charAt(0);

            if (strand == '-') {
                feature.setStrand(Strand.NEGATIVE);
            } else if (strand == '+') {
                feature.setStrand(Strand.POSITIVE);
            } else {
                feature.setStrand(Strand.NONE);
            }
        }

        if (tokenCount > 8) {
            String colorString = tokens[8];
            feature.setColor(ParsingUtils.parseColor(colorString));
        }

        // Coding information is optional
        if (tokenCount > 11) {
            createExons(start, tokens, feature, chr, feature.getStrand());
        }

        return feature;
    }

    /**
     * @return the type of feature we produce: BEDFeature
     */
    public Class getFeatureType() {
        return BEDFeature.class;
    }

    public Object readHeader(LineReader reader) {
        return null;  // BED files don't have a meaninful header
    }

    private void createExons(int start, String[] tokens, FullBEDFeature gene, String chr,
                             Strand strand) throws NumberFormatException {

        int cdStart = Integer.parseInt(tokens[6]) + 1;
        int cdEnd = Integer.parseInt(tokens[7]);

        int exonCount = Integer.parseInt(tokens[9]);
        String[] exonSizes = new String[exonCount];
        String[] startsBuffer = new String[exonCount];
        ParsingUtils.split(tokens[10], exonSizes, ',');
        ParsingUtils.split(tokens[11], startsBuffer, ',');

        int exonNumber = (strand == Strand.NEGATIVE ? exonCount : 1);

        if (startsBuffer.length == exonSizes.length) {
            for (int i = 0; i < startsBuffer.length; i++) {
                int exonStart = start + Integer.parseInt(startsBuffer[i]);
                int exonEnd = exonStart + Integer.parseInt(exonSizes[i]) - 1;
                gene.addExon(exonStart, exonEnd, cdStart, cdEnd, exonNumber);

                if (strand == Strand.NEGATIVE) {
                    exonNumber--;
                } else {
                    exonNumber++;
                }
            }
        }
    }

    public boolean canDecode(final String path) {
        return path.toUpperCase().endsWith(".BED");
    }
}
