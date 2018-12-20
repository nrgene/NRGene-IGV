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
package org.broad.igv.feature;

//~--- non-JDK imports --------------------------------------------------------

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.broad.igv.Globals;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.ui.IGV;

/**
 * This is a placeholder class for a true "feature database" wrapper.  Its purpose
 * is to return a feature given a name.  Used to support the "search" box.
 *
 * @author jrobinso
 */
public class FeatureDB {
	
	public static class SearchResult
	{
		public List<SearchResultEntry>		foundEntries;

		public long							elapsedMsec;
		public int							visitedGenomeCount;
		public boolean						partialResult;
	}
	
	public static class SearchResultEntry implements Comparable<SearchResultEntry>
	{
		public int				score;
		public String			key;
		public NamedFeature		feature;
		public String			genomeId;
		public String			tabName;
		
		public int				matchStart;
		public int				matchLength;
		
		public Color			color;
		
		@Override
		public String toString() 
		{
			return key + " " + namedFeatureAsText(feature, true) + " (" + score + ", " + genomeId + ")";
		}

		@Override
		public int compareTo(SearchResultEntry o) 
		{	
			// genome
			int		diff = genomeId.compareTo(o.genomeId);
			if ( diff != 0 )
				return diff;
			
			// chromosome
			String		chr0 = feature.getChr();
			String		chr1 = o.feature.getChr();
			if ( chr0 != null && chr1 != null )
			{
				if ( (diff = chr0.compareTo(chr1)) != 0 )
					return diff;
			}
			else if ( chr0 != null )
				return 1;
			else if ( chr1 != null )
				return -1;
			
			// start/end address
			if ( (diff = feature.getStart() - o.feature.getStart()) != 0 )
				return diff;
			if ( (diff = feature.getEnd() - o.feature.getEnd()) != 0 )
				return diff;
			
			// key
			diff = key.compareTo(o.key);
			if ( diff != 0 )
				return diff;
			
			// name
			String		name0 = feature.getName();
			String		name1 = o.feature.getName();
			if ( name0 != null && name1 != null )
			{
				if ( (diff = name0.compareTo(name1)) != 0 )
					return diff;
			}
			else if ( name0 != null )
				return 1;
			else if ( name1 != null )
				return -1;
			
			// tab
			diff = tabName.compareTo(o.tabName);
			if ( diff != 0 )
				return diff;
			
			// path
			String		path0 = feature.getPath();
			String		path1 = o.feature.getPath();
			if ( path0 != null && path1 != null )
			{
				if ( (diff = path0.compareTo(path1)) != 0 )
					return diff;
			}
			else if ( path0 != null )
				return 1;
			else if ( path1 != null )
				return -1;
			
			
			// score
			diff = score - o.score;
			if ( diff != 0 )
				return diff;

			// if here, same
			return 0;
		}
	}

    private static Logger log = Logger.getLogger(FeatureDB.class);
    /**
     * Map for all features other than genes.
     * 
     * Key1 - genomeId
     * Key2 - featureName
     * Key3 - path
     */
    private static Map<String, Map<String, Map<String, NamedFeature>>> multiGenomefeatureMap = Collections.synchronizedMap(new HashMap(5));

    private static int addByAttributesLengthLimit = -1;
    
    public static void addFeature(NamedFeature feature, Genome genome) {

        if (Globals.isHeadless()) {
            return;
        }

        final String name = feature.getName();
        if (name != null && name.length() > 0) {
            put(name, feature, genome);
        }
        if (feature instanceof IGVFeature) {
            final IGVFeature igvFeature = (IGVFeature) feature;
            final String id = igvFeature.getIdentifier();
            if (id != null && id.length() > 0) {
                put(id, feature, genome);
            }

            addByAttributes(igvFeature, genome);

            List<Exon> exons = igvFeature.getExons();
            if(exons != null) {
                for(Exon exon : exons) {
                    addByAttributes(exon, genome);
                }
            }
        }
    }

    private static void addByAttributes(IGVFeature igvFeature, Genome genome) {
    	if ( addByAttributesLengthLimit > 0 )
    	{
	        Map<String, String> attributes = igvFeature.getAttributes();
	        if (attributes != null) {
	            for (Map.Entry<String, String> entry : attributes.entrySet() ) {
	                if (entry.getValue().length() < 20) {
	                    put(entry.getKey() + "=" + entry.getValue(), igvFeature, genome);
	                }
	            }
	        }
    	}
    }

