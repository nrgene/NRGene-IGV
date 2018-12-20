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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.tribble.VCFWrapperCodec;
import org.broad.igv.nrgene.api.ApiRequest;
import org.broad.igv.util.ResourceLocator;
import org.broad.tribble.Feature;
import org.broad.tribble.FeatureCodec;
import org.broad.tribble.FeatureSource;
import org.broad.tribble.Tribble;
import org.broad.tribble.TribbleException;
import org.broad.tribble.index.Index;
import org.broad.tribble.iterators.CloseableTribbleIterator;
import org.broad.tribble.readers.AsciiLineReader;
import org.broad.tribble.readers.LineReader;
import org.broad.tribble.source.query.AsciiQuerySource;
import org.broad.tribble.source.query.IndexFreeAsciiQuerySource;
import org.broad.tribble.source.query.QuerySource;
import org.broad.tribble.source.tabix.TabixLineReader;
import org.broad.tribble.util.ParsingUtils;
import org.broad.tribble.util.SeekableStreamFactory;
import org.broad.tribble.util.URLHelper;

/**
 * jrobinso
 * <p/>
 * the feature reader class, which uses indices and codecs to read in Tribble file formats.
 */
public class BasicFeatureSource<T extends Feature> implements FeatureSource<T> {
    // the logging destination for this source
    private final static Logger log = Logger.getLogger("BasicFeatureSource");

    // the path to underlying data source
    String path;

    // the query source, codec, and header
    protected final QuerySource querySource;
    protected final FeatureCodec codec;
    protected final Object header;
    
    private Genome	genome;
    
    // A hook for the future, when we might allow clients to specify this.

    /**
     * factory for unknown file type,  could be ascii or could be tabix, or something else.
     *
     * @param featureResource the feature file to create from
     * @param codec           the codec to read features with
     * @throws FileNotFoundException
     */
    public static final BasicFeatureSource getFeatureSource(String featureResource, FeatureCodec codec, boolean requireIndex) throws TribbleException {

    	
        try {
            if (!resourceExists(featureResource))
                throw new TribbleException.FeatureFileDoesntExist(featureResource, featureResource);

            QuerySource querySource;
            // Crude test for now

            // todo -- check for existence of indexFile and ending with .gz to determine Tabix or BGZipped indices
            // If gzipped and Tribble, just use AsciiQuerySource with BlockedCompressedInputStream and everything should work correctly

            // tabix
            String featureResourcePath = null;
            try {
                URI featureResourceUri = new URI(featureResource);
                featureResourcePath = featureResourceUri.getPath();
            } catch (URISyntaxException e) {
                featureResourcePath = featureResource;
            }
            if (featureResourcePath.endsWith(".gz")) {
                querySource = new TabixLineReader(featureResource);
            }
            // text based file
            else {
                String indexFile = Tribble.indexFile(featureResource);
                if (resourceExists(indexFile)) {
                    querySource = new AsciiQuerySource(featureResource, indexFile);
                } else {
                    // See if the index is gzipped
                    indexFile = indexFile + ".gz";
                    if (resourceExists(indexFile)) {
                        querySource = new AsciiQuerySource(featureResource, indexFile);
                    } else if (requireIndex && !ApiRequest.isApiUrl(featureResource) ) {
                        throw new FileNotFoundException("A index is requred, but none could be found for " + featureResource);
                    } else {
                        querySource = new IndexFreeAsciiQuerySource(featureResource);
                    }
                }

            }
            return new BasicFeatureSource(querySource, codec, featureResource);
        } catch (IOException e) {
            throw new TribbleException.MalformedFeatureFile("Unable to create BasicFeatureReader using feature file ", featureResource, e);
        } catch (TribbleException e) {
            e.setSource(featureResource);
            throw e;
        }

    }

    private static boolean resourceExists(String resource) {

        boolean remoteFile = resource.startsWith("http://") || resource.startsWith("https://") || resource.startsWith("ftp://");
        if (remoteFile) {
            if (resource.startsWith("ftp://")) {
                throw new RuntimeException("FTP prototcol not yet supported: " + resource);
            } else {
                try {
                    URL url = new URL(resource);
                    URLHelper helper = SeekableStreamFactory.getURLHelper(url);
                    return helper.exists();
                } catch (Exception e) {
                    log.error("Error checking existence of resource: " + resource, e);
                    return false;
                }
            }
        } else {
            return (new File(resource)).exists();
        }
    }


    public static final BasicFeatureSource getFeatureSource(String featureFile, FeatureCodec codec) throws TribbleException {
        return getFeatureSource(featureFile, codec, true);
    }

    /**
     * Constructor for ascii indexed files
     *
     * @param featureFile   the feature file
     * @param indexInstance the index instance
     * @param codec         the codec
     * @throws FileNotFoundException
     */
    public BasicFeatureSource(String featureFile, Index indexInstance, FeatureCodec codec) throws IOException {
        this.path = featureFile;
        this.codec = codec;
        querySource = new AsciiQuerySource(featureFile, indexInstance);
        header = readHeader();
    }

    /**
     * Constructor for ascii indexed files
     *
     * @param featureFile the feature file
     * @param indexFile   the index instance
     * @param codec       the codec
     * @throws FileNotFoundException
     */
    public BasicFeatureSource(String featureFile, String indexFile, FeatureCodec codec) throws IOException {
        this.path = featureFile;
        querySource = new AsciiQuerySource(featureFile, indexFile);
        this.codec = codec;
        header = readHeader();
    }

    /**
     * a private constructor of a basic feature source
     *
     * @param source the query source to write
     * @param codec  the codec to use
     * @param path   where to find the file or input source
     * @throws IOException if we have trouble finding the file
     */
    private BasicFeatureSource(QuerySource source, FeatureCodec codec, String path) throws IOException {
        this.path = path;
        this.querySource = source;
        this.codec = codec;
        header = readHeader();
    }

