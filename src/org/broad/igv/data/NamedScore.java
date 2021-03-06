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

package org.broad.igv.data;

import org.broad.igv.track.WindowFunction;

/**
 * @author jrobinso
 * @date May 19, 2011
 */
public class NamedScore extends BasicScore {

    private String probe;

    public NamedScore(int start, int end, float score, String probe, double startFraction, double endFraction) 
    {
    	this(start, end, score, probe);
    	setStartFraction(startFraction);
    	setEndFraction(endFraction);
    }
    
    public NamedScore(int start, int end, float score, String probe) {
        super(start, end, score);
        this.probe = probe;
    }

    public NamedScore(double start, double end, float score, String probe) {
    	this((int)start, (int)end, score, probe, start - (int)start, end - (int)end);
    }

    public String getValueString(double position, WindowFunction windowFunction) {
        StringBuffer buf = new StringBuffer();
        buf.append("Value: " + score);
        if(probe != null && probe.length() > 0) {
            buf.append("&nbsp;(");
            buf.append(probe);
            buf.append(")");
        }
        buf.append("<br>");

        return buf.toString();
    }

}
