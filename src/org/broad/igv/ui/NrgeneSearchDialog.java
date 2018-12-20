package org.broad.igv.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.security.KeyStore;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.broad.igv.PreferenceManager;
import org.broad.igv.feature.FeatureDB;
import org.broad.igv.feature.FeatureDB.SearchResultEntry;
import org.broad.igv.feature.NamedFeature;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.genome.GenomeManager;

@SuppressWarnings("serial")
public class NrgeneSearchDialog extends JDialog {

    private static Logger log = Logger.getLogger(NrgeneSearchDialog.class);
	
	@SuppressWarnings("unused")
	private Frame				parentFrame;
	private JScrollPane			scrollPanel;
	
	private JTable				table;
	
	private JTextField			field;
	private JLabel				status;
	private JCheckBox			showColors;
	
	private Thread				searchThread;
	private String				lastSearchText;
	
	private Thread				timerThread;
	
	private String				summary;
	
	private boolean				addTabNameToToolTip = true;
		
	private final static String[]	columnNames = {
			"Key", "Feature", "Genome", "Tab"	
	};
	private final static int	ENTRY_COLUMN = columnNames.length;
	
	private FeatureDB.SearchResultEntry selectedEntry;
	
	private JButton				ok;
	
	private static String		globallastSearchText;
	
