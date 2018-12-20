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

package org.broad.igv.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicBorders;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.broad.igv.PreferenceManager;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.genome.mapping.GenomeLocus;
import org.broad.igv.feature.genome.mapping.GenomeMapperException;
import org.broad.igv.feature.genome.mapping.IGenomeMapper;
import org.broad.igv.nrgene.DraggableTabbedPane;
import org.broad.igv.nrgene.TabCloseIcon;
import org.broad.igv.renderer.DataRange;
import org.broad.igv.track.DataSourceTrack;
import org.broad.igv.track.SequenceTrack;
import org.broad.igv.track.Track;
import org.broad.igv.track.TrackManager;
import org.broad.igv.ui.panel.MainPanel;
import org.broad.igv.ui.panel.TrackNamePanel;
import org.broad.igv.ui.panel.TrackPanel;
import org.broad.igv.ui.panel.TrackPanelScrollPane;
import org.broad.igv.ui.util.ApplicationStatusBar;

import com.jidesoft.swing.JideTabbedPane;

/**
 * @author jrobinso
 *         <p/>
 *         Notes;
 *         <p/>
 *         The painting architecture of Swing requires an opaque JComponent to exist in the containment hieararchy above all
 *         other components. This is typically provided by way of the content pane. If you replace the content pane, it is
 *         recommended that you make the content pane opaque by way of setOpaque(true). Additionally, if the content pane
 *         overrides paintComponent, it will need to completely fill in the background in an opaque color in paintComponent.
 * @date Apr 4, 2011
 */
public class IGVContentPane extends JPanel {


    private static Logger log = Logger.getLogger(IGVContentPane.class);

    private JPanel commandBarPanel;
    private IGVCommandBar igvCommandBar;
    private MainPanel mainPanel;
    private ApplicationStatusBar statusBar;
    
    private TrackManager trackManager;

    private static final boolean useTabs = true;
    private static final boolean tabTrackManager = true;
    private static boolean frameTabs = System.getProperty("frameTabs") != null;
    private JTabbedPane		tabbedPane;
    private int				tabNameCounter = 0;
    private boolean			useJideTabbedPane = false;
    private boolean			useDraggableTabbedPane = true;
    private boolean			useCloseIcon = true;

    private Map<Genome,MainPanel> lastMainPanelForGenome = new LinkedHashMap<Genome, MainPanel>();
    private Map<Genome,String>	lastLocusForGenome = new LinkedHashMap<Genome, String>();
    private Genome			lastLocusGenome = null;
    private Map<String,String> lastMappings = new LinkedHashMap<String, String>();
    private boolean 		useLastMappings = false;
    private int				suspendMapping = 0;

	private boolean showGenomeOption = false;
	
	private Stack<Integer>	lastTabsSwitchToHistory = new Stack<Integer>();
    
