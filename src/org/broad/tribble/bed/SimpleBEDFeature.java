package org.broad.tribble.bed;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.broad.tribble.annotation.Strand;

/**
 *  Feature from a BED file without exon blocks.
 */
public class SimpleBEDFeature implements BEDFeature {
    protected String chr;
    protected int start = -1;
    protected int end = -1;
    protected Strand strand = Strand.NONE;
    private String name = "";
    private float score = Float.NaN;
    private String type = "";
    private Color color;
    private String description;//protected float confidence;
    //private String identifier;
    private String link;

    public SimpleBEDFeature(int start, int end, String chr) {
        this.start = start;
        this.end = end;
        this.chr = chr;
    }

    public String getChr() {
        return chr;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    public Strand getStrand() {
        return strand;
    }

    public void setStrand(Strand strand) {
        this.strand = strand;
    }

    public void setChr(String chr) {
        this.chr = chr;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getScore() {
        return score;
    }

    public void setScore(float score) {
        this.score = score;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    final static List<FullBEDFeature.Exon> emptyExonList = new ArrayList();

    public java.util.List<FullBEDFeature.Exon> getExons() {
        return emptyExonList;
    }
}
