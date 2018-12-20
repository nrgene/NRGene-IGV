package org.broad.tribble.dbsnp;

import org.broad.tribble.AbstractFeatureCodec;
import org.broad.tribble.Feature;
import org.broad.tribble.annotation.Strand;
import org.broad.tribble.readers.LineReader;


/**
 * @author aaron
 *
 * Example format:
 * 585 chr1 433 433 rs56289060  0  +  - - -/C  genomic  insertion unknown 0  0  unknown  between  1
 * 585 chr1 491 492 rs55998931  0  +  C C C/T  genomic  single   unknown 0 0 unknown exact 1
 */
public class OldDbSNPCodec extends AbstractFeatureCodec {

    // the number of tokens we expect to parse from a dbSNP line
    static final int expectedTokenCount = 18;

    // Declare a static array once, to be reused.

    public Feature decodeLoc(String line) {
        return decode(line);
    }
    
    /**
     * Decode a line as a db SNP Feature.
     *
     * @param line the line to decode
     *
     * @return Return the Feature encoded by the line, or null if the line does not represent a feature (e.g. is
     *         a comment)
     *
     * The ordering of db SNP fields from the UCSC track browser:
     * 1 bin
     * 2 chromosome
     * 3 chromosome Start
     * 4 chromosome End
     * 5 name
     * 6 score
     * 7 strand
     * 8 reference base NCBI
     * 9 reference base UCSC
     * 10 observed base
     * 11 mol. Type
     * 12 class
     * 13 valid
     * 14 avHet
     * 15 avHetSE
     * 16 functions
     * 17 locType
     * 18 weight
     */
    public Feature decode(String line) {

        // we may be asked to process a header line; ignore it
        if (line.startsWith("#")) return null;

        // split the line
        String[] tokens = line.split("\\t+");

        // check to see if we've parsed the string into the right number of tokens (expectedTokenCount)
        if (tokens.length != expectedTokenCount)
            return null;
        //    throw new CodecLineParsingException("the dbSNP line didn't have the expected number of tokens " +
        //                                        "(expected = " + expectedTokenCount + ", saw = " + tokens.length + " on " +
        //
            //                                         "line = " + line + ")");
        // create a new feature from the line
        int start = Integer.valueOf(tokens[2])+1;
        int stop = Integer.valueOf(tokens[3]);
        stop = (stop < start) ? start : stop; // Indels can be of length zero in dbSNP, we make them length one
        OldDbSNPFeature feature = new OldDbSNPFeature(tokens[1],
						      start,
						      stop);

        feature.setRsID(tokens[4]);
        feature.setScore(Integer.valueOf(tokens[5]));
        feature.setStrand(tokens[6].equals("+") ? Strand.POSITIVE : Strand.NEGATIVE);
        feature.setNCBIRefBase(tokens[7]);
        feature.setUCSCRefBase(tokens[8]);
        // split the observed bases
        feature.setObserved(tokens[9].split("/"));        
        feature.setMolType(tokens[10]);
        feature.setVariantType(tokens[11]);
        feature.setValidationStatus(tokens[12]);
        feature.setAvHet(Double.valueOf(tokens[13]));
        feature.setAvHetSE(Double.valueOf(tokens[14]));
        feature.setFunction(tokens[15]);
        feature.setLocationType(tokens[16]);
        feature.setWeight(Integer.valueOf(tokens[17]));

        // return the setup feature                                                                                
        return feature;
    }


    /**
     * @return the type of feature we produce: DbSNPFeature
     */
    public Class getFeatureType() {
        return OldDbSNPFeature.class;
    }

    public Object readHeader(LineReader reader)  {
        return null;  // we don't have a meaningful header
    }
}