    /**
     * read the header from the file from an INDEPENDENT stream
     *
     * @return a Object, representing the file header, if available
     * @throws IOException throws an IOException if we can't open the file
     */
    private Object readHeader() throws IOException {
        Object header;

        if (querySource.markSupported()) {
            // If mark is supported, read the header and then revert back to the beginning of the file.
            querySource.mark();
            header = codec.readHeader(querySource.iterate());
            querySource.reset();
        } else if (querySource instanceof TabixLineReader) {
            TabixLineReader independentReader = null;
            try {
                independentReader = new TabixLineReader(((TabixLineReader) querySource).getSource());
                header = codec.readHeader(independentReader);
            } finally {
                if (independentReader != null) {
                    independentReader.close();
                }
            }
        } else {
            // If mark is unsupported, just open a new copy of the file.
            AsciiLineReader reader = null;
            try {
                reader = ParsingUtils.openAsciiReader(path);
                header = codec.readHeader(reader);
            } catch (Exception e) {
                throw new TribbleException.MalformedFeatureFile("Unable to parse header with error: " + e.getMessage(), path, e);
            }
            finally {
                if (reader != null) {
                    reader.close();
                }
            }
        }
        return header;
    }

    /**
     * get the header
     *
     * @return the header object we've read-in
     */
    public Object getHeader() {
        return header;
    }

    /**
     * close the query source
     *
     * @throws IOException an IOException if we can't close the input query source
     */
    public void close() throws IOException {
        if (querySource != null) {
            querySource.close();
        }
    }

    public CloseableTribbleIterator<T> query(final String chr, final int start, final int end) throws IOException {
        return new IteratorImpl<T>(this, chr, start, end);
    }

    public CloseableTribbleIterator<T> iterator() throws IOException {
        return new IteratorImpl<T>(this);
    }


    public List<String> getSequenceNames() {
        return querySource.getSequenceNames();
    }


    /**
     * the basic feature iterator for indexed files
     */
    public static class IteratorImpl<T extends Feature> implements CloseableTribbleIterator {

        private static Logger log = Logger.getLogger("IteratorImpl");

        String chr;
        int start;
        int end;
        T currentRecord;

        final LineReader reader;

        BasicFeatureSource<T> basicFeatureSource;

        IteratorImpl(BasicFeatureSource<T> basicFeatureSource) throws IOException {
            this.basicFeatureSource = basicFeatureSource;
            reader = basicFeatureSource.querySource.iterate();
            // we have to read the header off in this case (since the reader seeked to position 0)
            readNextRecord();
        }

        IteratorImpl(BasicFeatureSource<T> basicFeatureSource,
                     String sequence,
                     int start,
                     int end) throws IOException {

            this.basicFeatureSource = basicFeatureSource;
            this.chr = sequence;
            this.start = start;
            this.end = end;
            reader = basicFeatureSource.querySource.query(chr, start, end);

            advanceToFirstRecord();
        }

        /**
         * read the next record, storing it in the currentRecord variable
         *
         * @throws IOException
         */
        protected void readNextRecord() throws IOException {
            currentRecord = null;
            String nextLine;
            while (currentRecord == null && reader != null && (nextLine = reader.readLine()) != null) {
                Feature f = null;
                try {
            		f = basicFeatureSource.codec.decode(nextLine, basicFeatureSource.genome);
                } catch (TribbleException e) {
                    e.setSource(basicFeatureSource.path);
                    throw e;
                }
                if (f == null)
                    continue;
                else if ((end > 0 && f.getStart() > end) || (chr != null && !chr.equals(f.getChr())))
                    break;
                else if (f.getEnd() >= start)
                    currentRecord = (T) f;
            }
        }

        /**
         * advance to the first record after using the query method (not for use with the plain iterator() method)
         *
         * @throws IOException thrown if we're having trouble reading the file
         */
        protected void advanceToFirstRecord() throws IOException {
            String nextLine;
            currentRecord = null;
            while (currentRecord == null && reader != null && (nextLine = reader.readLine()) != null) {
                Feature f = basicFeatureSource.codec.decodeLoc(nextLine);
                if (f == null)
                    continue;  // we got a null record, try again
                if (!f.getChr().equals(chr)) {
                    currentRecord = null;
                    break;
                } else if (f.getEnd() >= start) {
                    currentRecord = (T) basicFeatureSource.codec.decode(nextLine, basicFeatureSource.genome);
                    break;
                }
            }
        }

        public boolean hasNext() {

            // chr == null => iterator, not query.  Fix this
            if (chr == null) {
                return currentRecord != null;
            }

            return !(currentRecord == null ||
                    !chr.equals(currentRecord.getChr())) && (currentRecord.getStart() <= end);
        }

        public T next() {
            T ret = currentRecord;
            try {
                readNextRecord();
            } catch (IOException e) {
                throw new RuntimeException("Unable to read the next record, the last record was at " + ret.getChr() + ":" + ret.getStart() + "-" + ret.getEnd(), e);
            }
            return ret;

        }

        public void remove() {
            throw new UnsupportedOperationException("Remove is not supported in Iterators");
        }


        public void close() {
            // we don't have anything open, so don't sweat it
        }

        public Iterator<Feature> iterator() {
            return this;
        }

    }
    
    public void reload()
    {
    	
    }
    
    public String getSourceName()
    {
    	return (new File(path).getName());
    }

	public Genome getGenome() {
		return genome;
	}

	public void setGenome(Genome genome) {
		this.genome = genome;
	}
}
