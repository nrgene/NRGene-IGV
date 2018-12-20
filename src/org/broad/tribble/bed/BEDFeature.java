package org.broad.tribble.bed;

import java.awt.Color;

import org.broad.tribble.Feature;
import org.broad.tribble.annotation.Strand;

/**
 * Created by IntelliJ IDEA.
 * User: jrobinso
 * Date: Dec 24, 2009
 * Time: 3:46:08 PM
 * To change this template use File | Settings | File Templates.
 */
public interface BEDFeature extends Feature {
    Strand getStrand();

    String getType();

    Color getColor();

    String getDescription();

    java.util.List<FullBEDFeature.Exon> getExons();

    String getName();

    float getScore();

    String getLink();
}
