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

package org.broad.igv.variant;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.apache.log4j.Logger;
import org.broad.igv.track.AttributeManager;
import org.broad.igv.track.FeatureSource;
import org.broad.igv.track.Track;
import org.broad.igv.track.TrackManager;
import org.broad.igv.track.TrackMenuUtils;
import org.broad.igv.track.TribbleFeatureSource;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.IGVContentPane;
import org.broad.igv.ui.TrackFilter;
import org.broad.igv.ui.TrackFilterPane;
import org.broad.igv.ui.panel.AttributeHeaderPanel;
import org.broad.igv.ui.panel.IGVPopupMenu;
import org.broad.igv.ui.util.MessageUtils;
import org.broad.igv.ui.util.SortDialog;
import org.broad.igv.ui.util.UIUtilities;
import org.broad.igv.util.ColorUtilities;
import org.broad.igv.util.Filter;
import org.broad.igv.variant.VariantTrack.ColorMode;
import org.broadinstitute.sting.utils.codecs.vcf.VCFFormatHeaderLine;
import org.broadinstitute.sting.utils.codecs.vcf.VCFHeader;
import org.broadinstitute.sting.utils.codecs.vcf.VCFHeaderLine;

/**
 * User: Jesse Whitworth
 * Date: Jul 16, 2010
 */
public class VariantMenu extends IGVPopupMenu {

	private static String globalFilter;
	
    private static Logger log = Logger.getLogger(VariantMenu.class);
    private VariantTrack track;

    static boolean depthSortingDirection;
    static boolean genotypeSortingDirection;
    static boolean sampleSortingDirection;
    static boolean qualitySortingDirection;
    
    private JCheckBox showAllTracksFilterCheckBox = new JCheckBox();
    private JCheckBox matchAllCheckBox = new JCheckBox();
    private JCheckBox matchAnyCheckBox = new JCheckBox();
    private TrackFilterPane trackFilterPane;

    private static class VariantIndependentJMenuItem extends JMenuItem
    {
    	VariantIndependentJMenuItem(String label)
    	{
    		super(label);
    	}
    }
    
