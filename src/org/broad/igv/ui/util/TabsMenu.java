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

package org.broad.igv.ui.util;

import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import org.broad.igv.ui.IGV;
import org.broad.igv.ui.IGVContentPane;
import org.broad.igv.ui.action.MenuAction;

/**
 * @author jrobinso
 * @date Sep 1, 2010
 */
public class TabsMenu extends JMenu {
	
	List<JMenuItem>		conditionalItems = new LinkedList<JMenuItem>();
	private JMenuItem mappingFailturePopupItem;

    public TabsMenu() {
        this("Tabs");
    }


    @SuppressWarnings("serial")
	public TabsMenu(String name) {
        super(name);

        JMenuItem	item;
        
        add(item = new JMenuItem("New Tab"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
            	
            	IGV.getInstance().getContentPane().tabsCreate();
            	updateConditionals();
            }
        });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, Event.CTRL_MASK));
        item.setMnemonic(KeyEvent.VK_T);
        
        add(item = new JMenuItem("Duplicate Tab"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
            	
            	IGV.getInstance().getContentPane().tabsDuplicate();
            	updateConditionals();
            }
        });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, Event.CTRL_MASK | Event.SHIFT_MASK));
        item.setMnemonic(KeyEvent.VK_T);

        add(new JSeparator());
        
        add(item = new JMenuItem("Close Tab"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
            	
            	IGV.getInstance().getContentPane().tabsClose();
            	updateConditionals();
            }
        });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Event.CTRL_MASK));
        item.setMnemonic(KeyEvent.VK_W);
        conditionalItems.add(item);
    
        add(item = new JMenuItem("Close Other Tabs"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {

            	IGV.getInstance().getContentPane().tabsCloseOther();
            	updateConditionals();
            }
        });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_W, Event.CTRL_MASK | Event.SHIFT_MASK));
        item.setMnemonic(KeyEvent.VK_W);
        conditionalItems.add(item);
    
        add(new JSeparator());

        add(item = new JMenuItem("Rename Tab"));
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
            	
            	IGV.getInstance().getContentPane().tabsRename();
            	updateConditionals();
            }
        });
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, Event.CTRL_MASK | Event.SHIFT_MASK));
        item.setMnemonic(KeyEvent.VK_R);

        if ( IGVContentPane.isFrameTabs() )
        {
	        add(item = new JMenuItem("Frame Tab"));
	        item.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent actionEvent) {
	            	
	            	IGV.getInstance().getContentPane().tabsFrame();
	            	updateConditionals();
	            }
	        });
	        conditionalItems.add(item);
        }
        
        add(new JSeparator());
        MenuAction		menuAction;
        if ( IGV.getInstance().getContentPane().tabsShowGenomeOption() )
        {
			menuAction = new MenuAction("Show Tab Genome", null, KeyEvent.VK_G) {
	
				@Override
				public void actionPerformed(ActionEvent event) {
					JCheckBoxMenuItem		menuItem = (JCheckBoxMenuItem)event.getSource();
					
					IGV.getInstance().getContentPane().tabsSetShowGenome(menuItem.isSelected());
				}
	        	
	        };
	        item = MenuAndToolbarUtils.createMenuItem(menuAction, IGV.getInstance().getContentPane().tabsGetShowGenome());
	        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_G, Event.CTRL_MASK));
	        add(item);
        }

        menuAction = new MenuAction("Inter-Genome Locus Mapping", null, KeyEvent.VK_M) {

			@Override
			public void actionPerformed(ActionEvent event) {
				JCheckBoxMenuItem		menuItem = (JCheckBoxMenuItem)event.getSource();
				
				IGV.getInstance().getContentPane().tabsSetMappingEnabled(menuItem.isSelected());
			}
        	
        };
        item = MenuAndToolbarUtils.createMenuItem(menuAction, IGV.getInstance().getContentPane().tabsGetMappingEnabled());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_M, Event.CTRL_MASK));
        add(item);
        
		menuAction = new MenuAction("Mapping Failure Popup", null, KeyEvent.VK_P) {

			@Override
			public void actionPerformed(ActionEvent event) {
				JCheckBoxMenuItem		menuItem = (JCheckBoxMenuItem)event.getSource();
				boolean					selected = menuItem.isSelected();
				
				IGV.getInstance().getContentPane().tabsSetMappingFailurePopup(selected);
				IGV.getInstance().getContentPane().getStatusBar().setMessage("Mapping Failure Popup: " + (selected ? "ON" : "OFF"));
			}
        	
        };
        item = MenuAndToolbarUtils.createMenuItem(menuAction, IGV.getInstance().getContentPane().tabsGetMappingFailurePopup());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK | Event.SHIFT_MASK));
        item.setEnabled(IGV.getInstance().getContentPane().tabsGetMappingEnabled());
        add(item);
        mappingFailturePopupItem = item;
        
        this.addMenuListener(new MenuListener() {
            public void menuSelected(MenuEvent menuEvent) 
            {
            	updateConditionals();
            }

			public void menuCanceled(MenuEvent e) {
			}

			public void menuDeselected(MenuEvent e) {
			}
        });
    }
    
    private void updateConditionals()
    {
    	int			tabCount = IGV.getInstance().getContentPane().tabsCount();

        for ( JMenuItem item : conditionalItems )
        	item.setEnabled(tabCount > 1);
        
        mappingFailturePopupItem.setEnabled(IGV.getInstance().getContentPane().tabsGetMappingEnabled());
    }
}
