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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.broad.igv.data;

//~--- non-JDK imports --------------------------------------------------------

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.broad.igv.feature.genome.Genome;
import org.broad.igv.track.TrackProperties;
import org.broad.igv.track.TrackType;
import org.broad.igv.util.ArrayHeapIntSorter;
import org.broad.igv.util.IntComparator;
import org.broad.igv.util.collections.DoubleArrayList;
import org.broad.igv.util.collections.FloatArrayList;
import org.broad.igv.util.collections.IntArrayList;

/**
 * @author jrobinso
 */
public class WiggleDataset implements Dataset {

    Genome genome;
    private String name;
    private TrackProperties trackProperties;
    Map<String, IntArrayList> startLocationsMap = new HashMap();
    Map<String, IntArrayList> endLocationsMap = new HashMap();
    Map<String, FloatArrayList> dataMap = new HashMap();
    Map<String, DoubleArrayList> startFractionsMap = new HashMap();
    Map<String, DoubleArrayList> endFractionsMap = new HashMap();
    
    float dataMin = 0;
    float dataMax = 0;
    private Map<String, Integer> longestFeatureMap;
    private TrackType type = TrackType.OTHER;


    public WiggleDataset(Genome genome, String name) {
        this.genome = genome;
        this.name = name;
        this.trackProperties = new TrackProperties();

    }


    // TODO -- keep track of sortedness as data is loaded and skip this sort if unneccessary.

    public void sort(Set<String> unsortedChromosomes) {
        for (String c : unsortedChromosomes) {
            String chr = genome.getChromosomeAlias(c);


            final IntArrayList starts = startLocationsMap.get(chr);
            int sz = starts.size();

            int[] indeces = new int[sz];
            for (int i = 0; i < indeces.length; i++) {
                indeces[i] = i;
            }

            (new ArrayHeapIntSorter()).sort(indeces, new IntComparator() {

                public int compare(int arg0, int arg1) {
                    return starts.get(arg0) - starts.get(arg1);
                }
            });


            int[] sortedStarts = reorder(indeces, startLocationsMap.get(chr));
            int[] sortedEnds = reorder(indeces, endLocationsMap.get(chr));
            float[] sortedData = reorder(indeces, dataMap.get(chr));

            startLocationsMap.put(chr, new IntArrayList(sortedStarts));
            endLocationsMap.put(chr, new IntArrayList(sortedEnds));
            dataMap.put(chr, new FloatArrayList(sortedData));
            
            if ( startFractionsMap.size() > 0 )
            	startFractionsMap.put(chr, new DoubleArrayList(reorder(indeces, startFractionsMap.get(chr))));
            if ( endFractionsMap.size() > 0 )
            	endFractionsMap.put(chr, new DoubleArrayList(reorder(indeces, endFractionsMap.get(chr))));
        }

    }

    private double[] reorder(int[] indeces, DoubleArrayList values) {
        int size = values.size();
        if (indeces.length != size) {
            throw new IllegalArgumentException(
                    "Index array length not equal to size");
        }
        double[] reorderedValues = new double[size];
        for (int i = 0; i < size; i++) {
            reorderedValues[i] = values.get(indeces[i]);
        }
        return reorderedValues;
    }

    private float[] reorder(int[] indeces, FloatArrayList values) {
        int size = values.size();
        if (indeces.length != size) {
            throw new IllegalArgumentException(
                    "Index array length not equal to size");
        }
        float[] reorderedValues = new float[size];
        for (int i = 0; i < size; i++) {
            reorderedValues[i] = values.get(indeces[i]);
        }
        return reorderedValues;
    }

    private int[] reorder(int[] indeces, IntArrayList values) {
        int size = values.size();
        if (indeces.length != size) {
            throw new IllegalArgumentException(
                    "Index array length not equal to size");
        }
        int[] reorderedValues = new int[size];
        for (int i = 0; i < size; i++) {
            reorderedValues[i] = values.get(indeces[i]);
        }
        return reorderedValues;
    }


