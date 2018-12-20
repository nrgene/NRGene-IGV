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

import org.broad.igv.feature.genome.Genome;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.WaitCursorManager;

/**
 * @author eflakes
 */
public class RegionOfInterest{

	private String preferedTab;
	private String genome;
    private String chr;
    private String description;
    private int start;    // In Chromosome coordinates
    private int end;      // In Chromosome coordinates
    private static Color backgroundColor = Color.RED;
    private static Color foregroundColor = Color.BLACK;
    boolean selected = false;

    private WaitCursorManager.CursorToken token;

    /**
     * A bounded region on a chromosome.
     *
     * @param chromosomeName
     * @param start          The region starting position on the chromosome.
     * @param end            The region starting position on the chromosome.
     * @param description
     */
    public RegionOfInterest(String chromosomeName, int start, int end, String description) {
    	this(IGV.getInstance().getGenomeManager().currentGenome != null
    			? IGV.getInstance().getGenomeManager().currentGenome.getId() : null,
    			chromosomeName, start, end, description);
    }
    public RegionOfInterest(String genome, String chromosomeName, int start, int end, String description) {

    	this.genome = genome;
        this.chr = chromosomeName;
        this.description = description;
        this.start = start;
        this.end = end;
    }

    public String getTooltip() {
        return description == null ? chr + ":" + start + "-" + end : description;
    }

    public String getChr() {
        return chr;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }


    public void setEnd(int end) {
        this.end = end;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    /**
     * locations displayed to the user are 1-based.  start and end are 0-based.
     * @return
     */
    public int getDisplayEnd() {
        return getEnd() + 1;
    }

    public int getStart() {
        return start;
    }

    public int getCenter() {
        return (start + end) / 2;
    }

    public int getLength() {
        return end - start;
    }

    /**
     * locations displayed to the user are 1-based.  start and end are 0-based.
     * @return
     */
    public int getDisplayStart() {
        return getStart() + 1;
    }

    public static Color getBackgroundColor() {
        return backgroundColor;
    }

    public static Color getForegroundColor() {
        return foregroundColor;
    }


    public String getLocusString() {
        return getChr() + ":" + getStart() + "-" + getEnd();
    }

	public String getGenome() {
		return genome;
	}

	public void setGenome(String genome) {
		this.genome = genome;
	}
	public String getPreferedTab() {
		return preferedTab;
	}
	public void setPreferedTab(String preferedTab) {
		this.preferedTab = preferedTab;
	}
	public boolean isOnCurrentGenome() 
	{
		Genome			currentGenme = IGV.getInstance().getGenomeManager().currentGenome;
		
		// no genome, pass it
		if ( currentGenme == null )
			return true;
		
		// no genome on region, pass it
		if ( genome == null )
			return true;
		
		// otherwise, must be equal
		return genome.equals(currentGenme.getId());
	}
}
