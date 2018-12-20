package org.broad.igv.track;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import org.apache.log4j.Logger;
import org.broad.igv.batch.CommandListener;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.panel.FrameManager;
import org.broad.igv.util.ResourceLocator;

public class ExternalToolManager {
	
	static private ExternalToolManager			singleton;
	static private String						GLOBAL_PROPS_FILENAME = "NrgeneExternalTools.properties";
	static private String						USER_PROPS_FILENAME = "~/igv/NrgeneExternalTools.properties";
	
    static private Logger 						log = Logger.getLogger(ExternalToolManager.class);
    
    private Map<String, ExternalToolConf>		toolConfs = new LinkedHashMap<String, ExternalToolConf>();
	
	static public synchronized ExternalToolManager getInstance()
	{
		if ( singleton == null )
		{
			singleton = new ExternalToolManager();
			singleton.load();
		}
		
		return singleton;
	}
	
	public synchronized void load()
	{
		// clear
		toolConfs.clear();
		
		// load files
		loadFile(GLOBAL_PROPS_FILENAME, true, false);
		loadFile(USER_PROPS_FILENAME, false, false);
		loadFile(USER_PROPS_FILENAME, false, true);		
	}

	private void loadFile(String path, boolean searchOnClassPath, boolean searchOnMultiUsername) 
	{
		try
		{
			Properties			conf = new Properties();
			File				file = null;

			if ( searchOnClassPath )
			{
				URL url =  ClassLoader.getSystemResource(path);
				if ( url != null )
					file = new File(url.getFile());
			}
			else
			{
				if ( searchOnMultiUsername )
				{
					String		multiUsername = CommandListener.getListener().multiUsername();
					if ( multiUsername == null )
						return;
					path = path.replace("~", "/home/" + multiUsername + "");
				}
				
				file = new File(path);
			}
			
			if ( file == null || !file.exists() )
			{
				log.debug("skipping " + path + ": " + searchOnClassPath + "/" + searchOnMultiUsername);
				return;
			}
			conf.load(new FileInputStream(file));			
			log.info("loaded external tools from: " + file);
	
			// load the attribute - the only properties that have no . (dot) in their name 
			Enumeration<?>			names = conf.propertyNames();
			while ( names.hasMoreElements() )
			{
				String 		confKey = (String)names.nextElement();
				if ( confKey.indexOf('.') >= 0 )
					continue;
				
				ExternalToolConf	toolConf = new ExternalToolConf(conf, confKey);
				
				toolConfs.put(toolConf.getName(), toolConf);
			}
		}
		catch (Exception e)
		{
			log.debug("exception on " + path + ": " + searchOnClassPath + "/" + searchOnMultiUsername, e);
		}
	}

	public synchronized void invokeTool(final List<AbstractTrack> tracks, final String toolName)
	{
		final ExternalToolConf			toolConf = toolConfs.get(toolName);
		if ( toolConf == null )
			return;
		
		// get locus
		final String 	locus = FrameManager.getDefaultFrame().getFormattedLocusString();		

		// invoke tool
		log.debug("invoking tool: " + toolName + ", on: " + tracks);
		if ( toolConf.isBlocking() )
		{
			String		result = toolConf.invokeTool(tracks, toolName, locus);
			log.debug("tool result: " + result);
			processToolResult(toolConf, result);
		}
		else
		{
			log.debug("tool will run in background");
			Runnable		r = new Runnable() {
				
				@Override
				public void run() {

					String	result = toolConf.invokeTool(tracks, toolName, locus);
					log.debug("tool result: " + result);
					processToolResult(toolConf, result);
				}
			};
			
			Thread			t = new Thread(r);
			t.start();
		}
	}
	
	private void processToolResult(ExternalToolConf toolConf, String result) 
	{
		log.debug("tool " + toolConf.getName() + " result " + result);
		
		if ( toolConf.isReturnsTrack() && result != null && (new File(result)).exists() )
		{
			// add this track to the IGV
			List<ResourceLocator> locators = new ArrayList<ResourceLocator>(1);
			locators.add(new ResourceLocator(result));

            IGV.getFirstInstance().loadTracks(locators);		
        }
	}

	public Collection<? extends String> getExternalToolNames() 
	{
		List<String>		names = new LinkedList<String>();
		
		for ( ExternalToolConf toolConf : toolConfs.values() )
			names.add(toolConf.getName());
		
		Collections.sort(names);
		
		return names;
	}

	public void addToolsToMenu(JPopupMenu menu, final Collection<Track> tracks) 
	{
		Collection<? extends String>		toolNames = getExternalToolNames();
		if ( toolNames.size() <= 0 )
			return;
		
		final List<AbstractTrack>					abstractTracks = new LinkedList<AbstractTrack>();
		for ( Track track : tracks )
			if ( track instanceof AbstractTrack )
				abstractTracks.add((AbstractTrack)track);
		
		final JMenu			submenu = new JMenu("External Tools ...");
		for ( final String toolName : toolNames )
		{
	        JMenuItem item = new JMenuItem(toolName);
	        item.addActionListener(new ActionListener() {

	            public void actionPerformed(ActionEvent evt) {
	                invokeNamedTool(toolName, abstractTracks);
	            }
	        });

	        submenu.add(item);
		}
		
		menu.addSeparator();
		menu.add(submenu);
	}
	
	private void invokeNamedTool(String toolName, List<AbstractTrack> tracks)
	{
		invokeTool(tracks, toolName);
	}
}