    public void addDataChunk(String chr, IntArrayList starts, IntArrayList ends, FloatArrayList data, DoubleArrayList startsFraction, DoubleArrayList endsFraction) {
        IntArrayList startLocations = this.startLocationsMap.get(chr);
        if (startLocations == null) {
            this.startLocationsMap.put(chr, starts);
        } else {
            //starts.trimToSize();
            startLocations.addAll(starts);
        }

        if (ends != null) {
            IntArrayList endLocations = this.endLocationsMap.get(chr);
            if (endLocations == null) {
                this.endLocationsMap.put(chr, ends);
            } else {
                //ends.trimToSize();
                endLocations.addAll(ends);
            }
        }

        FloatArrayList dataArray = this.dataMap.get(chr);
        if (dataArray == null) {
            this.dataMap.put(chr, data);
        } else {

            dataArray.addAll(data);
        }
        float[] d = data.toArray();
        for (int i = 0; i < d.length; i++) {
            dataMax = Math.max(dataMax, d[i]);
            dataMin = Math.min(dataMin, d[i]);
        }
        
        if ( startsFraction != null )
        {
            DoubleArrayList startFractions = this.startFractionsMap.get(chr);
            if ( startFractions == null ) {
                this.startFractionsMap.put(chr, startsFraction);
            } else {
            	startFractions.addAll(startsFraction);
            }
        }

        if ( endsFraction != null )
        {
            DoubleArrayList endFractions = this.endFractionsMap.get(chr);
            if ( endFractions == null ) {
                this.endFractionsMap.put(chr, endsFraction);
            } else {
            	endFractions.addAll(endsFraction);
            }
        }
    }


    public float getDataMin() {
        return dataMin;
    }


    public float getDataMax() {
        return dataMax;
    }


    public String getName() {
        return name;
    }


    public TrackType getType() {
        return type;
    }

    public String[] getChromosomes() {
        return startLocationsMap.keySet().toArray(new String[]{});
    }


    public String[] getTrackNames() {
        return new String[]{getName()};
    }


    public int[] getStartLocations(String chr) {
        IntArrayList startLocations = this.startLocationsMap.get(chr);
        if (startLocations == null) {
            return null;
        } else {
            return startLocations.toArray();
        }
    }

    public int[] getEndLocations(String chr) {
        IntArrayList endLocations = this.endLocationsMap.get(chr);
        if (endLocations == null) {
            return null;
        } else {
            return endLocations.toArray();
        }
    }

    public float[] getData(String heading, String chr) {
        FloatArrayList data = this.dataMap.get(chr);
        if (data == null) {
            return null;
        } else {
            return data.toArray();
        }
    }

    public double[] getStartFractions(String chr) {
        DoubleArrayList startFractions = this.startFractionsMap.get(chr);
        if (startFractions == null) {
            return null;
        } else {
            return startFractions.toArray();
        }
    }

    public double[] getEndFractions(String chr) {
        DoubleArrayList endFractions = this.endFractionsMap.get(chr);
        if (endFractions == null) {
            return null;
        } else {
            return endFractions.toArray();
        }
    }


    public String[] getFeatureNames(String chr) {
        return null;
    }

    public boolean isLogNormalized() {
        return false;
    }

    public void setName(String name) {
        this.name = name;
    }

    public TrackProperties getTrackProperties() {
        return trackProperties;
    }

    public Integer getLongestFeature(String chr) {
        return longestFeatureMap == null ? 1000 :
                longestFeatureMap.containsKey(chr) ? longestFeatureMap.get(chr) : 1;
    }

    public void setLongestFeatureMap(Map<String, Integer> longestFeatureMap) {
        this.longestFeatureMap = longestFeatureMap;
    }

    public void setType(TrackType type) {
        this.type = type;
    }
}
