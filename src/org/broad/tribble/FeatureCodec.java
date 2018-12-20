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

package org.broad.tribble;

import org.broad.igv.feature.genome.Genome;
import org.broad.tribble.readers.LineReader;

/**
 * the base interface for classes that read in features.
 * @param <T> The feature type this codec reads
 */
public interface FeatureCodec<T extends Feature> {
    /**
     * Decode a line to obtain just its FeatureLoc for indexing -- contig, start, and stop.
     *
     * @param line the input line to decode
     * @return  Return the FeatureLoc encoded by the line, or null if the line does not represent a feature (e.g. is
     * a comment)
     */
    public Feature decodeLoc(String line);

    /**
     * Decode a line as a Feature.
     *
     * @param line the input line to decode
     * @return  Return the Feature encoded by the line,  or null if the line does not represent a feature (e.g. is
     * a comment)
     */
    public T decode(String line);
    public T decode(String line, Genome genome);

    /**
     * This function returns the object the codec generates.  This is allowed to be Feature in the case where
     * conditionally different types are generated.  Be as specific as you can though.
     *
     * This function is used by reflections based tools, so we can know the underlying type
     *
     * @return the feature type this codec generates.
     */
    public Class<T> getFeatureType();


    /**  Read and return the header, or null if there is no header.
     *
     * @return header object
     */
    public Object readHeader(LineReader reader);

    /**
     * This function returns true iff the File potentialInput can be parsed by this
     * codec.
     *
     * There is an assumption that there's never a situation where two different Codecs
     * return true for the same file.  If this occurs, the recommendation would be to error out.
     *
     * Note this function must never throw an error.  All errors should be trapped
     * and false returned.
     *
     * @param path the file to test for parsability with this codec
     * @return true if potentialInput can be parsed, false otherwise
     */
    public boolean canDecode(final String path);
}