    public static void put(String name, NamedFeature feature, Genome genome) {
    	
        String key = name.toUpperCase();
        String path = feature.getPath();
        
        Map<String, Map<String, NamedFeature>>		featureMap = getGenomeFeatureMap(genome);
        boolean										insertAll = true;

        synchronized (genome)
        {        
	        Genome currentGenome = genome;
	        if (currentGenome == null || (insertAll || currentGenome.getChromosome(feature.getChr()) != null)) {
	        	addFeatureToMap(featureMap, key, feature);
	        }
        }
    }


    public static void addFeature(String name, NamedFeature feature, Genome genome) {
        if (Globals.isHeadless()) {
            return;
        }
        Map<String, Map<String, NamedFeature>>		featureMap = getGenomeFeatureMap(genome);
 
        addFeatureToMap(featureMap, name, feature);
    }


    private static void addFeatureToMap(Map<String, Map<String, NamedFeature>> featureMap, String name, NamedFeature feature) 
    {
    	name = name.toUpperCase();
		String		path = feature.getPath();
    	
    	synchronized (featureMap) 
    	{
    		Map<String, NamedFeature>		features = featureMap.get(name);
    		if ( features == null )
    			featureMap.put(name, features = new LinkedHashMap<String, NamedFeature>());
    		
    		features.put(path, feature);
    	}
	}

	private FeatureDB() {
        // This class can't be instantiated
    }


    public static void addFeatures(List<org.broad.tribble.Feature> features, Genome genome) {
        for (org.broad.tribble.Feature feature : features) {
            if (feature instanceof IGVFeature)
                addFeature((IGVFeature) feature, genome);
        }
    }


    public static void clearFeatures() {
    	multiGenomefeatureMap.clear();
    }

    public static void clearGenomeFeatures(Genome genome) {
    	
        Map<String, Map<String, NamedFeature>>		featureMap = getGenomeFeatureMap(IGV.getInstance().getGenomeManager().currentGenome);
        List<String>								removeKeys = new LinkedList<String>();
        
        for ( Map.Entry<String, Map<String, NamedFeature>> entry : featureMap.entrySet() )
        {
        	for ( Map.Entry<String, NamedFeature> entry2 : entry.getValue().entrySet() )
        		if ( entry2.getValue() instanceof Cytoband )
        		{
        			removeKeys.add(entry.getKey());
        			break;
        		}
        }
        
        for ( String key : removeKeys )
        	featureMap.remove(key);
    }

    /**
     * Return the feature, if any, with the given name.  Genes are given
     * precedence.
     */
    public static NamedFeature getFeature(String nm) {

        String 										name = nm.trim().toUpperCase();
        Map<String, Map<String, NamedFeature>>		featureMap = getGenomeFeatureMap(IGV.getInstance().getGenomeManager().currentGenome);

        Map<String, NamedFeature>					features = featureMap.get(name);
        
        return getSingleFeature(features);
    }
    
    private static NamedFeature getSingleFeature(Map<String, NamedFeature> features) 
    {
    	if ( features == null || features.size() == 0 )
    		return null;
    	else
    		return features.values().iterator().next();
	}

	private static synchronized Map<String, Map<String, NamedFeature>> getGenomeFeatureMap(Genome genome)
    {
    	 Map<String, Map<String, NamedFeature>>		featureMap = multiGenomefeatureMap.get(genome.getId());
    	 
    	 if ( featureMap == null )
    	 {
    		 featureMap = Collections.synchronizedMap(new HashMap(10000));
    		 multiGenomefeatureMap.put(genome.getId(), featureMap);
    	 }
    	 
    	 return featureMap;
    }
    
	public static String getSummary() 
	{
		int			genomeCount = 0;
		int			featureCount = 0;
		
		for ( String genomeId : multiGenomefeatureMap.keySet() )
		{
			genomeCount++;
			featureCount += multiGenomefeatureMap.get(genomeId).size();
		}
		
		return "" + featureCount + " " + plural("feature", featureCount) + " in " + genomeCount + " " + plural("genome", genomeCount);
	}
    
