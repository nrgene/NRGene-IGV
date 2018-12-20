/*
 * Copyright (c) 2007-2010 by The Broad Institute, Inc. and the Massachusetts Institute of Technology.
 * All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL), Version 2.1 which
 * is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR WARRANTIES OF
 * ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING, WITHOUT LIMITATION, WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT
 * OR OTHER DEFECTS, WHETHER OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR
 * RESPECTIVE TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES OF
 * ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES, ECONOMIC
 * DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER THE BROAD OR MIT SHALL
 * BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT SHALL KNOW OF THE POSSIBILITY OF THE
 * FOREGOING.
 */

package org.broad.tribble.source;

import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.broad.tribble.Feature;
import org.broad.tribble.FeatureCodec;
import org.broad.tribble.index.Index;
import org.broad.tribble.iterators.CloseableTribbleIterator;

/**
 * jrobinso
 * <p/>
 * the feature reader class, which uses indices and codecs to read in Tribble file formats.
 */
public class PerformanceLoggingFeatureSource<T extends Feature> extends BasicFeatureSource<T> {
    private final static Logger log = Logger.getLogger("PerformanceLoggingFeatureSource");
    int nSkipsFromLastQuery, nTotalSkips, nEmptyLines, nRecordsRead, nQueries;

    /**
     * Constructor for ascii indexed files
     *
     * @param featureFile   the feature file
     * @param indexInstance the index instance
     * @param codec         the codec
     * @throws java.io.FileNotFoundException
     */
    public PerformanceLoggingFeatureSource(String featureFile, Index indexInstance, FeatureCodec codec) throws IOException {
        super(featureFile, indexInstance, codec);
    }

    /**
     * Constructor for ascii indexed files
     *
     * @param featureFile the feature file
     * @param indexFile   the index instance
     * @param codec       the codec
     * @throws java.io.FileNotFoundException
     */
    public PerformanceLoggingFeatureSource(String featureFile, String indexFile, FeatureCodec codec) throws IOException {
        super(featureFile, indexFile, codec);
    }

    public CloseableTribbleIterator<T> query(final String chr, final int start, final int end) throws IOException {
        nSkipsFromLastQuery = 0;
        return new TrackingIteratorImpl<T>(this, chr, start, end);
    }

    public int getnSkipsFromLastQuery() {
        return nSkipsFromLastQuery;
    }

    public int getnTotalSkips() {
        return nTotalSkips;
    }

    public int getnEmptyLines() {
        return nEmptyLines;
    }

    public int getnRecordsRead() {
        return nRecordsRead;
    }

    public String getPerformanceLog() {
        int nReads = nRecordsRead + nTotalSkips + nEmptyLines;
        return String.format("nQueries %d nRequestedRecords %d nTotalSkips %d efficiency %.2f",
                nQueries, nRecordsRead, nEmptyLines + nTotalSkips, nRecordsRead / (0.01 * Math.max(nReads, 1)));
    }

    /**
     * the basic feature iterator for indexed files
     */
    public class TrackingIteratorImpl<T extends Feature> extends IteratorImpl<T> {
        //private static Logger log = Logger.getLogger("IteratorImpl");
        PerformanceLoggingFeatureSource<T> performanceTrackingSource;

        TrackingIteratorImpl(PerformanceLoggingFeatureSource<T> featureSource,
                     String sequence,
                     int start,
                     int end) throws IOException {
            super(featureSource, sequence, start, end);
            nQueries++;
            performanceTrackingSource = featureSource;
        }

        public T next() {
            nRecordsRead++;
            return super.next();
        }

        public Iterator<Feature> iterator() {
            return this;
        }

        /**
         * advance to the first record after using the query method (not for use with the plain iterator() method)
         *
         * @throws java.io.IOException thrown if we're having trouble reading the file
         */
        protected void advanceToFirstRecord() throws IOException {
//            log.warn("advanceToFirstRecord in TrackingIteratorImpl");
//            super.advanceToFirstRecord();
            String nextLine;
            currentRecord = null;
            while (currentRecord == null && reader != null && (nextLine = reader.readLine()) != null) {
                Feature f = basicFeatureSource.codec.decodeLoc(nextLine);

                // null records are comments etc
                if (f == null) {
                    nTotalSkips++;
                    nEmptyLines++;
                    continue;  // we got a null record, try again
                }

                if (!f.getChr().equals(chr)) {
                    // we've somehow read the wrong chr?
                    nTotalSkips++;
                    nSkipsFromLastQuery++;
                    currentRecord = null;
                    break;
                } else if (f.getEnd() >= start) {
                    nRecordsRead++;
                    currentRecord = (T) basicFeatureSource.codec.decode(nextLine);
                    break;
                } else {
                    // this is really a skip
                    nTotalSkips++;
                    nSkipsFromLastQuery++;
                }
            }
        }
    }
}
