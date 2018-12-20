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

package org.broad.igv.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.broad.igv.track.Track;
import org.broad.igv.util.Filter;
import org.broad.igv.util.FilterElement;
import org.broad.igv.util.FilterElement.BooleanOperator;

/**
 * @author eflakes
 */
public class TrackFilter extends Filter {

    List<Track> currentTrackList;

    @SuppressWarnings("unchecked")
	public TrackFilter()
    {
    	super(null, Collections.EMPTY_LIST);
    }
    
    @SuppressWarnings("unchecked")
	public TrackFilter(String stringRepresentation)
    {
    	super(null, Collections.EMPTY_LIST);

    	String[]					toks = stringRepresentation.split(",");
    	String						name = toks[0];
    	List<TrackFilterElement>	elements = new LinkedList<TrackFilterElement>();
    	for ( int n = 1 ; n < toks.length ; n++ )
    		elements.add(new TrackFilterElement(this, toks[n]));
    	
    	init(name, elements);
    }
    
    public TrackFilter(String name, List<TrackFilterElement> elements) {
        super(name, elements);
    }

    public void clearTracks() {
        currentTrackList = null;
    }

    public void addTracks(List<Track> tracks) {

        if (tracks == null || tracks.isEmpty())
            return;

        if (currentTrackList == null) {
            currentTrackList = new ArrayList<Track>(tracks);
        }
    }

    /**
     * Evaluate the TrackFilterElement set.
     *
     * @return
     */
    @Override
    public void evaluate() {

        if (currentTrackList == null || currentTrackList.isEmpty())
            return;

        boolean filterEnabled = isEnabled();

        for (Track track : currentTrackList) {

            // Must start as null which means no previous results
            Boolean result = null;

            // If filter is not enabled just show all tracks
            if (!filterEnabled) {
                track.setVisible(!filterEnabled);
                continue;
            }

            // Evaluate tracks
            Iterator iterator = getFilterElements();
            while (iterator.hasNext()) {

                FilterElement element = (FilterElement) iterator.next();
                result = element.evaluate(track, result);
            }
            track.setVisible(result);
        }
    }

    public String toString()
    {
    	List<String>	list = new LinkedList<String>();
    	
    	list.add(getName());
    	Iterator 		element = getFilterElements();
    	while ( element.hasNext() )
    		list.add(element.next().toString());
    	
    	return StringUtils.join(list, ",");
    }

	public boolean getMatchAll() {
		
		boolean		matchAll = true;		// default
		
    	Iterator 		element = getFilterElements();
    	if ( element.hasNext() )
    	{
    		TrackFilterElement	filterElement = (TrackFilterElement)element.next();
    		
    		if ( filterElement.getBooleanOperator() == BooleanOperator.OR )
    			matchAll = false;
    	}
		
		
		return matchAll;
	}

}