    public static SearchResult searchFeatures(String searchTerm, int maxResults) 
    {
    	SearchResult		sr = new SearchResult();
    	
    	// adjust search term
    	searchTerm = searchTerm.trim().toUpperCase();
    	int		searchTermLength = searchTerm.length();
    	
    	// start with an empty list
    	sr.foundEntries = new Vector<FeatureDB.SearchResultEntry>();
    	if ( searchTermLength == 0 )
    	{
    		sr.visitedGenomeCount = multiGenomefeatureMap.size();
    		return sr;
    	}
    	
    	// map all files to tabs
    	Map<String, Set<String>>	pathToTabsMap = IGV.getInstance().getContentPane().tabsPathToTabsMap(); 
    	
    	// collect matches from all genomes
    	long		startedAt = System.currentTimeMillis();
    	boolean		matchAll = searchTerm.equals("*");
		for ( String genomeId : multiGenomefeatureMap.keySet() )
		{
			int							count = 0;
			Map<String, Map<String, NamedFeature>>	map = multiGenomefeatureMap.get(genomeId);
			boolean						foundInGenome = false;
						
			for ( Map.Entry<String, Map<String, NamedFeature>> topMapEntry : map.entrySet() )
				for ( Map.Entry<String, NamedFeature> lowerMapEntry : topMapEntry.getValue().entrySet() )
				{
					String			key = topMapEntry.getKey();
					int				index, score = 0, matchLength = 0;
					String			matchKey = null;
					
					// contains?
					if ( matchAll )
					{
						index = 0;
						score = 0;
						matchLength = key.length();
						matchKey = key;
					}
					else
					{
						index = key.indexOf(searchTerm);
						if ( index >= 0 )
						{
							// establish score (low is better, 0 is exact match)
							score = key.length() - searchTermLength;
							matchLength = searchTermLength;
							matchKey = key;
						} 
						else if ( lowerMapEntry.getValue() instanceof IGVFeature )
						{
							// try to match on an attribute
							Map<String, String>		attrs = ((IGVFeature)lowerMapEntry.getValue()).getAttributes();
							if ( attrs != null )
								for ( Map.Entry<String, String> entry : attrs.entrySet() )
								{
									String		text = entry.getKey() + "=" + entry.getValue();
									
									index = text.toUpperCase().indexOf(searchTerm);
									if ( index >= 0 )
									{
										score = text.length() - searchTermLength;
										matchLength = searchTermLength;
										matchKey = text;									
									}
								}
							
						}
					}
					if ( matchKey == null )
						continue;
					
					
					// add entries
					String				path = lowerMapEntry.getValue().getPath();
					Set<String>			tabNames;
					if ( path != null )
						tabNames = pathToTabsMap.get(path);
					else
					{
						tabNames = new LinkedHashSet<String>();
						tabNames.add(null);
					}
					if ( tabNames != null )
						for ( String tabName : tabNames )
						{
							SearchResultEntry	entry = new SearchResultEntry();
							entry.score = score;
							entry.key = matchKey;
							entry.feature = lowerMapEntry.getValue();
							entry.genomeId = genomeId;
							entry.matchStart = index;
							entry.matchLength = matchLength;
							entry.tabName = tabName;
							
							// color?
							if ( entry.feature instanceof BasicFeature )
								entry.color = ((BasicFeature)entry.feature).getColor();
							
							sr.foundEntries.add(entry);
							count++;
							foundInGenome = true;
						}
					
					// stop on this genome?
					if ( count >= maxResults )
					{
						sr.partialResult = true;
						break;
					}
				}
				
				if ( foundInGenome )
					sr.visitedGenomeCount++;
			}
    	
    	
    	// sort vector 
    	Collections.sort(sr.foundEntries);
    	
    	// return it (may contain addition items beyond maxResult)
    	sr.elapsedMsec = System.currentTimeMillis() - startedAt;
    	return sr;
    }

	public static String namedFeatureAsText(NamedFeature feature, boolean addName)
	{
		String		s = feature.getChr() + ":" + feature.getStart() + "-" + feature.getEnd();
		
		if ( addName && !StringUtils.isEmpty(feature.getName()) )
			s += " (" + feature.getName() + ")";
		
		return s;
	}
	
	public static String plural(String s, int count)
	{
		if ( count == 1 )
			return s;
		else
			return s + "s";
	}

}
