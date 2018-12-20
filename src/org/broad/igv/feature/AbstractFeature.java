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

//~--- non-JDK imports --------------------------------------------------------

import java.awt.Color;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.broad.igv.feature.genome.Genome;

/**
 * @author jrobinso
 */
abstract public class AbstractFeature extends LocusScoreBase implements IGVFeature, org.broad.tribble.Feature {

    private static Logger log = Logger.getLogger(AbstractFeature.class);
    protected Strand strand = Strand.NONE;
    protected String chromosome;
    protected int start = -1;
    protected int end = -1;
    protected String type = "";
    protected Color color;
    protected String description;
    protected Map<String, String> attributes;
    protected String name = "";
    
    protected double startFraction;
    protected double endFraction;
    protected double startFraction2;
    protected double endFraction2;
    
    protected int	lastDrawNameStart;
    protected int	lastDrawNameWidth;
    
    protected String path;


    /**
     * Constructs ...
     */
    public AbstractFeature() {
    }

    /**
     * Constructs ...
     *
     * @param chr
     * @param start
     * @param end
     * @param strand
     */
    public AbstractFeature(String chr, int start, int end, Strand strand) {
        this.chromosome = chr;
        this.start = start;
        this.end = end;
        this.strand = strand;
    }

    public AbstractFeature(String chr, int start, int end, Strand strand, double startFraction, double endFraction) {
    	this(chr, start, end, strand);
    	setStartFraction(startFraction);
    	setEndFraction(endFraction);
    }

    public String getIdentifier() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public boolean hasScore() {
        return false;
    }

    public List<Exon> getExons() {
        return null;
    }

    public Exon getExonAt(double location) {
        return null;
    }

    public float getScore() {
        return Float.NaN;
    }

    public String getChr() {
        return chromosome;
    }

    /**
     * By default features are 1 bp wide
     *
     * @return
     */
    public int getEnd() {
        return end;
    }

    public int getLength() {
        return end - start;
    }

    public int getStart() {
        return start;
    }

    /**
     * Return true if the feature is completely contained within the bounds of this
     * featre.
     * <p/>
     * //TODO -- should strand be included in this test?
     *
     * @param feature
     * @return
     */
    public boolean contains(IGVFeature feature) {
        if (feature == null) {
            return false;
        }
        if (!this.getChr().equals(feature.getChr()) ||
                this.getStrand() != feature.getStrand()) {
            return false;
        }
        if ((feature.getStart() >= this.getStart()) && (feature.getEnd() <= this.getEnd())) {
            return true;
        } else {
            return false;
        }
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public Strand getStrand() {
        return strand;
    }

    public void setStrand(Strand strand) {
        this.strand = strand;
    }

    public boolean hasStrand() {
        return ((strand != null) && (strand != Strand.NONE));
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public void setColor(String[] rgb, int nTokens) {
        try {
            if (nTokens < 3) {
                if (rgb[0].equals(".")) {
                    return;
                }
                color = new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[0]),
                        Integer.parseInt(rgb[0]));
            } else {
                color = new Color(Integer.parseInt(rgb[0]), Integer.parseInt(rgb[1]),
                        Integer.parseInt(rgb[2]));
            }
        } catch (NumberFormatException numberFormatException) {

        }
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return (description == null) ? getName() : description;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public boolean contains(double location) {
        return location >= getStart() && location < getEnd();
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @param chromosome the chromosome to set
     */
    public void setChr(String chromosome) {
        this.chromosome = chromosome;
    }

    public String getLocusString() {
    	return getLocusString(null);
    }

    public String getLocusString(Genome genome) {
    	if ( genome == null || !genome.isGeneticMap() )
    		return getChr() + ":" + getStart() + "-" + getEnd();
    	else
    	{
    		return getChr() + ":" 
    						+ genome.getLocationFormatter().format(getStart() + getStartFraction())  
    						+ "-"
    						+ genome.getLocationFormatter().format(getEnd() + getEndFraction());  
    	}
    }

	public double getStartFraction() {
		return startFraction;
	}

	public void setStartFraction(double startFraction) {
		this.startFraction = startFraction;
		this.startFraction2 = startFraction;
	}

	public double getEndFraction() {
		return endFraction;
	}

	public void setEndFraction(double endFraction) {
		this.endFraction = endFraction;
		this.endFraction2 = endFraction;
	}

	public double getStartFraction2() {
		return startFraction2;
	}

	public void setStartFraction2(double startFraction2) {
		this.startFraction2 = startFraction2;
	}

	public double getEndFraction2() {
		return endFraction2;
	}

	public void setEndFraction2(double endFraction2) {
		this.endFraction2 = endFraction2;
	}

	public int getLastDrawNameStart() {
		return lastDrawNameStart;
	}

	public void setLastDrawNameStart(int lastDrawNameStart) {
		this.lastDrawNameStart = lastDrawNameStart;
	}

	public int getLastDrawNameWidth() {
		return lastDrawNameWidth;
	}

	public void setLastDrawNameWidth(int lastDrawNameWidth) {
		this.lastDrawNameWidth = lastDrawNameWidth;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
