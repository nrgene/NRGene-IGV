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
package org.broad.igv.ui.action;

import java.awt.event.ActionEvent;

import javax.swing.JOptionPane;

import org.broad.igv.ui.IGV;
import org.broad.igv.ui.IGVContentPane;
import org.broad.igv.ui.UIConstants;
import org.broad.igv.ui.util.UIUtilities;

/**
 * @author jrobinso
 */
public class NewSessionMenuAction extends MenuAction {

    IGV mainFrame;

    public NewSessionMenuAction(String label, int mnemonic, IGV mainFrame) {
        super(label, null, mnemonic);
        this.mainFrame = mainFrame;
        setToolTipText(UIConstants.NEW_SESSION_TOOLTIP);
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        UIUtilities.invokeOnEventThread(new Runnable() {

            public void run() {
            	
                // If anything has been loaded warn the users.  Popping up the
                // warning all the time will get annoying.
                if (IGV.getInstance().getTrackManager().getAllTracks(false).size() > 0) {
                    int status =
                            JOptionPane.showConfirmDialog(
                                    mainFrame.getMainFrame(),
                                    UIConstants.NEW_SESSION_MESSAGE,
                                    null,
                                    JOptionPane.OK_CANCEL_OPTION,
                                    JOptionPane.PLAIN_MESSAGE,
                                    null);

                    if (status == JOptionPane.CANCEL_OPTION ||
                            status == JOptionPane.CLOSED_OPTION) {
                        return;
                    }
                }

            	if ( IGVContentPane.isUseTabs() )
            		IGV.getInstance().getContentPane().tabsReset();
            	
                mainFrame.createNewSession(null); // Clear everything but the genome 

            }
        });
    }
}
