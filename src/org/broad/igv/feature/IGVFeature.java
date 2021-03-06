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


package org.broad.igv.feature;

import java.awt.Color;
import java.util.List;
import java.util.Map;


public interface IGVFeature extends LocusScore, NamedFeature {

    public String getType();

    public String getChr();

    public void setChr(String chr);

    public int getStart();

    public int getEnd();

    public double getStartFraction();

    public double getEndFraction();

    public String getIdentifier();

    public String getName();

    public String getDescription();

    public boolean hasScore();

    public Strand getStrand();

    public void setName(String name);

    public Color getColor();

    public boolean contains(IGVFeature feature);

    public List<Exon> getExons();

    public int getLength();

    public Map<String, String> getAttributes();

    boolean contains(double location);

    public String getURL();

    public Exon getExonAt(double location);
    
    public void setPath(String path);
}
