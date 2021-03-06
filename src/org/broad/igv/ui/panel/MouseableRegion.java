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
package org.broad.igv.ui.panel;

import java.awt.Rectangle;
import java.awt.Shape;
import java.util.HashSet;
import java.util.Set;

import org.broad.igv.nrgene.api.ApiRequest;
import org.broad.igv.track.Track;
import org.broad.igv.ui.IGV;
import org.broad.igv.variant.VariantTrack;
import org.jfree.util.Log;

/**
 * Regions that represent tracks
 */
public class MouseableRegion {

    private Shape region;
    private String text;
    private TrackCltn trackCltn;


    public MouseableRegion(Shape region, Track track) {
      this.region = region;

      StringBuilder sb = new StringBuilder();
      sb.append("<html>");

      int maxLength = 150;
      String shortTrackName = track.getName().length() >  maxLength ? (track.getName().substring(0, maxLength) + "...") : track.getName();

      if (track.getResourceLocator() != null) {

        if (track.getResourceLocator().getApiRequest() != null) {
          ApiRequest apiRequest = track.getResourceLocator().getApiRequest();
          sb.append("<b>").append(apiRequest.getState()).append(": </b>");
        }

        sb.append(shortTrackName);

        if (track.getResourceLocator().getHttpErrorCode() > 0 ) {
          sb.append("<br>").append(track.getResourceLocator().getHttpErrorCode()).append(" ").append(track.getResourceLocator().getHttpErrorMessage());
        }
      }
      sb.append("</html>");
      this.text = sb.toString();
      trackCltn = new SingleTrackRegion(track);
    }

    public MouseableRegion(Shape region, String name, String value) {

        this.region = region;
        this.text = name + " = " + value;
        trackCltn = new AttributePanelRegion(name, value);
    }

    public MouseableRegion(Shape region, String name, String value, Track track) {

        this.region = region;
        this.text = name + " = " + value;
        trackCltn = new SingleTrackRegion(track);
    }

    public boolean containsPoint(double x, double y) {
        return region.contains(x, y);
    }


    public Rectangle getBounds() {
        return region.getBounds();
    }


    public String getText() {
        return text;
    }

    public String getText(int x, int y) {

    	for ( Track track : getTracks() )
    		if ( track instanceof VariantTrack )
    		{
    			String		sample = ((VariantTrack)track).getSampleAtY(y);
    			if ( sample != null )
    				return sample;
    		}

        return text;
    }

    public Set<Track> getTracks() {
        return trackCltn.getTracks();
    }


    @Override
    public String toString() {
        return region.toString();
    }

    interface TrackCltn {

        public Set<Track> getTracks();

        public boolean contains(Track t);
    }

    class SingleTrackRegion implements TrackCltn {

        private Set<Track> tracks;

        public SingleTrackRegion(Track track) {
            tracks = new HashSet<Track>();
            tracks.add(track);
        }

        public Set<Track> getTracks() {
            return tracks;
        }

        public boolean contains(Track t) {
            return tracks.contains(t);
        }
    }

    class AttributePanelRegion implements TrackCltn {

        private String key;
        private String value;


        public AttributePanelRegion(String key, String value) {
            this.key = key.toUpperCase();
            this.value = value;
        }

        public Set<Track> getTracks() {
            Set<Track> selectedTracks = new HashSet();
            for (Track track : IGV.getInstance().getTrackManager().getAllTracks(true)) {
                String attributeValue = track.getAttributeValue(key);
                if (attributeValue == null) {
                    continue;
                }
                if (attributeValue.equals(value)) {
                    selectedTracks.add(track);
                }
            }
            return selectedTracks;
        }

        public boolean contains(Track t) {
            return getTracks().contains(t);
        }
    }
}
