/*
 * Copyright (c) 2007-2011 by The Broad Institute of MIT and Harvard.  All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */

package org.broad.igv.feature.genome;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.log4j.Logger;
import org.broad.igv.util.FileUtils;
import org.broad.igv.util.stream.IGVSeekableStreamFactory;
import org.broad.tribble.util.SeekableStream;

/**
 * Represents a sequence database composed of plain text files with no white space, one per chromosome, in a directory.
 * This is the original IGV "sequence" format, replaced in favor if indexed fasta files.
 *
 * @author jrobinso
 * @Date 8/8/11
 */

public class IGVSequence implements Sequence {

    private static Logger log = Logger.getLogger(IGVSequence.class);
    private final ZipFile genomeFile;

    private String dirPath;
    private Map<String, String> chrFileNameCache = new HashMap();

    public IGVSequence(String dirPath, ZipFile genomeFile) {
        this.genomeFile = genomeFile;
        if (!dirPath.endsWith("/")) {
            dirPath = dirPath + "/";
        }
        this.dirPath = dirPath;
    }

    public byte[] readSequence(String chr, int start, int end) {
        String fileName = chr + ".txt";
        fileName = getChrFileName(fileName);
        String seqFile = dirPath + fileName;

        InputStream is = null;
        try {
            byte[] bytes = new byte[end - start];
            try {
                is = IGVSeekableStreamFactory.getStreamFor(seqFile);
                ((SeekableStream)is).seek(start);
            } catch (FileNotFoundException ex) {
                if (genomeFile == null) {
                    throw ex;
                }
                log.info("File not found when reading genome sequence from: " + seqFile + ". Will now look inside zip");
                String parentDirPath = new File(genomeFile.getName()).getParent();
                String relativeDirPath = dirPath.replaceFirst(parentDirPath, "");
                if (relativeDirPath.startsWith("/") || relativeDirPath.startsWith("\\")) {
                    relativeDirPath = relativeDirPath.substring(1);
                }
                String relativeFilePath = relativeDirPath + fileName;
                ZipEntry entry = genomeFile.getEntry(relativeFilePath);
                if (entry == null) {
                    throw new FileNotFoundException("File " + seqFile + " not found outside zip nor inside zip as name " + relativeFilePath);
                }
                is = new BufferedInputStream(genomeFile.getInputStream(entry));
                is.skip(start);
            }
            is.read(bytes);
            return bytes;
        } catch (Exception ex) {
            log.error("Error reading genome sequence from: " + seqFile, ex);
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ex) {
                    log.error("Error closing sequence file.", ex);
                }
            }
        }
    }


    private String getChrFileName(String fn) {
        String chrFN = chrFileNameCache.get(fn);
        if (chrFN == null) {
            chrFN = FileUtils.legalFileName(fn);
            chrFileNameCache.put(fn, chrFN);
        }
        return chrFN;
    }
}