    public VariantMenu(final VariantTrack variantTrack, Variant variant) {

        this.track = variantTrack;

        this.addPopupMenuListener(new PopupMenuListener() {
            public void popupMenuWillBecomeVisible(PopupMenuEvent popupMenuEvent) {

            }

            public void popupMenuWillBecomeInvisible(PopupMenuEvent popupMenuEvent) {
                close();
            }

            public void popupMenuCanceled(PopupMenuEvent popupMenuEvent) {
                close();
            }

            private void close() {
                track.clearSelectedVariant();
            }

        });


        //Title
        JLabel popupTitle = new JLabel("<html><b>" + this.track.getName(), JLabel.LEFT);
        Font newFont = getFont().deriveFont(Font.BOLD, 12);
        popupTitle.setFont(newFont);
        add(popupTitle);

        //Change Track Settings
        addSeparator();

        List<Track> selectedTracks = Arrays.asList((Track) variantTrack);
        add(TrackMenuUtils.getTrackRenameItem(selectedTracks));
        add(TrackMenuUtils.getChangeFontItem(selectedTracks));
        add(TrackMenuUtils.getChangeFontColorItem(selectedTracks));


        //Hides
        addSeparator();
        JLabel colorByItem = new JLabel("<html>&nbsp;&nbsp;<b>Color By", JLabel.LEFT);
        add(colorByItem);
        add(getColorByGenotype());
        // DK remove add(getColorByAllele());
        if (track.isEnableMethylationRateSupport()) {
            add(getColorByMethylationRate());
        }
        JMenuItem		custom = getColorByCustom();
        if ( custom != null )
        	add(custom);
        JMenuItem		alternate = getColorByAlternate();
        if ( alternate != null )
        	add(alternate);
        	

        //add(getRenderIDItem());

        //Sorter
        addSeparator();
        for (JMenuItem item : getSortMenuItems(variant)) {
            add(item);
            if (variant == null && !(item instanceof VariantIndependentJMenuItem) ) {
                item.setEnabled(false);
            }
        }

        //Variant Information
        addSeparator();
        JLabel displayHeading = new JLabel("Display Mode", JLabel.LEFT);
        add(displayHeading);
        for (JMenuItem item : getDisplayModeItems()) {
            add(item);
        }

        addSeparator();
        JMenuItem item = new JMenuItem("Change Squished Row Height...");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                int currentValue = track.getSquishedHeight();
                int newValue = TrackMenuUtils.getIntValue("Squished row height", currentValue);
                if (newValue != Integer.MIN_VALUE) {
                    track.setSquishedHeight(newValue);
                    IGV.getInstance().getContentPane().repaint();
                }
            }
        });
        add(item);
        
        item = new JMenuItem("Change Variant Band Height...");
        item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {
                int currentValue = track.getVariantBandHeight();
                int newValue = TrackMenuUtils.getIntValue("Variant band height", currentValue);
                if (newValue != Integer.MIN_VALUE) {
                    track.setVariantBandHeight(newValue);
                    IGV.getInstance().getContentPane().repaint();
                }
            }
        });
        add(item);

        add(getHideFilteredItem());
        add(getFeatureVisibilityItem());

        addSeparator();
        add(TrackMenuUtils.getRemoveMenuItem(Arrays.asList(new Track[]{this.track})));
        add(TrackMenuUtils.getReloadMenuItem(Arrays.asList(new Track[]{this.track})));
        TrackMenuUtils.addMoveMenuItem(this, Arrays.asList(new Track[]{this.track}));
        add(TrackMenuUtils.getInfoMenuItem(Arrays.asList(new Track[]{this.track})));
}

    private JMenuItem getFeatureVisibilityItem() {
        JMenuItem item = new JMenuItem("Set Feature Visibility Window...");
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                changeVisibilityWindow();
                IGV.getInstance().getContentPane().repaint();
            }
        });
        return item;
    }

    private JMenuItem getColorByGenotype() {
        final JMenuItem item = new JCheckBoxMenuItem("Genotype", track.getColorMode() == VariantTrack.ColorMode.GENOTYPE);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                track.setColorMode(VariantTrack.ColorMode.GENOTYPE);
                IGV.getInstance().getContentPane().repaint();
            }
        });
        return item;
    }


    private JMenuItem getColorByAllele() {
        final JMenuItem item = new JCheckBoxMenuItem("Allele", track.getColorMode() == VariantTrack.ColorMode.ALLELE);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                track.setColorMode(VariantTrack.ColorMode.ALLELE);
                IGV.getInstance().getContentPane().repaint();
            }
        });
        return item;
    }

    private JMenuItem getColorByMethylationRate() {
        final JMenuItem item = new JCheckBoxMenuItem("Methylation Rate", track.getColorMode() == VariantTrack.ColorMode.METHYLATION_RATE);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                track.setColorMode(VariantTrack.ColorMode.METHYLATION_RATE);
                IGV.getInstance().getContentPane().repaint();
            }
        });
        return item;
    }



    
    private JMenuItem getColorByCustom() {

    	JMenuItem			menu = new JMenu("Custom");
    	Map<String, String>	colors = track.getTrackCustomColors();

    	for ( Map.Entry<String, String> entry : colors.entrySet() )
    	{
    		String		menuValue = entry.getKey();
    		String		menuName = entry.getValue();
    		
			final String	value = menuValue;
			String			name = menuName + " (" + value + ")";
			boolean			checked = (track.getColorMode() == VariantTrack.ColorMode.CUSTOM) 
										&& (value.equals(track.getColoringCustom()));
	        final JMenuItem item = new JCheckBoxMenuItem(name, checked);
	        item.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent evt) {
	                track.setColorMode(VariantTrack.ColorMode.CUSTOM);
	                track.setColoringCustom(value);
	                IGV.getInstance().getContentPane().repaint();
	            }
	        });
	        
	        menu.add(item);    		
    	}
    	
    	return (colors.size() > 0) ? menu : null;
    }

    private JMenuItem getColorByAlternate() {

    	String				extension = ".vcf";
    	String				colorBy = ".color_by.";
    	
    	JMenuItem			menu = new JMenu("Alternate");
    	List<String>		names = new LinkedList<String>();
    	
    	// establish base name and folder
    	File				path = new File(track.getResourceLocator().getPath());
    	if ( !path.getName().endsWith(extension) )
    		return null;
    	File				folder = path.getParentFile();
    	String				name = path.getName().substring(0, path.getName().length() - extension.length());
    	int					index = name.indexOf(colorBy);
    	if ( index >= 0 )
    		name = name.substring(0, index);
    	
    	// loop on folder and add found names
    	File[]				files = folder.listFiles();
    	if ( files != null )
	    	for ( File file : files )
	    	{
	    		String		filename = file.getName();
	    		if ( !filename.endsWith(extension) )
	    			continue;
	    		
	    		if ( filename.equals(name + extension) || filename.startsWith(name + colorBy) )
	    			names.add(filename);
	    	}
    	
    	// if only one name, no sense to continue
    	if ( names.size() <=  1 )
    		return null;
    	Collections.sort(names);

    	// add entries to menu
    	for ( final String filename : names )
    	{
			boolean			checked = path.getName().equals(filename);
			final JMenuItem item = new JCheckBoxMenuItem(filename, checked);
			final String alternate = folder.getAbsolutePath() + File.separator + filename;
			item.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent evt) {
					IGV.getInstance().replaceAlterante(track, alternate, false);
				}
			});

			menu.add(item);
    	}
    	return menu;
    }
    
    private JMenuItem getHideFilteredItem() {
        JMenuItem item = new JCheckBoxMenuItem("Suppress Filtered Sites", track.getHideFiltered());
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                track.setHideFiltered(!track.getHideFiltered());
                IGV.getInstance().getContentPane().repaint();
            }
        });
        return item;
    }


    public JMenuItem getGenotypeSortItem(final Variant variant) {

        JMenuItem item = new JMenuItem("Sort By Color");
        if (variant != null) {
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    GenotypeComparator compare = new GenotypeComparator(variant);
                    genotypeSortingDirection = !genotypeSortingDirection;
                    track.sortSamples(compare);
                    IGV.getInstance().getContentPane().repaint();
                }
            });
        }

        return item;
    }

    public JMenuItem getSampleNameSortItem(final Variant variant) {
        JMenuItem item = new VariantIndependentJMenuItem("Sort By Sample Name");
        if (true) {
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    Comparator<String> compare = new Comparator<String>() {
                        public int compare(String o, String o1) {
                            if (sampleSortingDirection) {
                                return o.compareTo(o1);
                            } else {
                                return o1.compareTo(o);
                            }
                        }
                    };
                    sampleSortingDirection = !sampleSortingDirection;
                    track.sortSamples(compare);
                    IGV.getInstance().getContentPane().repaint();
                }
            });
        }
        return item;
    }

    public JMenuItem getDepthSortItem(final Variant variant) {
        JMenuItem item = new JMenuItem("Sort By Depth");
        if (variant != null) {
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    DepthComparator compare = new DepthComparator(variant);
                    depthSortingDirection = !depthSortingDirection;
                    track.sortSamples(compare);
                    IGV.getInstance().getContentPane().repaint();
                }
            });

        }
        return item;
    }

    public JMenuItem getQualitySortItem(final Variant variant) {
        JMenuItem item = new JMenuItem("Sort By Quality");
        if (variant != null) {
            double quality = variant.getPhredScaledQual();
            if (quality > -1) {
                item.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        QualityComparator compare = new QualityComparator(variant);
                        qualitySortingDirection = !qualitySortingDirection;
                        track.sortSamples(compare);
                        IGV.getInstance().getContentPane().repaint();
                    }
                });
            } else {
                item.setEnabled(false);
            }
        }

        return item;
    }

    public JMenuItem getAttributesSortItem(final Variant variant) {
        JMenuItem item = new VariantIndependentJMenuItem("Sort By Attributes ...");
        if (true) {
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                	UIUtilities.invokeOnEventThread(new Runnable() {

                        public void run() {
                            doSortVariantsByAttribute(variant);
                        }
                    });
                }
            });
        }

        return item;
    }

    public JMenuItem getAttributesFilterItem(final Variant variant) {
        JMenuItem item = new VariantIndependentJMenuItem("Filter By Attributes ...");
        if (true) {
            item.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                	UIUtilities.invokeOnEventThread(new Runnable() {

                        public void run() {
                            doFilterVariantsByAttribute(variant);
                        }
                    });
                }
            });
        }

        return item;
    }

    public void changeVisibilityWindow() {
        int value = getIntValue("Visibility Window", track.getVisibilityWindow());
        if (value > 0) {
            track.setVisibilityWindow(value);
        }
    }

    private static int getIntValue(String parameter, int value) {
        while (true) {
            String height = JOptionPane.showInputDialog(
                    IGV.getMainFrame(), parameter + ": ",
                    String.valueOf(value));
            if ((height == null) || height.trim().equals("")) {
                return Integer.MIN_VALUE;   // <= the logical "null" value
            }

            try {
                value = Integer.parseInt(height);
                return value;
            } catch (NumberFormatException numberFormatException) {
                JOptionPane.showMessageDialog(IGV.getMainFrame(),
                        parameter + " must be an integer number.");
            }
        }
    }


    public Collection<JMenuItem> getSortMenuItems(Variant variant) {

        java.util.List<JMenuItem> items = new ArrayList<JMenuItem>();
        items.add(getGenotypeSortItem(variant));
        items.add(getSampleNameSortItem(variant));
        items.add(getDepthSortItem(variant));
        items.add(getQualitySortItem(variant));
        items.add(getAttributesSortItem(variant));
        items.add(getAttributesFilterItem(variant));
        return items;
    }

    public List<JMenuItem> getDisplayModeItems() {

        List<JMenuItem> items = new ArrayList();

        ButtonGroup group = new ButtonGroup();

        Track.DisplayMode displayMode = track.getDisplayMode();

        JRadioButtonMenuItem m1 = new JRadioButtonMenuItem("Collapsed");
        m1.setSelected(displayMode == Track.DisplayMode.COLLAPSED);
        m1.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                track.setDisplayMode(Track.DisplayMode.COLLAPSED);
                IGV.getInstance().doRefresh();
            }
        });

        JRadioButtonMenuItem m2 = new JRadioButtonMenuItem("Squished");
        m2.setSelected(displayMode == Track.DisplayMode.SQUISHED);
        m2.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                track.setDisplayMode(Track.DisplayMode.SQUISHED);
                IGV.getInstance().doRefresh();
            }
        });

        JRadioButtonMenuItem m3 = new JRadioButtonMenuItem("Expanded");
        m3.setSelected(displayMode == Track.DisplayMode.EXPANDED);
        m3.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                track.setDisplayMode(Track.DisplayMode.EXPANDED);
                IGV.getInstance().doRefresh();
            }
        });


        items.add(m1);
        items.add(m2);
        items.add(m3);
        group.add(m1);
        group.add(m2);
        group.add(m3);

        return items;
    }


     class GenotypeComparator implements Comparator<String> {

        Variant variant;

        GenotypeComparator(Variant variant) {
            this.variant = variant;
        }

        public int compare(String e1, String e2) {

        	ColorMode		colorMode = track.getColorMode();
        	Genotype		g1 = variant.getGenotype(e1);
        	Genotype		g2 = variant.getGenotype(e2);
        	
        	
        	if ( colorMode != ColorMode.CUSTOM )
        	{
	            int genotype1 = classifyGenotype(g1);
	            int genotype2 = classifyGenotype(g2);
	
	            if (genotype2 == genotype1) {
	                return 0;
	            } else if (genotype2 > genotype1) {
	                return genotypeSortingDirection ? 1 : -1;
	            } else {
	                return genotypeSortingDirection ? -1 : 1;
	            }
        	}
        	else
        	{
            	String		field = track.getColoringCustom();
            	
            	String		c1 = g1.getAttributeAsString(field);
            	String		c2 = g2.getAttributeAsString(field);

            	if ( c1 == null )
            		c1 = "";
            	if ( c2 == null )
            		c2 = "";
            	
            	int			order = c1.compareTo(c2);
            	if ( !genotypeSortingDirection )
            		order = -order;
            	
            	return order;
        	}
        }


        private int classifyGenotype(Genotype genotype) {

            if (genotype.isNoCall()) {
                return genotypeSortingDirection ? 1 : 10;
            } else if (genotype.isHomVar()) {
                return 4;
            } else if (genotype.isHet()) {
                return 3;
            } else if (genotype.isHomRef()) {
                return genotypeSortingDirection ? 2 : 9;
            }
            return -1; //Unknown
        }
    }


    static class DepthComparator implements Comparator<String> {

        Variant variant;

        DepthComparator(Variant variant) {
            this.variant = variant;
        }

        public int compare(String s1, String s2) {


            Double readDepth1 = variant.getGenotype(s1).  getAttributeAsDouble("DP");
            Double readDepth2 = variant.getGenotype(s2).getAttributeAsDouble("DP");

            double depth1 = readDepth1 == null ? -1 : readDepth1.doubleValue();
            double depth2  = readDepth2 == null ? -1 : readDepth2.doubleValue();
            if (depth2 == depth1) {
                return 0;
            } else if (depth2 < depth1) {
                return depthSortingDirection ? -1 : 1;
            } else {
                return depthSortingDirection ? 1 : 1;
            }
        }
    }

    static class QualityComparator implements Comparator<String> {

        Variant variant;

        QualityComparator(Variant variant) {
            this.variant = variant;
        }

        public int compare(String s1, String s2) {

            double qual1 = variant.getGenotype(s1).getPhredScaledQual();
            double qual2 = variant.getGenotype(s2).getPhredScaledQual();

            if (qual2 == qual1) {
                return 0;
            } else if (qual2 < qual1) {
                return qualitySortingDirection ? -1 : 1;
            } else {
                return qualitySortingDirection ? 1 : 1;
            }
        }
    }

    static public class AttributesComparator implements Comparator<String> {

        Variant 			variant;
        String[] 			attributeNames;
        boolean[] 			ascending;
        boolean[]			isNumeric;
        AttributeManager 	manager = AttributeManager.getInstance();
        
        public AttributesComparator(String stringRepresentation)
        {
        	String[]		attributeNames = AttributeManager.parseTextRepresentationKeys(stringRepresentation);
        	boolean[]		ascending = AttributeManager.parseTextRepresentationAscending(stringRepresentation);
        	
            init(attributeNames, ascending);
        }

        public AttributesComparator(String[] attributeNames, boolean[] ascending) {
            init(attributeNames, ascending);
        }

        private void init(String[] attributeNames, boolean[] ascending) {
            this.attributeNames = attributeNames;
            this.ascending = ascending;
            
            isNumeric = new boolean[attributeNames.length];
            for ( int n = 0 ; n < isNumeric.length ; n++ )
            	isNumeric[n] = manager.isNumeric(attributeNames[n]);
        }
        public int compare(String s1, String s2) {

        	int		result = 0;

        	for ( int attrIndex = 0 ; attrIndex < attributeNames.length ; attrIndex++ )
        	{
        		String			attrName = attributeNames[attrIndex];
        		String 			v1 = manager.getAttribute(s1, attrName);
        		String			v2 = manager.getAttribute(s2, attrName);
        		
        		result = compareValue(v1, v2, isNumeric[attrIndex], ascending[attrIndex]);
        		
        		if ( result != 0 )
        			return result;
        	}

        	return result;
        }

		private int compareValue(String v1, String v2, boolean isNumeric, boolean ascending) 
		{
			if ( v1 == null )
				return (v2 == null) ? 0 : 1;
			else if ( v2 == null )
				return -1;
			else 
			{
				int		result;
				
				if ( isNumeric )
					result = Double.compare(Double.parseDouble(v1), Double.parseDouble(v2));
				else
					result = v1.compareTo(v2);
				
				if ( !ascending )
					result = -result;
				
				return result;
			}
		}
		
		public String toString()
		{
			return AttributeManager.formatTextRespresentation(attributeNames, ascending);
		}
    }

    final public void doSortVariantsByAttribute(final Variant variant) {

    	IGV			mainFrame = IGV.getFirstInstance();
    	
        List<String> keys = AttributeManager.getInstance().getAttributeNames();
        Object availableSortKeys[] = keys.toArray();
        SortDialog dialog = new SortDialog(mainFrame.getMainFrame(), true, availableSortKeys, true);
        
        // DK - init dialog from current sort attributes on this track
        if ( track.getLastSortComparatorSetting() != null && track.getLastSortComparatorSetting().length() != 0 )
        {
        	AttributesComparator		comparator = new AttributesComparator(track.getLastSortComparatorSetting());
        	dialog.setSelections(comparator.attributeNames, comparator.ascending);
        }
        
        dialog.setVisible(true);

        if (dialog.isCanceled()) {
            return;
        }

        String[] selectedSortKeys = dialog.getSelectedSortKeys();
        boolean[] ascending = dialog.isAscending();
        
        if (selectedSortKeys != null) {

        	AttributesComparator compare = new AttributesComparator(selectedSortKeys, ascending);
        	
        	if ( dialog.isSortSingleTrack() )
        	{
        		AttributeHeaderPanel.reportSort(null, null);
        		track.sortSamples(compare);
        	}
        	else for ( VariantTrack track : allVariantTracks() )
        	{
        		AttributeHeaderPanel.reportSort(selectedSortKeys, ascending);
        		track.sortSamples(compare);
        	}
        	
            IGV.getInstance().getContentPane().repaint();
        }
    }

    final public void doFilterVariantsByAttribute(final Variant variant) {

    	IGV			mainFrame = IGV.getFirstInstance();

        boolean previousDisableFilterState = showAllTracksFilterCheckBox.isSelected();

        boolean previousMatchAllState = matchAllCheckBox.isSelected();

        List<String> uniqueAttributeKeys = AttributeManager.getInstance().getAttributeNames();

        // Sort the attribute keys if we have any
        if (uniqueAttributeKeys != null) {
            //Collections.sort(uniqueAttributeKeys, AttributeManager.getInstance().getAttributeComparator());
        } else // If we have no attribute we can't display the
            // track filter dialog so say so and return
            if (uniqueAttributeKeys == null || uniqueAttributeKeys.isEmpty()) {

                MessageUtils.showMessage("No attributes found to use in a filter");
                return;
            }

        if (trackFilterPane == null) {
        	
        	// DK restore filter
        	TrackFilter		filter = mainFrame.getSession().getFilter(); 
        	if ( filter == null )
        	{
        		if ( track.getLastFilterSetting() != null && track.getLastFilterSetting().length() != 0 )
        			filter = new TrackFilter(track.getLastFilterSetting());
        	}
        	
            trackFilterPane = new TrackFilterPane(uniqueAttributeKeys, "Show tracks whose attribute",
                    filter);
            if ( filter != null )
            	trackFilterPane.setMatchAll(filter.getMatchAll());

        } else {

            trackFilterPane.setItems(uniqueAttributeKeys);

            // Backup the initial state for restores
            trackFilterPane.backup();
            Filter filter = trackFilterPane.getFilter();
            if (filter == null || filter.isEmpty()) {
                trackFilterPane.more();
            }
        }

        trackFilterPane.clearTracks();
        trackFilterPane.addTracks(IGV.getInstance().getTrackManager().getAllTracks(false));

        while (true) {

            Integer response = createFilterTrackDialog(mainFrame.getMainFrame(), trackFilterPane, "Filter Tracks");

            if (response == null) {
                continue;
            }

            if (response.intValue() == JOptionPane.CANCEL_OPTION) {

                // Restore previous filter state
                boolean disableFilterState = showAllTracksFilterCheckBox.isSelected();
                if (disableFilterState != previousDisableFilterState) {
                    showAllTracksFilterCheckBox.setSelected(previousDisableFilterState);
                }

                // Restore previous boolean match state
                boolean matchAllState = matchAllCheckBox.isSelected();
                if (matchAllState != previousMatchAllState) {
                    matchAllCheckBox.setSelected(previousMatchAllState);
                    matchAnyCheckBox.setSelected(!previousMatchAllState);
                }
                // Reset state
                trackFilterPane.restore();
                return;
            } else if ((response.intValue() == JOptionPane.OK_OPTION) || (response.intValue() == JOptionPane.NO_OPTION)) {

                filterVariants(variant, trackFilterPane, response.intValue() == JOptionPane.OK_OPTION);
                
                break;
            }
        }

        // Update the state of the current tracks
        mainFrame.doRefresh();
    }

	private void filterVariants(Variant variant, TrackFilterPane trackFilterPane, boolean filterAllTracks) 
	{
        boolean 		showAllTracks = showAllTracksFilterCheckBox.isSelected();
        TrackFilter 	filter = trackFilterPane.getFilter();
        
        if ( !filterAllTracks )
        {
        	track.filterSamples(filter, showAllTracks);
        }
        else 
        {
        	for ( VariantTrack track : allVariantTracks() )
            	track.filterSamples(filter, showAllTracks);
        	globalFilter = showAllTracks ? null : filter.toString();
        }
        	

        IGV.getInstance().getContentPane().repaint();
	}

	private Integer createFilterTrackDialog(Frame parent,
			final TrackFilterPane trackFilterPane, String title) {

		final JScrollPane scrollPane = new JScrollPane(trackFilterPane);
		scrollPane
				.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane
				.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		int optionType = JOptionPane.OK_CANCEL_OPTION;
		int messageType = JOptionPane.PLAIN_MESSAGE;

		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());

		JPanel filterHeaderPanel = new JPanel();
		filterHeaderPanel.setBackground(Color.WHITE);
		filterHeaderPanel.setLayout(new GridLayout(0, 1));
		filterHeaderPanel.add(new JLabel("For attributes that:"));

		ButtonGroup booleanButtonGroup = new ButtonGroup();
		booleanButtonGroup.add(matchAllCheckBox);
		booleanButtonGroup.add(matchAnyCheckBox);

		showAllTracksFilterCheckBox.setText("Disable Filter");
		matchAllCheckBox.setText("Match all of the following");
		matchAnyCheckBox.setText("Match any of the following");
		boolean matchAll = trackFilterPane.getMatchAll();
		if (matchAll) {
			matchAllCheckBox.setSelected(true);
		} else {
			matchAnyCheckBox.setSelected(true);
		}

		matchAllCheckBox.addActionListener(new java.awt.event.ActionListener() {

			public void actionPerformed(java.awt.event.ActionEvent evt) {
				trackFilterPane.setMatchAll(true);
			}
		});
		matchAnyCheckBox.addActionListener(new java.awt.event.ActionListener() {

			public void actionPerformed(java.awt.event.ActionEvent evt) {
				trackFilterPane.setMatchAll(false);
			}
		});

		showAllTracksFilterCheckBox.addActionListener(new java.awt.event.ActionListener() {

			public void actionPerformed(java.awt.event.ActionEvent evt) {
			
				boolean			disableFilter = showAllTracksFilterCheckBox.isSelected();
				
				matchAllCheckBox.setEnabled(!disableFilter);
				matchAnyCheckBox.setEnabled(!disableFilter);
				setEnabledAll(scrollPane, !disableFilter);
			}
			
		});
		if ( track.isLastShowAll() )
		{
			showAllTracksFilterCheckBox.setSelected(true);
			matchAllCheckBox.setEnabled(false);
			matchAnyCheckBox.setEnabled(false);
			setEnabledAll(scrollPane, false);
		}
		

		JPanel controls = new JPanel();
		FlowLayout layoutManager = new FlowLayout();
		layoutManager.setAlignment(FlowLayout.LEFT);
		controls.setLayout(layoutManager);
		controls.add(matchAllCheckBox);
		controls.add(matchAnyCheckBox);
		controls.add(showAllTracksFilterCheckBox);
		controls.setBackground(Color.WHITE);
		controls.setOpaque(true);
		filterHeaderPanel.add(controls);

		panel.setOpaque(true);
		panel.add(filterHeaderPanel, BorderLayout.NORTH);
		panel.add(scrollPane, BorderLayout.CENTER);

		final JOptionPane optionPane = new JOptionPane(panel, messageType,
				optionType);
		
		JButton		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				optionPane.setValue(JOptionPane.CANCEL_OPTION);
			}
		});
		JButton		filterTrackButton = new JButton("Filter Track");
		filterTrackButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				optionPane.setValue(JOptionPane.NO_OPTION);
			}
		});
		JButton		filterAllTracksButton = new JButton("Filter All Tracks");
		filterAllTracksButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				optionPane.setValue(JOptionPane.OK_OPTION);
			}
		});
		
		optionPane.setOptions(new Object[] {filterAllTracksButton, filterTrackButton, cancelButton});
		
		optionPane.setPreferredSize(new Dimension(700, 500));
		optionPane.setOpaque(true);
		optionPane.setBackground(Color.WHITE);
		optionPane.addPropertyChangeListener(JOptionPane.VALUE_PROPERTY,
				new PropertyChangeListener() {

					public void propertyChange(PropertyChangeEvent e) {

						Object value = e.getNewValue();
						if (value instanceof Integer) {

							int option = (Integer) value;
							if (option == JOptionPane.OK_OPTION || option == JOptionPane.NO_OPTION) {

								if (trackFilterPane.isFilterValid()) {
									trackFilterPane.applyFilterMatching();
									trackFilterPane.save();
								}
							}
						}
					}
				});

		JDialog dialog = optionPane.createDialog(parent, title);
		dialog.setBackground(Color.WHITE);
		dialog.getContentPane().setBackground(Color.WHITE);

		Dimension maximumSize = new Dimension(dialog.getSize().width, 100);
		if (maximumSize != null) {
			dialog.setMaximumSize(maximumSize);
		}

		Component[] children = optionPane.getComponents();
		if (children != null) {
			for (Component child : children) {
				child.setBackground(Color.WHITE);
			}

		}

		dialog.pack();
		dialog.setVisible(true);

		Object selectedValue = optionPane.getValue();
		if (selectedValue == null) {
			return JOptionPane.CANCEL_OPTION;
		} else if (((Integer) selectedValue).intValue() == JOptionPane.OK_OPTION) {
			if (!trackFilterPane.isFilterValid()
					&& !showAllTracksFilterCheckBox.isSelected()) {
				JOptionPane
						.showMessageDialog(
								parent,
								"Some of the filter values are missing."
										+ "\nPlease enter all value before pressing ok.");

				selectedValue = null;
			}
		}
		return ((Integer) selectedValue);
	}
	
	private void setEnabledAll(Component comp, boolean enabled) 
	{
		comp.setEnabled(enabled);
		if ( comp instanceof Container )
			for ( Component sub : ((Container)comp).getComponents() )
				setEnabledAll(sub, enabled);
	}
	
	private List<VariantTrack> allVariantTracks()
	{
		List<VariantTrack>		tracks = new LinkedList<VariantTrack>();
		
    	List<TrackManager>		trackManagers = new LinkedList<TrackManager>();
    	if ( !IGVContentPane.isUseTabs() )
    		trackManagers.add(IGV.getInstance().getTrackManager());
    	else
    		trackManagers.addAll(IGV.getInstance().getContentPane().tabsListTrackManagers());
    	
    	for ( TrackManager trackManager : trackManagers )
    		for ( Track track : trackManager.getAllTracks(false) )
    			if ( track instanceof VariantTrack )
    				tracks.add((VariantTrack)track);
    	
    	return tracks;
	}

	public static void clearGlobalFilter() {
		globalFilter = null;
	}

	public static void setGlobalFilter(String globalFilter) {
		VariantMenu.globalFilter = globalFilter;
	}

	public static String getGlobalFilter() {
		return globalFilter;
	}
}
