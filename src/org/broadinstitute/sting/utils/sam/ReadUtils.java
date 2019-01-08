package org.broadinstitute.sting.utils.sam;

import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;

import java.util.Arrays;

public class ReadUtils {
    public static String prettyPrintSequenceRecords( final SAMSequenceDictionary sequenceDictionary ) {
        final String[] sequenceRecordNames = new String[sequenceDictionary.size()];
        int sequenceRecordIndex = 0;
        for (final SAMSequenceRecord sequenceRecord : sequenceDictionary.getSequences()) {
            sequenceRecordNames[sequenceRecordIndex++] = sequenceRecord.getSequenceName();
        }
        return Arrays.deepToString(sequenceRecordNames);
    }}