	public NrgeneSearchDialog(Frame parentFrame, String text)
	{
		// super & setup
		super(parentFrame);
		this.parentFrame = parentFrame;
		
		// dialog behavior
		setTitle("?");
		setSize(700, 550);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		// create contents components
		JPanel			topPanel = new JPanel();
		topPanel.setLayout(new BorderLayout());
		getContentPane().add(topPanel);

		field = new JTextField();
		TextPrompt tp7 = new TextPrompt("Enter search term here ...", field);
		tp7.changeAlpha(0.5f);
		Border		border = BorderFactory.createLineBorder(Color.black, 2);
		field.setBorder(border);
		topPanel.add(field, BorderLayout.NORTH);
		if ( globallastSearchText != null )
			field.setText(globallastSearchText);
		
		scrollPanel = new JScrollPane();
		topPanel.add(scrollPanel, BorderLayout.CENTER);

		// status
		status = new JLabel("Ready.");
		status.setSize(500, 25);
		status.setHorizontalAlignment(SwingConstants.CENTER);

		// buttons
		JPanel			toolbar = new JPanel();
		toolbar.setLayout(new BorderLayout());
		
		ok = new JButton("Cancel");
		toolbar.add(status, BorderLayout.CENTER);
		toolbar.add(ok, BorderLayout.EAST);
		ok.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent arg0) {
				go();
			}
		});	
		
		showColors = new JCheckBox("Show Colors");
		toolbar.add(showColors, BorderLayout.WEST);
		showColors.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				PreferenceManager.getInstance().put(PreferenceManager.SEARCH_SHOW_COLORS, showColors.isSelected());
				table.repaint();
			}
		});
		if ( PreferenceManager.getInstance().getAsBoolean(PreferenceManager.SEARCH_SHOW_COLORS) )
			showColors.setSelected(true);
		
		topPanel.add(toolbar, BorderLayout.SOUTH);
		
		// Listen for changes in the text
		field.getDocument().addDocumentListener(new DocumentListener() {
			
			@Override
			public void removeUpdate(DocumentEvent arg0) {
				startUpdateSearch();
			}
			
			@Override
			public void insertUpdate(DocumentEvent arg0) {
				startUpdateSearch();
			}
			
			@Override
			public void changedUpdate(DocumentEvent arg0) {
				startUpdateSearch();
			}
		});
		
		// update status
		status.setText(summary = "Total of " + FeatureDB.getSummary());
		
		// start timer thread
		timerThread = new Thread(new Runnable() {
			
			@Override
			public void run() 
			{
				try {
					while ( true )
					{
						startUpdateSearch();
						Thread.sleep(100);
					}
				} catch (InterruptedException e) 
				{
					// interruption is normal here. this is the way the timer is killed
				}
				
			}
		});
		timerThread.start();
	}	
	
	private synchronized void startUpdateSearch()
	{
		String			searchText = field.getText();
		
		if ( !StringUtils.equals(searchText, lastSearchText) && (searchThread == null) )
				updateSearch(searchText);
	}
	
	private void updateSearch(final String searchText)
	{
		final long			debugSleep = 0;
		
		lastSearchText = searchText;
		searchThread = new Thread(new Runnable() {
			
			@Override
			public void run() 
			{
				try
				{
					int					maxResults = 100;
					
					IGV.getStatusTracker().startActivity("search", searchText);
					final FeatureDB.SearchResult				searchResult = FeatureDB.searchFeatures(globallastSearchText = searchText, maxResults); 
					Thread.sleep(debugSleep);
					IGV.getStatusTracker().finishActivity("search", searchText, searchResult.elapsedMsec);
					
					
					// build search result status
					StringBuilder		searchStatusBuilder = new StringBuilder();
					if ( searchResult.foundEntries.size() == 0 )
						searchStatusBuilder.append(summary);
					else
					{
						searchStatusBuilder.append("Found");
						if ( searchResult.partialResult )
							searchStatusBuilder.append(" at least");
						searchStatusBuilder.append(" ");
						searchStatusBuilder.append(searchResult.foundEntries.size());
						searchStatusBuilder.append(" " + FeatureDB.plural("feature", searchResult.foundEntries.size()) + " in ");
						searchStatusBuilder.append(searchResult.visitedGenomeCount);
						searchStatusBuilder.append(" " + FeatureDB.plural("genome", searchResult.visitedGenomeCount));
						
						// add time
						searchStatusBuilder.append(" (");
						searchStatusBuilder.append(searchResult.elapsedMsec);
						searchStatusBuilder.append("ms)");
					}
					final String		searchStatus = searchStatusBuilder.toString();
					
					final Object[][]	data = new Object[Math.min(maxResults, searchResult.foundEntries.size())][];
					int					index = 0;
					for ( FeatureDB.SearchResultEntry entry : searchResult.foundEntries )
					{				
						Object[]		row = 
										{
											entry.key, 
											FeatureDB.namedFeatureAsText(entry.feature, true), 
											entry.genomeId,
											!StringUtils.isEmpty(entry.tabName) ? entry.tabName : "", 
											entry
										};
						
						data[index++] = row;
						
						maxResults--;
						if ( maxResults <= 0 )
							break;
					}
					ok.setText("Cancel");
							
					SwingUtilities.invokeLater(new Runnable() {
						
						@Override
						public void run() 
						{
							if ( table != null )
								table.remove(scrollPanel.getViewport());
							selectedEntry = null;
							table = new JTable(data, columnNames) {
								@Override
								public Component prepareRenderer(TableCellRenderer renderer, int row, int column)
							    {
							        Component c = super.prepareRenderer(renderer, row, column);
							        
							        if ( showColors.isSelected() )
							        {
										FeatureDB.SearchResultEntry entry = (FeatureDB.SearchResultEntry)table.getModel().getValueAt(row, ENTRY_COLUMN);
										if (!isRowSelected(row) )
										{
											if ( entry.color != null )
												c.setBackground(entry.color);
											else
												c.setBackground(Color.white);
										}
							        }
							        else if ( !isRowSelected(row) )
										c.setBackground(Color.white);

									/*
							        if (!isRowSelected(row))
										c.setBackground(row % 2 == 0 ? getBackground() : Color.LIGHT_GRAY);
									*/
							        
							        return c;
							    }								
								@Override
								public boolean isCellEditable(int row, int column) {                
					                return false;               
								};
								@Override
								public String getToolTipText(MouseEvent e) {
					                String 			tip = null;
					                java.awt.Point 	p = e.getPoint();
					                int 			row = rowAtPoint(p);
									if ( row >= 0 )
									{
										FeatureDB.SearchResultEntry entry = (FeatureDB.SearchResultEntry)table.getModel().getValueAt(row, ENTRY_COLUMN);
										
										String		path = entry.feature.getPath();
										String		tabName = entry.tabName;
										if ( path != null )
										{
											tip = path;
											if ( addTabNameToToolTip && tabName != null )
												tip = tabName + ": " + tip;
										}
									}
									
									return tip;

								}
							};
							table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
														
							table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "Enter");
						    table.getActionMap().put("Enter", new AbstractAction() {
								
								@Override
								public void actionPerformed(ActionEvent arg0) {
									go();
								}
							});
							
	                        TableColumnModel cm = table.getColumnModel();
							table.setFillsViewportHeight(true);
	                        cm.getColumn(0).setPreferredWidth(180);
	                        cm.getColumn(1).setPreferredWidth(270);
	                        cm.getColumn(2).setPreferredWidth(170);
							scrollPanel.getViewport().add(table);
							
							status.setText(" " + searchStatus);
							
							ListSelectionModel sm = table.getSelectionModel();
							sm.addListSelectionListener(new ListSelectionListener() {
								
								@Override
								public void valueChanged(ListSelectionEvent arg0) 
								{	
									int			row = table.getSelectedRow();
									
									selectedEntry = null;
									if ( row >= 0 )
									{
										selectedEntry = (FeatureDB.SearchResultEntry)table.getModel().getValueAt(row, ENTRY_COLUMN);
									}
									
									ok.setText((selectedEntry == null) ? "Cancel" : "Go");
									
								}
							});
						}
					});
				} catch (Throwable e) {
					e.printStackTrace();
				} finally {
					searchThread = null;
				}
			}
		});
		searchThread.start();
	}
	
	protected void gotoEntry(final SearchResultEntry entry) 
	{
		SwingUtilities.invokeLater(new Runnable() {
			
			@Override
			public void run()
			{
				NamedFeature		feature = entry.feature;
				String				locus = feature.getChr() + ":" + Math.max(1, feature.getStart()) + "-" + Math.max(1,feature.getEnd());
				String				path = feature.getPath();
				int					targetTab = -1;
				
				log.info("locus: " + locus);
				log.info("path: " + path);
				log.info("genomeId: " + entry.genomeId);
				
				if ( entry.tabName != null )
					targetTab = IGV.getInstance().getContentPane().tabsNameList().indexOf(entry.tabName);
				
				// if has path, find tab in which the path is located
				if ( targetTab < 0 && path != null )
				{
					targetTab = IGV.getInstance().getContentPane().tabsPathTabIndex(path);
					
					log.info("targetTab (by path): " + targetTab);
				}
				
				// if no tab yet, try to find by genome
				Genome			genome = IGV.getInstance().getGenomeManager().getCachedGenomeById(entry.genomeId);
				if ( targetTab < 0 )
				{
					targetTab = IGV.getInstance().getContentPane().tabsGenomeTabIndex(genome);

					log.info("targetTab (by genome): " + targetTab);
				}
				
				// switch to target tab?
				try {
					if ( targetTab >= 0 )
					{
						if ( IGV.getInstance().getContentPane().tabsCurrentTabIndex() != targetTab )
						{
							IGV.getInstance().getContentPane().getCommandBar().setRegionGenomeSwitch(true);
							if ( locus != null )								
							{
								IGV.getInstance().getContentPane().getCommandBar().setRegionGenomeSwitchLocus(locus);
								locus = null;
							}
							log.info("switching to tabIndex: " + targetTab);
							IGV.getInstance().getContentPane().tabsSwitchTo(targetTab);
						}
					}
					
					// if here, simply switch to locus (hopefully on target tab)
					if ( locus != null )
					{
						log.info("goto locus: " + locus);
						IGV.getInstance().goToLocus(locus);
					}
				} finally {
				}
			}
		});
	}

	private void go()
	{
		setVisible(false);
		timerThread.interrupt();
		
		if ( selectedEntry != null )
		{
			gotoEntry(selectedEntry);
		}
	}
}