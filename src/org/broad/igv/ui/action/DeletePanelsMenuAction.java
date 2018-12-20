/*
 * Copyright (c) 2007-2010 by The Broad Institute, Inc. and the Massachusetts Institute of Technology.
 * All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL), Version 2.1 which
 * is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR WARRANTIES OF
 * ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING, WITHOUT LIMITATION, WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT
 * OR OTHER DEFECTS, WHETHER OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR
 * RESPECTIVE TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES OF
 * ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES, ECONOMIC
 * DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER THE BROAD OR MIT SHALL
 * BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT SHALL KNOW OF THE POSSIBILITY OF THE
 * FOREGOING.
 */

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.broad.igv.ui.action;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;
import org.broad.igv.track.Track;
import org.broad.igv.track.TrackGroup;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.UIConstants;
import org.broad.igv.ui.panel.DataPanel;
import org.broad.igv.ui.panel.MainPanel;

/**
 * @author jrobinso
 */
public class DeletePanelsMenuAction extends MenuAction {

    static Logger log = Logger.getLogger(DeletePanelsMenuAction.class);
    IGV mainFrame;

    public DeletePanelsMenuAction(String label, int mnemonic, IGV mainFrame) {
        super(label, null, mnemonic);
        this.mainFrame = mainFrame;
        setToolTipText(UIConstants.DELETE_PANELS_TOOLTIP);
        
    }

    @Override
    /**
     * The action method. A swing worker is used, so "invoke later" and explicit
     * threads are not neccessary.
     *
     */
    public void actionPerformed(ActionEvent e) {
    	
    	MainPanel		mainPanel = IGV.getInstance().getMainPanel();
    	
    	mainPanel.purgeEmptyPanels();
    	
    }

    /**
     * Adjust the height of all tracks so that all tracks fit in the available
     * height of the panel.  This is not possible in all cases as the
     * minimum height for a track is 1 pixel.
     *
     * @param dataPanel
     * @return
     */
    private boolean fitTracksToPanel(DataPanel dataPanel) {

        boolean success = true;

        int visibleHeight = dataPanel.getVisibleHeight();
        int visibleTrackCount = 0;
        int geneTrackHeight = 0;

        // Process data tracks first
        Collection<TrackGroup> groups = dataPanel.getTrackGroups();

        // Count visible tracks and note gene track 'was found' and ist height
        for (TrackGroup group : groups) {
            List<Track> tracks = group.getTracks(true);
            for (Track track : tracks) {
                if (track.isVisible()) {
                    ++visibleTrackCount;
                }
            }
        }


        // Auto resize the height of the visible tracks
        if (visibleTrackCount > 0) {
            int groupGapHeight = (groups.size() + 1) * UIConstants.groupGap;
            int adjustedVisibleHeight = visibleHeight - groupGapHeight;

            if (adjustedVisibleHeight > 0) {

                float delta = (float) adjustedVisibleHeight / visibleTrackCount;

                // If the new track height is less than 1 theres nothing we 
                // can do to force all tracks to fit so we do nothing
                if (delta < 1) {
                    delta = 1;
                }

                int iTotal = 0;
                float target = 0;
                for (TrackGroup group : groups) {
                    List<Track> tracks = group.getTracks(true);
                    for (Track track : tracks) {
                        target += delta;
                        int newHeight = (int) Math.round(target - iTotal);
                        iTotal += newHeight;
                        if (track.isVisible()) {
                            track.setHeight(newHeight);
                        }
                    }
                }
            }
        }

        return success;
    }

}
