package org.broad.tribble.gelitext;

import java.util.Arrays;

import org.broad.tribble.AbstractFeatureCodec;
import org.broad.tribble.Feature;
import org.broad.tribble.annotation.DiploidGenotype;
import org.broad.tribble.exception.CodecLineParsingException;
import org.broad.tribble.readers.LineReader;


/**
 * @author aaron
 *         <p/>
 *         a codec for parsing geli text files, which is the text version of the geli binary format.
 *         <p/>
 *
 *         GELI text has the following tab-seperated fields:
 *         contig             the contig (string)
 *         position           the position on the contig (long)
 *         refBase            the reference base (char)
 *         depthOfCoverage    the depth of coverage at this position (int)
 *         maximumMappingQual the maximum mapping quality of a read at this position (int)
 *         genotype           the called genotype (string)
 *         LODBestToReference the LOD score of the best to the reference (double)
 *         LODBestToNext      the LOD score of the best to the next best genotype (double)
 *         likelihoods        the array of all genotype likelihoods, in ordinal ordering (array of 10 doubles, in ordinal order)
 */
public class GeliTextCodec extends AbstractFeatureCodec {
    private String[] parts;

    public Feature decodeLoc(String line) {
        return decode(line);
    }

    /**
     * Decode a line as a Feature.
     *
     * @param line
     *
     * @return Return the Feature encoded by the line,  or null if the line does not represent a feature (e.g. is
     *         a comment)
     */
    public Feature decode(String line) {
        try {
            // clean out header lines and comments
            if (line.startsWith("#") || line.startsWith("@"))
                return null;

            // parse into lines
            parts = line.trim().split("\\s+");

            // check that we got the correct number of tokens in the split
            if (parts.length != 18)
                throw new CodecLineParsingException("Invalid GeliTextFeature row found -- incorrect element count.  Expected 18, got " + parts.length + " line = " + line);

            // UPPER case and sort
            char[] x = parts[5].toUpperCase().toCharArray();
            Arrays.sort(x);
            String bestGenotype = new String(x);

            double genotypeLikelihoods[] = new double[10];
            for (int pieceIndex = 8, offset = 0; pieceIndex < 18; pieceIndex++, offset++) {
                genotypeLikelihoods[offset] = Double.valueOf(parts[pieceIndex]);
            }
            return new GeliTextFeature(parts[0],
                                       Long.valueOf(parts[1]),
                                       Character.toUpperCase(parts[2].charAt(0)),
                                       Integer.valueOf(parts[3]),
                                       Integer.valueOf(parts[4]),
                                       DiploidGenotype.toDiploidGenotype(bestGenotype),
                                       Double.valueOf(parts[6]),
                                       Double.valueOf(parts[7]),
                                       genotypeLikelihoods);
        } catch (CodecLineParsingException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException("Unable to parse line " + line,e);
        } catch (NumberFormatException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw new RuntimeException("Unable to parse line " + line,e);
        }
    }


    /**
     * @return GeliTextFeature
     */
    public Class getFeatureType() {
        return GeliTextFeature.class;
    }

    public Object readHeader(LineReader reader)  {
        return null;  // we don't have a meaningful header
    }
}
