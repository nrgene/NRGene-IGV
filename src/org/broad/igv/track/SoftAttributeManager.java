package org.broad.igv.track;

import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.broad.igv.batch.CommandListener;
import org.broad.igv.ui.panel.FrameManager;

public class SoftAttributeManager {
	
	static private SoftAttributeManager			singleton;
	static private String						GLOBAL_PROPS_FILENAME = "NrgeneSoftAttributes.properties";
	static private String						USER_PROPS_FILENAME = "~/igv/NrgeneSoftAttributes.properties";
	
	static private int							CACHE_PRUNE_THRESHOLD = 1000;
	static private int							CACHE_PRUNE_LEAVE = 100;
	
    static private Logger 						log = Logger.getLogger(SoftAttributeManager.class);
    
    private Map<String, SoftAttributeConf>		attrConfs = new LinkedHashMap<String, SoftAttributeConf>();
    private Map<String, SoftAttributeCachedValue> attrCache = new LinkedHashMap<String, SoftAttributeCachedValue>();
	
	static synchronized SoftAttributeManager getInstance()
	{
		if ( singleton == null )
			singleton = new SoftAttributeManager();
		
		return singleton;
	}
	
	public synchronized void load()
	{
		// clear
		attrConfs.clear();
		
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
	
			// load the attribute - the only properties that have no . (dot) in their name 
			Enumeration<?>			names = conf.propertyNames();
			int						attrConfCount = 0;
			while ( names.hasMoreElements() )
			{
				String 		confKey = (String)names.nextElement();
				if ( confKey.indexOf('.') >= 0 )
					continue;
				
				log.info("loading confKey: " + confKey);
				
				SoftAttributeConf	attrConf = new SoftAttributeConf(conf, confKey);
				
				attrConfs.put(attrConf.getName().toUpperCase(), attrConf);
				attrConfCount++;
			}

			log.info("loaded soft attributes from: " + file + ", attrConfCount: " + attrConfCount);
		}
		catch (Exception e)
		{
			log.debug("exception on " + path + ": " + searchOnClassPath + "/" + searchOnMultiUsername, e);
		}
	}

	public synchronized String getAttribute(AbstractTrack track, String attrName)
	{
		if ( track == null || track.getResourceLocator() == null || track.getResourceLocator().getPath() == null )
			return null;
		
		String						trackKey = track.getResourceLocator().getPath();
		
		SoftAttributeConf			attrConf = attrConfs.get(attrName.toUpperCase());
		if ( attrConf == null )
			return null;
		
		// get locus
		String locus = "";
		if ( attrConf.isLocusDependant() )
			locus = FrameManager.getDefaultFrame().getFormattedLocusString();		

		// find out if not already in cache
		SoftAttributeCachedValue	cv = new SoftAttributeCachedValue(attrConf, trackKey, locus);
		String						key = cv.getKey();
		if ( attrCache.containsKey(key) )
		{
			SoftAttributeCachedValue	cv1 = attrCache.get(key);
			
			cv1.setDate(new Date());
			
			log.debug("[" + attrCache.size() + "] cache hit for: " + cv1.getKey());
			
			return cv1.getValue();
		}
		
		// prune?
		if ( attrCache.size() >= CACHE_PRUNE_THRESHOLD )
			pruneCache();
		
		// calculate and enter into cache
		cv.setValue(attrConf.getAttribute(track, attrName, locus));
		attrCache.put(key, cv);

		log.debug("[" + attrCache.size() + "] cache added for: " + cv.getKey());
		
		return cv.getValue();
	}
	
	public synchronized void pruneCache()
	{
		List<SoftAttributeCachedValue>		fixed = new LinkedList<SoftAttributeCachedValue>();
		List<SoftAttributeCachedValue>		locus = new LinkedList<SoftAttributeCachedValue>();
		int									size = attrCache.size();
		
		// collect into lists
		for ( SoftAttributeCachedValue cv : attrCache.values() )
			(cv.getConf().isLocusDependant() ? locus : fixed).add(cv);
		
		// sort local list (fresh entries first)
		Collections.sort(locus, new Comparator<SoftAttributeCachedValue>() {

			@Override
			public int compare(SoftAttributeCachedValue o1, SoftAttributeCachedValue o2) {
				return -o1.getDate().compareTo(o2.getDate());
			}
		});
		
		// restore cache
		attrCache.clear();
		for ( SoftAttributeCachedValue cv : fixed )
			attrCache.put(cv.getKey(), cv);
		int			count = 0;
		for ( SoftAttributeCachedValue cv : locus )
			if ( count++ >= CACHE_PRUNE_LEAVE )
				break;
			else
				attrCache.put(cv.getKey(), cv);
		
		log.debug("pruned from " + size + " to " + attrCache.size() + " enties");
	}


	public Collection<? extends String> getAttributeNames(boolean onlyVisible) 
	{
		List<String>		names = new LinkedList<String>();
		
		for ( SoftAttributeConf attrConf : attrConfs.values() )
			if ( !onlyVisible || attrConf.isVisible() )
				names.add(attrConf.getName());
		
		return names;
	}
}