    /**
     * Creates new form IGV
     */
    public IGVContentPane(TrackManager trackManager) {

        this.trackManager = trackManager;

        // Create components

        setLayout(new BorderLayout());

        commandBarPanel = new JPanel();
        BoxLayout layout = new BoxLayout(commandBarPanel, BoxLayout.PAGE_AXIS);

        commandBarPanel.setLayout(layout);
        add(commandBarPanel, BorderLayout.NORTH);

        igvCommandBar = new IGVCommandBar();
        igvCommandBar.setMinimumSize(new Dimension(250, 33));
        igvCommandBar.setBorder(new BasicBorders.MenuBarBorder(Color.GRAY, Color.GRAY));
        igvCommandBar.setAlignmentX(Component.BOTTOM_ALIGNMENT);
        commandBarPanel.add(igvCommandBar);

        // DK - TABS
        if ( !useTabs )
        {
        	mainPanel = new MainPanel(trackManager);
        	add(mainPanel, BorderLayout.CENTER);
        }
        else
        {
        	if ( useJideTabbedPane )
        	{
        		JideTabbedPane	jideTabbedPane = new JideTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        		
        		tabbedPane = jideTabbedPane; 
        	}
        	else if ( useDraggableTabbedPane )
        	{
        		DraggableTabbedPane	draggableTabbedPane = new DraggableTabbedPane();
        		
        		draggableTabbedPane.setSetCurrentAfterDrop(true);
        		
        		tabbedPane = draggableTabbedPane;
        	}
        	else
        		tabbedPane = new JTabbedPane();
        	add(tabbedPane, BorderLayout.CENTER);
        	
        	mainPanel = new MainPanel(trackManager);
        	
        	String		tabName = tabsAutoName();
        	mainPanel.setTabName(tabName);
        	tabbedPane.addTab(tabName, mainPanel);
        	if ( useCloseIcon )
        		tabbedPane.setIconAt(0, new TabCloseIcon());
        	
        	tabbedPane.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                	
               		tabsSwitchTo(tabbedPane.getSelectedIndex());
                }
            });
        	
        }

        statusBar = new ApplicationStatusBar();
        statusBar.setDebugGraphicsOptions(javax.swing.DebugGraphics.NONE_OPTION);
        add(statusBar, BorderLayout.SOUTH);


    }

	public void addCommandBar(JComponent component) {
        component.setBorder(new BasicBorders.MenuBarBorder(Color.GRAY, Color.GRAY));
        component.setAlignmentX(Component.BOTTOM_ALIGNMENT);
        commandBarPanel.add(component);
        commandBarPanel.invalidate();
    }

    public void removeCommandBar(JComponent component) {
        commandBarPanel.remove(component);
        commandBarPanel.invalidate();
    }

    @Override
    public Dimension getPreferredSize() {
        return UIConstants.preferredSize;
    }


    public void repaintDataPanels() {
        for (TrackPanelScrollPane tsv : trackManager.getTrackPanelScrollPanes()) {
            tsv.getDataPanel().repaint();
        }

    }


    final public void doRefresh() {

        mainPanel.revalidate();
        repaint();
        //getContentPane().repaint();
    }

    /**
     * Reset the default status message, which is the number of tracks loaded.
     */
    public void resetStatusMessage() {
        statusBar.setMessage("" +
                IGV.getInstance().getTrackManager().getVisibleTrackCount() + " tracks loaded");

    }

    public MainPanel getMainPanel() {
        return mainPanel;
    }

    public IGVCommandBar getCommandBar() {
        return igvCommandBar;
    }

    public void chromosomeChanged(String chrName) {
        igvCommandBar.chromosomeChanged(chrName);
    }

    public void updateCurrentCoordinates() {
        igvCommandBar.updateCurrentCoordinates();
    }

    public ApplicationStatusBar getStatusBar() {

        return statusBar;
    }

	public static boolean isUseTabs() {
		return useTabs;
	}

	public int tabsCount() {
		return tabbedPane.getComponentCount();
	}
	
	public MainPanel tabsCreate() {
		return tabsCreate(null);
	}

	
	public MainPanel tabsCreate(String tabName) {
						
		TrackManager newTrackManager = trackManager;
		if ( tabTrackManager )
		{
			newTrackManager = new TrackManager(IGV.getInstance(), null);
			newTrackManager.setSequenceTrack(trackManager.getSequenceTrack());
			newTrackManager.setGeneTrack(trackManager.getGeneTrack());
		}

		if ( tabName == null )
			tabName = tabsAutoName();
		
    	MainPanel	tabMainPanel = new MainPanel(newTrackManager);
    	if ( tabTrackManager )
    		newTrackManager.setMainPanel(tabMainPanel);
    	tabMainPanel.setTabName(tabName);
    	tabMainPanel.setGenome(IGV.getInstance().getGenomeManager().currentGenome);
    	tabbedPane.addTab(tabsCalcTabTitle(tabMainPanel), tabMainPanel);    	
    	tabbedPane.setSelectedComponent(tabMainPanel);
    	if ( useCloseIcon )
    		tabbedPane.setIconAt(tabbedPane.getSelectedIndex(), new TabCloseIcon());
    	    	
    	tabsSwitchTo(tabbedPane.getSelectedIndex());

    	mainPanel.resetPanels();
    	tabsUpdateShowGenome();
    	
    	return mainPanel;
	}
	
	public void tabsDuplicate() 
	{
		// save current tab
		String		currentTab = tabsCurrentTab();
		
		// create a new (switches to it)
		tabsCreate();
		String		newTab = tabsCurrentTab();
		
		// switch back to old current
		tabsSwitchTo(currentTab);
		
		// loop over tracks and copy to new tab
		List<Track>		tracks = new LinkedList<Track>();
		tracks.addAll(IGV.getInstance().getTrackManager().getAllTracks(false));
		for ( Track track : tracks )
			tabsCopyTrackTo(track, newTab);
		
		// switch to new tab
		tabsSwitchTo(newTab);
		
		// remove empty panels
		mainPanel.removeEmptyDataPanels();
		
		// rename to a "dup of" name
		tabsRename("Dup of " + currentTab);
	}
	
	public void tabsReset() 
	{
		String		tabName;
		
		tabsCloseOther();
		
		tabNameCounter = 0;
		tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), tabName = tabsAutoName(true));
		
		mainPanel.setGenome(IGV.getInstance().getGenomeManager().currentGenome);
		mainPanel.setTabName(tabName);
		
    	tabsUpdateShowGenome();
	}

	public Component tabsClose() 
	{
		Component	current = (MainPanel)tabbedPane.getSelectedComponent();
		TabCloseIcon.cleanupIconAt(tabbedPane, tabbedPane.getSelectedIndex());

		tabbedPane.remove(current);
		mainPanel = (MainPanel)tabbedPane.getSelectedComponent();
		
    	if ( tabTrackManager )
    		IGV.getInstance().setTrackManager(trackManager = mainPanel.getTrackManager());

    	tabsUpdateShowGenome();
    	tabsRealizeCurrentGenome();
    	
		return current;
	}

	public void tabsCloseOther() 
	{
		MainPanel			current = (MainPanel)tabbedPane.getSelectedComponent();

		for ( Component other : tabbedPane.getComponents() )
			if ( other != current )
			{
				MainPanel	otherPanel = (MainPanel)other;
				otherPanel.resetPanels();
				
				int			index = getComponentIndex(other);
				if ( index >= 0 )
					TabCloseIcon.cleanupIconAt(tabbedPane, index);
				tabbedPane.remove(other);
			}

		TrackNamePanel.removeDropListenersForAllMainPanelsExceptFor(current);
		
    	tabsUpdateShowGenome();
	}

	private int getComponentIndex(Component c) 
	{
		for ( int index = 0 ; index < tabbedPane.getComponentCount() ; index++ )
		{
			if ( tabbedPane.getComponentAt(index) == c )
				return index;
		}
		
		return -1;
	}

	public void tabsRename() 
	{
		tabsDumpState("before rename");
		
		String				title = tabsGetTitleAt(tabbedPane.getSelectedIndex());
		
		String				newTitle = JOptionPane.showInputDialog("New title:", title);
		if ( newTitle != null )
		{
			// no change?
			if ( newTitle.equals(title) )
				return;
			
			// trying to change into an existing name?
			if ( tabsNameList().contains(newTitle) )
			{
				// prevent renaming into an existing name
		        JOptionPane.showMessageDialog(IGV.getMainFrame(), "Duplicate tab name: " + newTitle);
		        return;
			}
			
			// if here, rename
			tabsRename(newTitle);
		}
	}
	
	public void tabsRename(String newTitle) 
	{
		// make sure the tab name is not duplicate
		String			title = tabbedPane.getTitleAt(tabbedPane.getSelectedIndex());
		if ( !title.equals(newTitle) )
		{
			List<String>		names = tabsNameList();
			int					index = 2;
			String				uniqueTitle = newTitle;
			while ( names.contains(uniqueTitle) )
			{
				uniqueTitle = newTitle + " (" + index + ")";
				index++;
			}
			
			tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), uniqueTitle);
			((MainPanel)tabbedPane.getComponentAt(tabbedPane.getSelectedIndex())).setTabName(uniqueTitle);
	    	tabsUpdateShowGenome();
		}
	}
	
	public void tabsSwitchTo()
	{
		tabsSwitchTo(tabbedPane.getSelectedIndex());
    	tabsUpdateShowGenome();
	}
	
	public void tabsSwitchTo(int tabIndex)
	{
		lastTabsSwitchToHistory.push(tabIndex);
    	
		tabbedPane.setSelectedIndex(tabIndex);
		
		mainPanel = (MainPanel)tabbedPane.getSelectedComponent();
		
    	if ( tabTrackManager )
    		IGV.getInstance().setTrackManager(trackManager = mainPanel.getTrackManager());
    	
    	tabsRealizeCurrentGenome();    	
	}

	public void tabsSwitchTo(String tab)
	{
		tabsSwitchTo(tab, null);
	}
	
	public void tabsSwitchTo(String tab, String genomeId)
	{
		int			tabNameIndex = tabsNameList().indexOf(tab);
		int			tabIndex = tabsList().indexOf(tab);
		
		if ( tabNameIndex >= 0 )
			tabsSwitchTo(tabNameIndex);
		else if ( tabIndex >= 0 )
			tabsSwitchTo(tabIndex);
		else
		{
			// before going a head and creating a tab, see if there is an empty tab with the same genome we can hijack
			if ( genomeId != null )
				for ( int index = 0; index < tabbedPane.getComponentCount() ; index++ )
				{
					MainPanel		mainPanel = (MainPanel)tabbedPane.getComponentAt(index);
					
					if ( mainPanel.getGenome() == null || genomeId.equals(mainPanel.getGenome().getId()) )
					{
						if ( mainPanel.getTrackManager().getAllTracks(false).size() == 0 )
						{
							// found an empty panel with same genome
							mainPanel.setGenome(IGV.getInstance().getGenomeManager().getCachedGenomeById(genomeId));
							tabsSwitchTo(index);
							tabsRename(tab);
							
							return;
						}
					}
				}
			
			// if here, must create
			tabsCreate(tab);
		}
	}

	public List<String> tabsList()
	{
		List<String>	tabs = new LinkedList<String>();
		
		for ( int i = 0 ; i < tabbedPane.getComponentCount() ; i++ )
			tabs.add(tabbedPane.getTitleAt(i));
		
		return tabs;
	}
	
	public List<String> tabsNameList()
	{
		List<String>	tabs = new LinkedList<String>();
		
		for ( int i = 0 ; i < tabbedPane.getComponentCount() ; i++ )
			tabs.add(((MainPanel)tabbedPane.getComponentAt(i)).getTabName());
		
		return tabs;
	}
	
	public List<TrackManager> tabsListTrackManagers()
	{
		List<TrackManager>	trackManagers = new LinkedList<TrackManager>();
		
		if ( tabTrackManager )
		{
			for ( int i = 0 ; i < tabbedPane.getComponentCount() ; i++ )
			{
				MainPanel		mainPanel = (MainPanel)tabbedPane.getComponentAt(i);
				
				trackManagers.add(mainPanel.getTrackManager());
			}
		}
		else
			trackManagers.add(((MainPanel)tabbedPane.getComponentAt(0)).getTrackManager());
		
		
		return trackManagers;
	}
	
	public Map<String,List<TrackPanel>> tabsMapTrackPanels()
	{
		Map<String,List<TrackPanel>>	trackPanels = new LinkedHashMap<String, List<TrackPanel>>();
		
		for ( int i = 0 ; i < tabbedPane.getComponentCount() ; i++ )
		{
			MainPanel		mainPanel = (MainPanel)tabbedPane.getComponentAt(i);
			
			trackPanels.put(tabsGetTitleAt(i), mainPanel.getTrackPanels());
		}		
		
		return trackPanels;
	}
	
	public Map<String,MainPanel> tabsMapMainPanels()
	{
		 Map<String,MainPanel> 	mainPanels = new LinkedHashMap<String,MainPanel>();
		
		if ( tabbedPane != null )
			for ( int i = 0 ; i < tabbedPane.getComponentCount() ; i++ )
			{
				MainPanel		mainPanel = (MainPanel)tabbedPane.getComponentAt(i);
				
				mainPanels.put(tabsGetTitleAt(i), mainPanel);
			}		
		
		return mainPanels;
	}
	
	public String tabsCurrentTab()
	{
		return tabsGetTitleAt(tabbedPane.getSelectedIndex());
	}
	
	public int tabsCurrentTabIndex()
	{
		return tabbedPane.getSelectedIndex();
	}
	
	public void tabsFrame() 
	{
		String				title = tabsGetTitleAt(tabbedPane.getSelectedIndex());
		Component			compoenent = tabsClose();
		JFrame				frame = new JFrame("IGV Frame");
		
		//frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(compoenent, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
	}

	private String tabsAutoName()
	{
		return tabsAutoName(false);
	}

	private String tabsAutoName(boolean force)
	{
		String		name;
		
		do
		{
			name = String.format("Tab %d", ++tabNameCounter);
			if ( force )
				return name;
			
		} while ( tabsNameList().contains(name) );
		
		return name;
	}

	public void tabsMoveTrackTo(Track track, String targetTab)
	{
		tabsMoveOrCopyTrackTo(track, targetTab, true);
	}

	public void tabsCopyTrackTo(Track track, String targetTab)
	{
		tabsMoveOrCopyTrackTo(track, targetTab, false);
	}

	public void tabsMoveOrCopyTrackTo(Track track, String targetTab, boolean isMove)
	{
		// get name of source panel
		TrackPanel			sourcePanel = IGV.getInstance().getPanelForTrack(track);
		if ( sourcePanel == null )
			return;
		String				sourcePanelName = sourcePanel.getName();
		DataRange			dataRange = (track instanceof DataSourceTrack) ? ((DataSourceTrack)track).getDataRange() : null;
		
		// establish the source path
		if ( track.getResourceLocator() == null )
			return;
		String				sourcePath = track.getResourceLocator().getPath();
		
		// get track info
		Map<String, String>		trackPS = track.getPersistentState();		
		
		if ( isMove )
		{
			// remove the track
	    	List<Track>			tracks = new LinkedList<Track>();
	    	tracks.add(track);
	        IGV.getInstance().getTrackManager().removeTracks(tracks);
		}
        
        // switch to new tab
        int				selectedIndex = tabbedPane.getSelectedIndex();
        tabsSwitchTo(targetTab);
        
        // enter into a panel on target tab
        TrackPanel			targetPanel = IGV.getInstance().getDataPanel(sourcePanelName);
        List<Track> 		tracks = IGV.getInstance().getTrackManager().load(sourcePath, targetPanel);
    	
        // restore track props
        for ( Track t : tracks )
        {
        	t.restorePersistentState(trackPS);
        	if ( (t instanceof DataSourceTrack) && (dataRange != null) ) 
        	{
        		DataRange			newDataRange = new DataRange(0, 0, 0);
        		newDataRange.restorePersistentState(dataRange.getPersistentState());
        		((DataSourceTrack)t).setDataRange(newDataRange);
        	}
        }
        
    	// restore source tab
    	tabsSwitchTo(selectedIndex);
    	
	}

	public static boolean isFrameTabs() {
		return frameTabs;
	}

	public boolean tabIsTrackeNamePanelActive(TrackNamePanel trackNamePanel) 
	{
		for ( TrackPanel trackPanel : mainPanel.getTrackPanels() )
			if ( trackPanel.getNamePanel() == trackNamePanel )
				return true;
		
		return false;
	}
	
	private String tabsTabTooltip(MainPanel mainPanel)
	{
		StringBuilder		sb = new StringBuilder();
		Genome				genome = mainPanel.getGenome();
		
		sb.append("<html>");
		sb.append("TabName: " + mainPanel.getTabName());
		sb.append("<br>Genome: " + (genome != null ? genome.getExtendedDisplayName() : "?"));
		sb.append("</html>");
		
		return sb.toString();
	}

	public void tabsGenomeChanged(Genome currentGenome) 
	{
		// premature?
		if ( tabbedPane == null )
			return;
		
		boolean		modified = false;
		
		// loop over tabs
		for ( int index = 0; index < tabbedPane.getComponentCount() ; index++ )
		{
			MainPanel		mainPanel = (MainPanel)tabbedPane.getComponentAt(index);
			
			if ( mainPanel.getGenome() == null )
			{
				mainPanel.setGenome(currentGenome);
				modified = true;
			}
		}
		
		if ( modified )
			tabsUpdateShowGenome();
	}

	public String tabsGenomeTabLocus(Genome genome) throws GenomeMapperException
	{
		return tabsGenomeTabLocus(genome, false);
	}
	
	public String tabsGenomeTabLocus(Genome genome, boolean silent) throws GenomeMapperException
	{
		if ( tabbedPane == null )
			return null;
		
		if ( !isSuspendMapping() && tabsGetMappingEnabled() )
		{
			if ( lastLocusGenome != null && genome != lastLocusGenome && lastLocusForGenome.containsKey(lastLocusGenome)  )
			{
				String			locus = lastLocusForGenome.get(lastLocusGenome);
				if ( !StringUtils.isEmpty(locus) && !locus.equalsIgnoreCase("all") )
				{
					GenomeLocus		fromLocus = new GenomeLocus(locus);
					
					log.info("mapping " + fromLocus + " from " + lastLocusGenome + " to " + genome);
					
					GenomeLocus		toLocus = null;
					IGenomeMapper	mapper = null;
					
					String			fromGenomeId = null;
					String			toGenomeId = null;
					String			fromToKey = null;
					String			toFromKey = null;
				
					try
					{
						fromGenomeId = lastLocusGenome.getId();
						toGenomeId = genome.getId();
						fromToKey = fromGenomeId + " -> " + toGenomeId;
						toFromKey = toGenomeId + " -> " + fromGenomeId;
						
						if ( useLastMappings && lastMappings.containsKey(fromToKey) )
						{
							log.info("using saved mapping " + fromToKey);
							toLocus = new GenomeLocus(lastMappings.get(fromToKey));
						}
						else
						{
							mapper = IGV.getInstance().getGenomeManager().getGenomeMappingManager().getMapper(fromGenomeId, toGenomeId);
							if ( mapper == null )
								throw new GenomeMapperException(fromLocus, "no mapping from " + lastLocusGenome.getId() + " to " + genome.getId());
						
							toLocus = mapper.mapLocus(fromLocus);
							
						}
						if ( toLocus != null )
							lastMappings.put(toFromKey, fromLocus.toString());
							
					}
					catch (GenomeMapperException e)
					{
						if ( silent )
							toLocus = null;
						else
							if ( getCommandBar().handleMappingError(e, true) == false )
								return "canceled";
					}

					if ( toLocus != null )
					{
						log.info("mapped into " + toLocus);
						
						return toLocus.toString();
					}
				}
				else
					return locus;
			}
		}
		
		return lastLocusForGenome.get(genome);
	}
	
	public String tabsGenomeTabLocus(Genome genome, String locus) 
	{
		lastLocusGenome = genome;
		
		return lastLocusForGenome.put(genome, locus);
	}
	
	public int tabsGenomeTabIndex(Genome genome) 
	{
		if ( tabbedPane == null )
			return -1;
		
		// give precedence to current tab
		MainPanel		mainPanel = (MainPanel)tabbedPane.getSelectedComponent();
		if ( mainPanel.getGenome() == genome )
			return tabbedPane.getSelectedIndex();
		
		// next try and use cache
		mainPanel = lastMainPanelForGenome.get(genome);
		if ( mainPanel != null )
		{
			if ( mainPanel.getGenome() == genome )
			{
				int			index = tabsMainPanelIndex(mainPanel);
				if ( index >= 0 )
					return index;
			}
		}
		
		// loop over tabs
		for ( int index = 0; index < tabbedPane.getComponentCount() ; index++ )
		{
			mainPanel = (MainPanel)tabbedPane.getComponentAt(index);
			
			if ( mainPanel.getGenome() == genome)
				return index;
		}
		
		return -1;
	}

	public void tabsSetShowGenome(boolean show) 
	{
		PreferenceManager.getInstance().put("tabsShowGenome", show);
    	tabsUpdateShowGenome();
	}
	
	public boolean tabsIsMultiGenome()
	{
		Collection<MainPanel>		mainPanels = tabsMapMainPanels().values();
		
		if ( mainPanels.size() < 2 )
			return false;
		else
		{
			Genome		genome = null;
			
			for ( MainPanel mp : mainPanels )
				if ( mp.getGenome() != null )
				{
					if ( genome == null )
						genome = mp.getGenome();
					else if ( genome != mp.getGenome() )
						return true;
				}
			
			return false;
		}
	}
	
	public boolean tabsHasPhysicalGenome()
	{
		for ( MainPanel mp : tabsMapMainPanels().values() )
			if ( mp.getGenome() != null && !mp.getGenome().isGeneticMap() )
				return true;
		
		return false;
	}
	
	public Genome tabsFirstPhysicalGenome()
	{
		for ( MainPanel mp : tabsMapMainPanels().values() )
			if ( mp.getGenome() != null && !mp.getGenome().isGeneticMap() )
				return mp.getGenome();
		
		return null;
	}

	public Genome tabsFirstNonCurrentPhysicalGenome()
	{
		for ( MainPanel mp : tabsMapMainPanels().values() )
			if ( mainPanel != mp )
				if ( mp.getGenome() != null && !mp.getGenome().isGeneticMap() )
					return mp.getGenome();
		
		return null;
	}

	public boolean tabsShowGenomeOption()
	{
		return showGenomeOption;
	}
	
	public boolean tabsGetShowGenome()
	{
		if ( tabsShowGenomeOption() )
			return PreferenceManager.getInstance().getAsBoolean("tabsShowGenome");
		else
			return false;
	}
	
	public void tabsSetMappingEnabled(boolean show) 
	{
		PreferenceManager.getInstance().put("tabsMappingEnabled", show);
    	tabsUpdateShowGenome();
	}
	
	public boolean tabsGetMappingEnabled()
	{
		return PreferenceManager.getInstance().getAsBoolean("tabsMappingEnabled");
	}
	
	public void tabsSetMappingFailurePopup(boolean show) 
	{
		PreferenceManager.getInstance().put("tabsMappingFailurePopup", show);
    	tabsUpdateShowGenome();
	}
	
	public boolean tabsGetMappingFailurePopup()
	{
		return PreferenceManager.getInstance().getAsBoolean("tabsMappingFailurePopup");
	}
	
	public void tabsUpdateShowGenome()
	{
		for ( int index = 0; index < tabbedPane.getComponentCount() ; index++ )
		{
			MainPanel		mainPanel = (MainPanel)tabbedPane.getComponentAt(index);
			
			tabbedPane.setTitleAt(index, tabsCalcTabTitle(mainPanel));
			tabbedPane.setToolTipTextAt(index, tabsTabTooltip(mainPanel));
		}		
	}

	private String tabsCalcTabTitle(MainPanel mainPanel)
	{
		boolean		show = tabsGetShowGenome() || tabsIsMultiGenome();

		String		title = mainPanel.getTabName();
		if ( show && mainPanel.getGenome() != null )
			title += "  [" + mainPanel.getGenome().getExtendedDisplayName() + "]";
		
		return title;
	}
	
	private void tabsRealizeCurrentGenome() 
	{
		MainPanel		mainPanel = (MainPanel)tabbedPane.getSelectedComponent();
		Genome			genome = mainPanel.getGenome();
		
		if ( genome != null )
			lastMainPanelForGenome.put(genome, mainPanel);
		
		if ( genome != null && genome != IGV.getInstance().getGenomeManager().currentGenome )
		{
			String			locus = getCommandBar().getSearchTextField().getText();
			lastLocusForGenome.put(IGV.getInstance().getGenomeManager().currentGenome, locus);
			
			getCommandBar().setCurrentGenome(mainPanel.getGenome(), true);
		}
		
		// on non-genomes, make sure we have a sequence track
		if ( genome != null && !genome.isGeneticMap() )
		{
	        Track sequenceTrack = trackManager.getSequenceTrack();
	        Track geneTrack = trackManager.getGeneTrack();

	        if (PreferenceManager.getInstance().getAsBoolean(PreferenceManager.SHOW_SINGLE_TRACK_PANE_KEY)) {
	            if (sequenceTrack != null) {
	                mainPanel.getDataTrackScrollPane().getTrackPanel().addTrack(sequenceTrack);
	            }
	            if (geneTrack != null) {
	            	mainPanel.getDataTrackScrollPane().getTrackPanel().addTrack(geneTrack);
	            }
	        } else {
	            if (sequenceTrack != null) {
	                mainPanel.getFeatureTrackScrollPane().getTrackPanel().addTrack(sequenceTrack);
	            }
	            if (geneTrack != null) {
	                mainPanel.getFeatureTrackScrollPane().getTrackPanel().addTrack(geneTrack);
	            }
	        }
	        
	        IGV.getInstance().doRefresh();
		}
	}
	
	private String tabsGetTitleAt(int index)
	{
		return ((MainPanel)tabbedPane.getComponentAt(index)).getTabName();
	}

	public MainPanel tabsMainPanelAt(int index) 
	{
		if ( index < tabbedPane.getComponentCount() )
			return (MainPanel)tabbedPane.getComponentAt(index);
		else
			return null;
	}

	public boolean isSuspendMapping() {
		return suspendMapping != 0;
	}

	public void setSuspendMapping(boolean suspendMapping) {
		if ( suspendMapping )
			this.suspendMapping++;
		else
			this.suspendMapping--;
	}

	public int tabsMainPanelIndex(MainPanel mainPanel)
	{
		for ( int index = 0; index < tabbedPane.getComponentCount() ; index++ )
			if ( mainPanel.equals((MainPanel)tabbedPane.getComponentAt(index)) )
				return index;
		
		return -1;
	}
	
	public void tabsDumpState(String message)
	{
		log.info("dumping: " + message);
		
		int				selected = tabbedPane.getSelectedIndex();
		for ( int index = 0; index < tabbedPane.getComponentCount() ; index++ )
		{
			MainPanel		mainPanel = (MainPanel)tabbedPane.getComponentAt(index);
			
			log.info("[" + index + "]" + (index == selected ? " **selected**" : ""));
			log.info("  mainPanel: " + mainPanel);
			log.info("  mainPanel.genome: " + mainPanel.getGenome());
			log.info("  mainPanel.tabName: " + mainPanel.getTabName());
			log.info("  title: " + tabbedPane.getTitleAt(index));
		}		
	}

	public void tabsReorder(int[] newIndex) 
	{
		tabsDumpState("before order");
		
		// must be of same size
		if ( tabbedPane.getComponentCount() != newIndex.length )
		{
			log.info("can't reorder because sizes are different. tabbedPane.getComponentCount()=" + tabbedPane.getComponentCount() 
							+ ", newIndex.length=" + newIndex.length);
			return;
		}
		
		// verify indexes are valid
		int				size = newIndex.length;
		for ( int n = 0 ; n < size ; n++ )
			if ( newIndex[n] < 0 || newIndex[n] >= newIndex.length )
			{
				log.info("can't reorder because value of " + newIndex[n] + " of newIndex[" + n + "] is out of bounds");
				return;
			}
		
		
		// extract info
		MainPanel[]		mainPanels = new MainPanel[size];
		String[]		titles = new String[size];
		String[]		tooltips = new String[size];
		for ( int n = 0 ; n < size ; n++ )
		{
			mainPanels[n] = (MainPanel)tabbedPane.getComponentAt(n);
			titles[n] = tabbedPane.getTitleAt(n);
			tooltips[n] = tabbedPane.getToolTipTextAt(n);
		}
		
		// install new components as a temp
		for ( int n = 0 ; n < size ; n++ )
			tabbedPane.setComponentAt(n, new JPanel());
		
		// install new order
		for ( int n = 0 ; n < size ; n++ )
		{
			tabbedPane.setComponentAt(n, mainPanels[newIndex[n]]);
			tabbedPane.setTitleAt(n, titles[newIndex[n]]);
			tabbedPane.setToolTipTextAt(n, tooltips[newIndex[n]]);
		}
		
		// reorder selection
		tabbedPane.setSelectedIndex(newIndex[tabbedPane.getSelectedIndex()]);
		
		tabsDumpState("after order");
	}

	public void tabsResetSingleGenome() 
	{
		if ( tabbedPane != null )
			if ( tabbedPane.getComponentCount() == 1 )
			{
				MainPanel			mainPanel = (MainPanel)tabbedPane.getComponentAt(0);
	
				if ( mainPanel.getTrackManager().getAllTracks(false).size() == 0 )
				{
					mainPanel.setGenome(null);
				}
			}
	}

	public void tabsClearAllCaches() 
	{
	    lastMainPanelForGenome.clear();
	    lastLocusForGenome.clear();
	    lastMappings.clear();
	    lastLocusGenome = null;
	}

	public void tabsCloseEmptyTabs() 
	{
		
	}

	/*
	private boolean tabsCheckCanSwitchTo(int selectedIndex) 
	{
		try
		{
			// establish genomes
			Genome				fromGenome = ((MainPanel)tabbedPane.getComponent(lastTabsSwitchToIndex)).getGenome();
			Genome				toGenome = ((MainPanel)tabbedPane.getComponent(selectedIndex)).getGenome();
			if ( fromGenome == null || toGenome == null || fromGenome.getId().equals(toGenome.getId()) )
				return true;
			
			// check for mapping
			String				loc = tabsGenomeTabLocus(toGenome);
			if ( loc == null )
				return false;
			
		} catch (Throwable e)
		{
		}
		
		return true;		
	}
	*/

	public void tabsRestoreToLast() 
	{
		if ( lastTabsSwitchToHistory.size() >= 2 )
		{
			int			switchTo = -1;
			int			top = lastTabsSwitchToHistory.peek();
			
			for ( int n = lastTabsSwitchToHistory.size() - 2 ; n >= 0  ; n-- )
				if ( lastTabsSwitchToHistory.get(n) != top )
				{
					switchTo = lastTabsSwitchToHistory.get(n);
					break;
				}
			
			if ( switchTo >= 0 ) 
			{
				suspendMapping++;
				tabsSwitchTo(switchTo);
				suspendMapping--;
			}
		}
	}

	public void tabsCleanupSequenceFromGM()
	{
		log.debug("cleaning up sequence track");
		
		for ( MainPanel mp : tabsMapMainPanels().values() )
		{
			log.debug("checking " + mp.getTabName() + ", genome " + mp.getGenome());
			if ( mp.getGenome() != null && mp.getGenome().isGeneticMap() )
			{
				for ( TrackPanel trackPanel : mp.getTrackPanels() )
				{
					log.debug("checking trackPanel " + trackPanel.getName());
					
					List<Track> 		removeTracks = new LinkedList<Track>();
					
					for ( Track track : trackPanel.getTracks() )
					{
						log.debug("checking track: " + track.getName() + " of class " + track.getClass());
						if ( track instanceof SequenceTrack )
							removeTracks.add(track);
					}
					
					for ( Track track : removeTracks )
						log.debug("removing track: " + track.getName() + " of class " + track.getClass());
					trackPanel.removeTracks(removeTracks);
				}
			}
		}
	}
	
	public boolean tabsPathInTab(String path, int tabIndex)
	{
		MainPanel		mainPanel = (MainPanel)tabbedPane.getComponentAt(tabIndex);
		TrackManager	trackManager = mainPanel.getTrackManager();

		for ( Track track : trackManager.getAllTracks(true) )
		{
			String			trackPath = null;
			if ( track.getResourceLocator() != null )
				trackPath = track.getResourceLocator().getPath();
			
			//log.info("trackPath: " + trackPath);
			
			if ( StringUtils.equals(path, trackPath) )
				return true;
		}
		
		return false;
	}
	
	public List<String> tabsAllPathsInTab(int tabIndex)
	{
		MainPanel		mainPanel = (MainPanel)tabbedPane.getComponentAt(tabIndex);
		TrackManager	trackManager = mainPanel.getTrackManager();
		List<String>	paths = new LinkedList<String>();

		for ( Track track : trackManager.getAllTracks(true) )
		{
			String			trackPath = null;
			if ( track.getResourceLocator() != null )
				trackPath = track.getResourceLocator().getPath();
			
			if ( trackPath != null )
				paths.add(trackPath);
		}
		
		return paths;
	}
	
	public int tabsPathTabIndex(String path) 
	{
		if ( tabbedPane == null )
			return -1;
		
		// give precedence to current tab
		int				index = tabbedPane.getSelectedIndex();
		if ( tabsPathInTab(path, index) )
			return index;

		// loop over tabs
		for ( index = 0; index < tabbedPane.getComponentCount() ; index++ )
			if ( tabsPathInTab(path, index) )
				return(index);
		
		return -1;
	}

	public Map<String, Set<String>> tabsPathToTabsMap()
	{
		Map<String, Set<String>>		map = new LinkedHashMap<String, Set<String>>();
		
		for ( int tabIndex = 0; tabIndex < tabbedPane.getComponentCount() ; tabIndex++ )
		{
			String		tabName = tabsNameList().get(tabIndex);
			
			for ( String path : tabsAllPathsInTab(tabIndex) )
			{
				Set<String>		tabs = map.get(path);
				if ( tabs == null )
					map.put(path, tabs = new LinkedHashSet<String>());
				
				tabs.add(tabName);
			}
		}
		
		return map;
	}
	
	public String tabsTabName(int tabIndex)
	{
		return tabsList().get(tabIndex);
		
	}
	
}
