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
 * AttributeManager.java
 *
 * Everything to do with attributes.
 */
package org.broad.igv.track;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.broad.igv.exceptions.DataLoadException;
import org.broad.igv.renderer.AbstractColorScale;
import org.broad.igv.renderer.ContinuousColorScale;
import org.broad.igv.renderer.JetColorScale;
import org.broad.igv.renderer.MonocolorScale;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.panel.AttributeHeaderPanel;
import org.broad.igv.ui.util.MessageUtils;
import org.broad.igv.util.ColorUtilities;
import org.broad.igv.util.FileUtils;
import org.broad.igv.util.ParsingUtils;
import org.broad.igv.util.ResourceLocator;
import org.broad.igv.util.Utilities;
import org.broad.tribble.readers.AsciiLineReader;

/**
 * @author jrobinso
 */
public class AttributeManager_ORG {

    private static Logger log = Logger.getLogger(AttributeManager_ORG.class);
    
    private static boolean allowDups = false;

    private static AttributeManager_ORG singleton;
    final public static String ATTRIBUTES_LOADED_PROPERTY = "ATTRIBUTES_LOADED_PROPERTY";
    final public static String ATTRIBUTES_NARROWED_PROPERTY = "ATTRIBUTES_NARROWED_PROPERTY";

    private PropertyChangeSupport propertyChangeSupport;

    /**
     * The set of currently loaded attribute resource files
     */
    Set<ResourceLocator> loadedResources = new HashSet();


    /**
     * Map of data track identifiers (i.e. "array names") to its
     * attributeMap.   The attributeMap for a track maps attribute name (for
     * example "Cell Type"  to value (for example "ES");
     */
    LinkedHashMap<String, Map<String, String>> attributeMap = new LinkedHashMap();

    /**
     * List of attribute names.  The list
     * is kept so the keys may be fetched in the order they were added.
     */
    LinkedHashMap<String, String> attributeNames = new LinkedHashMap();
    Map<String, String> attributeOrigin = new LinkedHashMap();

    /**
     * Column meta data (column == attributeKey).
     */
    Map<String, ColumnMetaData> columnMetaData = new HashMap();


    /**
     * The complete set of unique attribute values per attribute key.  This is useful in
     * assigning unique colors
     */
    Map<String, Set<String>> uniqueAttributeValues;

    /**
     * Maps symbolic (discrete) attribute values to colors. Key is a composite of attribute name and value
     */
    Map<String, Color> colorMap = new Hashtable();

    /**
     * Map of attribute column name -> color scale.   For numeric columns.
     */
    Map<String, AbstractColorScale> colorScales = new HashMap();

    Map<String, Integer> colorCounter = new HashMap();

	private boolean defaultToJet = true;
	
	private Map<Long,List<String>>	dupErrorsMap = new LinkedHashMap<Long, List<String>>();


    private AttributeManager_ORG() {
        propertyChangeSupport = new PropertyChangeSupport(this);
        uniqueAttributeValues = new HashMap();
        //hiddenAttributes.add("NAME");
        //hiddenAttributes.add("DATA FILE");
        //hiddenAttributes.add("DATA TYPE");
        
        SoftAttributeManager.getInstance().load();

    }

	static synchronized public AttributeManager_ORG getInstance() {

        if (singleton == null) {
            singleton = new AttributeManager_ORG();
        }
        return singleton;
    }

    public synchronized void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public synchronized void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    /**
     * Return the attribute value for the given track (trackName) and key.
     */
    public synchronized String getAttribute(String trackName, String attributeName) {
        Map attributes = attributeMap.get(trackName);
        String key = attributeName.toUpperCase();
        return (attributes == null ? null : (String) attributes.get(key));
    }

    /**
     * Return the list of attribute names (keys) in the order they should
     * be displayed.
     */
    public synchronized List<String> getAttributeNames() {
        
    	List<String> list = new ArrayList(attributeNames.values());
    	
    	list.addAll(SoftAttributeManager.getInstance().getAttributeNames(false));
    	
    	return list;
    }

    /**
     * Return true if the associated column contains all numeric values
     */
    public boolean isNumeric(String attributeName) {
        String key = attributeName.toUpperCase();
        ColumnMetaData metaData = columnMetaData.get(key);
        return metaData != null && metaData.isNumeric();
    }


    // TODO -- don't compute this on the fly every time its called

    public synchronized List<String> getVisibleAttributes() {
    	final Set<String> allKeys = new LinkedHashSet<String>(attributeNames.keySet());
        Set<String> hiddenAttributes = IGV.getInstance().getSession().getHiddenAttributes();
        
        allKeys.addAll(SoftAttributeManager.getInstance().getAttributeNames(true));
        
        if (hiddenAttributes != null) {
        	for ( String hidden : hiddenAttributes )
        		allKeys.remove(hidden.toUpperCase());
        }

        ArrayList<String> visibleAttributes = new ArrayList<String>(allKeys.size());
        for (String key : allKeys) {
        	if ( attributeNames.containsKey(key) )        		
        		visibleAttributes.add(attributeNames.get(key));
        	else
        		visibleAttributes.add(key);
        }
        
        return visibleAttributes;
    }

    public synchronized void clearAllAttributes() {
        attributeMap.clear();
        attributeNames.clear();
        attributeOrigin.clear();
        uniqueAttributeValues.clear();
        columnMetaData.clear();
        //hiddenAttributes.clear();
        loadedResources = new HashSet();
        SoftAttributeManager.getInstance().load();
        AttributeHeaderPanel.reportSort(null, null);
    }

    /**
     * Set the attribute value for the given track or sample id and key.
     */
    public synchronized void addAttribute(String trackIdentifier, String name, String attributeValue, String path) {

        if (attributeValue.equals("")) {
            return;
        }

        String key = name.toUpperCase();
        addAttributeName(name, path);

        Set<String> uniqueSet = uniqueAttributeValues.get(key);
        if (uniqueSet == null) {
            uniqueSet = new HashSet<String>();
            uniqueAttributeValues.put(key, uniqueSet);
        }
        uniqueSet.add(attributeValue);

        Map attributes = attributeMap.get(trackIdentifier);
        if (attributes == null) {
            attributes = new LinkedHashMap();
            attributeMap.put(trackIdentifier, attributes);
        }

        // check for dup
    	if ( !allowDups )
    	{
    		if ( attributes.containsKey(key) )
            	if ( !attributes.get(key).equals(attributeValue) )
	        		addDupErrors(Thread.currentThread().getId(), trackIdentifier + ": " + attributes.get(key) + " != " + attributeValue);
    		
    		// attributeKey = column header, attributeValue = value for header
    		// and track name (trackIdentifier) row intersection
    		attributes.put(key, attributeValue);
    		updateMetaData(key, attributeValue);
    	}
    	else
    	{
    		
    	}
    }

    public synchronized void addAttributeName(String name, String path) {
        String key = name.toUpperCase();
        if (!attributeNames.containsKey(key) && !name.startsWith("#")) {
            attributeNames.put(key, name);
            if ( path != null )
            	attributeOrigin.put(key, path);
        }
    }

    /**
     * Update the column meta data associated with the attribute key.
     * <p/>
     * Note: Currently the meta data only records if the column is numeric.
     *
     * @param attributeName
     * @param attributeValue
     */
    private void updateMetaData(String attributeName, String attributeValue) {

        String key = attributeName.toUpperCase();
        ColumnMetaData metaData = columnMetaData.get(key);
        if (metaData == null) {
            metaData = new ColumnMetaData();
            columnMetaData.put(key, metaData);
        }

        // Test if data is numeric.  Skip null and blank values
        if (attributeValue != null && attributeValue.length() > 0 && metaData.isNumeric()) {
            try {
                double val = Double.parseDouble(attributeValue);
                metaData.updateRange(val);
            } catch (NumberFormatException e) {
                metaData.markNonNumeric();
            }
        }


    }

    /**
     * Test to see if this file could be a sample information file.  Some characteristics are (1) is tab delimited
     * with at least 2 columns,  (2) is ascii,  (3) is not too large
     *
     * @param locator
     * @return
     */
    public static boolean isSampleInfoFile(ResourceLocator locator) throws IOException {


        if (!FileUtils.isTabDelimited(locator, 2)) {
            return false;
        }

        // If the file is "too large"  ask user
        // TODO -- ftp test
        final int oneMB = 1000000;
        long fileLength = ParsingUtils.getContentLength(locator.getPath());
        if (fileLength > oneMB) {
            return MessageUtils.confirm("<html>Cannot determine file type of: " + locator.getPath() +
                    "<br>Is this a sample information file?");
        }


        return true;
    }

    /**
     * Load attributes from an ascii file in "Sample Info" format.
     */
    public void loadSampleInfo(ResourceLocator locator) {
    	
    	clearDupErrors(Thread.currentThread().getId());
    	
        AsciiLineReader reader = null;
        String nextLine = null;
        try {
            reader = ParsingUtils.openAsciiReader(locator);
            nextLine = reader.readLine();
            if (nextLine.toLowerCase().startsWith("#sampletable")) {
                loadSampleTable(reader, nextLine, locator.getPath());
            } else {
                loadOldSampleInfo(reader, nextLine, locator.getPath());
            }
            loadedResources.add(locator);

            //createCurrentAttributeFileString(files);
            IGV.getInstance().getTrackManager().resetOverlayTracks();

            IGV.getInstance().doRefresh();

        } catch (IOException ex) {
            log.error("Error loading attribute file", ex);
            throw new DataLoadException("Error reading attribute file", locator.getPath());
        } finally {
            if (reader != null) {
                reader.close();

            }
            firePropertyChange(this, ATTRIBUTES_LOADED_PROPERTY, null, null);
        }
        
        String		dupErrors = getDupErrors(Thread.currentThread().getId());
        if ( dupErrors != null )
        	MessageUtils.showMessage(dupErrors);        	
    }

    private void loadOldSampleInfo(AsciiLineReader reader, String nextLine, String path) throws IOException {
        // Parse column neadings for attribute names.
        // Columns 1 and 2 are array and sample name (not attributes)
        boolean foundAttributes = false;
        String[] colHeadings = nextLine.split("\t");
        int nLines = 0;
        int lineLimit = 100000;
        while ((nextLine = reader.readLine()) != null) {
            if (nLines++ > lineLimit) {
                break;
            }

            if (nextLine.startsWith("#colors")) {
                parseColors(reader);
                return;
            }

            String[] values = nextLine.split("\t");

            if (values.length >= 2) {
                String arrayName = values[0].trim();
                // Loop through attribute columns
                for (int i = 0; i < colHeadings.length; i++) {
                    String attributeName = colHeadings[i].trim();
                    String attributeValue = (i < values.length ? values[i].trim() : "");
                    addAttribute(arrayName, attributeName, attributeValue, path);
                    foundAttributes = true;
                }
            }
        }


        if (!foundAttributes) {
            throw new DataLoadException("Could not determine file type.  Does file have proper extension? ", path);
        }
    }

    private void parseColors(AsciiLineReader reader) throws IOException {

        String nextLine;
        while ((nextLine = reader.readLine()) != null) {
            try {
                String[] tokens = nextLine.split("\t");
                if (tokens.length >= 3) {
                    String attKey = tokens[0].toUpperCase();
                    if (isNumeric(attKey)) {

                        ColumnMetaData metaData = columnMetaData.get(attKey);
                        String rangeString = tokens[1].trim();
                        float min = (float) metaData.min;
                        float max = (float) metaData.max;
                        if (!rangeString.equals("*") && rangeString.length() > 0) {
                            String[] tmp = rangeString.split(":");
                            if (tmp.length > 1) {
                                try {
                                    min = Float.parseFloat(tmp[0]);
                                    max = Float.parseFloat(tmp[1]);
                                } catch (NumberFormatException e) {
                                    log.error("Error parsing range string: " + rangeString, e);
                                }
                            }
                        }

                        AbstractColorScale scale = null;
                        if (tokens.length == 3 && !rangeString.equals("*") && rangeString.indexOf(':') < 0)
                        {
                            Color color = ColorUtilities.stringToColor(tokens[2]);
                            String key = (attKey + "_" + rangeString).toUpperCase();
                            colorMap.put(key, color);                        	
                        }
                        else if (tokens.length == 3 && tokens[2].equals("jet") ) {
                        	scale = new JetColorScale(min, max);
                        }
                        else if (tokens.length == 3 && tokens[2].equals("jet/log") ) {
                        	scale = new JetColorScale(min, max, true);
                        }
                        else if (tokens.length == 3) {
                            Color baseColor = ColorUtilities.stringToColor(tokens[2]);
                            scale = new MonocolorScale(min, max, baseColor);
                            //colorScales.put(attKey, scale);
                        } else {
                            Color color1 = ColorUtilities.stringToColor(tokens[2]);
                            Color color2 = ColorUtilities.stringToColor(tokens[3]);
                            if (min < 0) {
                                scale = new ContinuousColorScale(min, 0, max, color1, Color.white, color2);
                            } else {
                                scale = new ContinuousColorScale(min, max, color1, color2);
                            }
                        }
                        if ( scale != null )
                        	colorScales.put(attKey, scale);

                    } else {
                        String attValue = tokens[1];
                        Color color = ColorUtilities.stringToColor(tokens[2]);
                        String key = (attKey + "_" + attValue).toUpperCase();
                        colorMap.put(key, color);
                    }
                }
            } catch (Exception e) {
                log.error("Error parsing color line: " + nextLine, e);
            }
        }
    }

    /**
     * Load attributes from an ascii file in "Sample Table" format.  This format expects
     * a sample table section, prefaced by #sampleTable,  followed by a sample mapping
     * section  (track -> sample) prefaced by #sampleMappings
     */
    private void loadSampleTable(AsciiLineReader reader, String nextLine, String path) throws IOException {

        // Parse column neadings for attribute names.
        // Columns 1 and 2 are array and sample name (not attributes)
        nextLine = reader.readLine();
        String[] colHeadings = nextLine.split("\t");

        // Map of sample -> attribute list
        Map<String, List<Attribute>> sampleTable = new HashMap();

        boolean foundAttributes = false;
        int nLines = 0;
        int lineLimit = 100000;
        while ((nextLine = reader.readLine()) != null) {
            if (nLines++ > lineLimit || nextLine.toLowerCase().startsWith("#samplemapping")) {
                break;
            }

            if (nextLine.toLowerCase().startsWith("#colors")) {
                parseColors(reader);
                break;
            }

            String[] values = nextLine.split("\t");

            if (values.length >= 2) {
                String sampleName = values[0].trim();
                // Loop through attribute columns
                List<Attribute> attributes = new ArrayList(colHeadings.length);
                for (int i = 0; i < colHeadings.length; i++) {
                    String attributeName = colHeadings[i].trim();
                    String attributeValue = (i < values.length ? values[i].trim() : "");
                    attributes.add(new Attribute(attributeName, attributeValue));
                    foundAttributes = true;
                }
                sampleTable.put(sampleName, attributes);
            }
        }
        if (!foundAttributes) {
            throw new DataLoadException("Could not determine file type.  Does file have proper extension? ", path);
        }

        if (nextLine.toLowerCase().startsWith("#samplemapping")) {
            while ((nextLine = reader.readLine()) != null) {

                if (nextLine.toLowerCase().startsWith("#colors")) {
                    parseColors(reader);
                    break;
                }

                String[] tokens = nextLine.split("\t");
                if (tokens.length < 2) {
                    continue;
                }
                String array = tokens[0];
                String sample = tokens[1];
                List<Attribute> attributes = sampleTable.get(sample);
                if (attributes != null) {
                    for (Attribute att : attributes) {
                        addAttribute(array, att.getKey(), att.getValue(), path);
                    }
                }
            }
        } else {
            // No mapping section.
            for (Map.Entry<String, List<Attribute>> entry : sampleTable.entrySet()) {
                String sample = entry.getKey();
                for (Attribute att : entry.getValue()) {
                    addAttribute(sample, att.getKey(), att.getValue(), path);
                }
            }
        }


    }
    
    public List<String> getAllAttributeOrigins()
    {
    	Set<String>			all = new LinkedHashSet<String>();
    	
    	for ( Map.Entry<String, String> entry : attributeOrigin.entrySet() )
    		all.add(entry.getValue());
    	
    	List<String>		list = new LinkedList<String>(all);
    	Collections.sort(list);
    	
    	return list;
    }

	public void removeAttributeOrigin(String path) 
	{
		// remove from loaded resources
		for ( ResourceLocator locator : new LinkedList<ResourceLocator>(loadedResources) )
			if ( locator.getPath().equals(path) )
				loadedResources.remove(locator);
			
		// remove attributes
		for ( String key : new LinkedList<String>(attributeOrigin.keySet()) )
			if ( path.equals(attributeOrigin.get(key)) )
			{
    			log.info("removing: " + key);
    			attributeNames.remove(key);
    			attributeOrigin.remove(key);				
			}
	}

	public synchronized void firePropertyChange(Object source, String propertyName,
                                   Object oldValue, Object newValue) {

        PropertyChangeEvent event =
                new PropertyChangeEvent(
                        source,
                        propertyName,
                        oldValue,
                        newValue);
        propertyChangeSupport.firePropertyChange(event);
    }

    public synchronized Comparator getAttributeComparator() {
        return Utilities.getNumericStringComparator();
    }

    /**
     * @return set of curently loaded resources
     */
    public synchronized Set<ResourceLocator> getLoadedResources() {
        return loadedResources;
    }

    /**
     * Represents a specific attribute instance.
     *
     * @author jrobinso
     */
    private static class Attribute {
        private String key;
        private String value;

        public Attribute(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public String getValue() {
            return value;
        }

    }


    public Color getColor(String attKey, String attValue) {

        if (attValue == null || attValue.length() == 0) {
            return Color.white;
        }

        if (isNumeric(attKey)) {
            AbstractColorScale cs = colorScales.get(attKey);
            {
                if (cs != null) {
                    try {
                        float x = Float.parseFloat(attValue);
                        return cs.getColor(x);
                    } catch (NumberFormatException e) {
                        return Color.white;
                    }
                }

            }
        }

        String key = (attKey + "_" + attValue).toUpperCase();
        Color c = colorMap.get(key);
        if (c == null) {
            key = ("*_" + attValue).toUpperCase();
            c = colorMap.get(key);

			if ( c == null && defaultToJet && isNumeric(attKey) )
            {
                ColumnMetaData metaData = columnMetaData.get(attKey);
                if ( metaData != null )
                {
					float min = (float) metaData.min;
                	float max = (float) metaData.max;
                	
                	JetColorScale		scale = new JetColorScale(min, max);
                	
                    try {
                        float x = Float.parseFloat(attValue);
                        c = scale.getColor(x);
                    } catch (NumberFormatException e) {
                        return Color.white;
                    }
                }
            }
            
            if (c == null) {

                key = (attValue + "_*").toUpperCase();
                c = colorMap.get(key);

                if (c == null) {

                    Integer cnt = colorCounter.get(attKey);
                    if (cnt == null) {
                        cnt = 0;
                    }
                    cnt++;
                    colorCounter.put(attKey, cnt);
                    c = randomColor(cnt);
                }

                colorMap.put(key, c);
            }
        }
        return c;
    }


    static class ColumnMetaData {
        // Assume meta data is true until proven otherwise
        boolean numeric = true;
        double min = Double.MAX_VALUE;
        double max = -min;

        void updateRange(double value) {
            min = Math.min(min, value);
            max = Math.max(max, value);
        }

        // Allow up to 1 non-numeric field

        public boolean isNumeric() {
            return numeric;
        }


        public void markNonNumeric() {
            numeric = false;
        }
    }

    static class Range {
        double min;
        double max;
        Color color;
    }


    public static Color randomColor(int idx) {
        float hue = (float) Math.random();
        float sat = (float) (0.2 + 0.8 * Math.random());	// DK added 0.2 to exclude white
        float bri = (float) (0.6 + 0.4 * Math.random());
        return Color.getHSBColor(hue, sat, bri);
    }
    
    public void clearDupErrors(long id)
    {
    	dupErrorsMap.remove(id);
    }
    
    public void addDupErrors(long id, String error)
    {
    	List<String>		errors = dupErrorsMap.get(id);
    	if ( errors == null )
    		dupErrorsMap.put(id, errors = new LinkedList<String>());
    	
    	errors.add(error);
    }
    
    public String getDupErrors(long id)
    {
    	List<String>		errors = dupErrorsMap.get(id);
    	if ( errors == null )
    		return null;
    	
    	StringBuilder		sb = new StringBuilder();
    	int					count = Math.min(5, errors.size());
    	
    	sb.append("" + errors.size() + " conflicting attribute values:");
    	for ( int n = 0 ; n < count ; n++ )
    	{
    		sb.append("\n");
    		sb.append(errors.get(n));
    	}
    	if ( count != errors.size() )
    		sb.append("\n...");
    	
    	return sb.toString();
    }

    static public String[] parseTextRepresentationKeys(String stringRepresentation)
    {
    	String[]		toks = stringRepresentation.split(",");
    	String[]		attributeNames = new String[toks.length];
    	
    	for ( int n = 0 ; n < toks.length ; n++ )
    	{
    		if ( toks[n].charAt(0) == '-' )
    			attributeNames[n] = toks[n].substring(1);
    		else
        		attributeNames[n] = toks[n];        			
    	}
    	
    	return attributeNames;
    }

    static public boolean[] parseTextRepresentationAscending(String stringRepresentation)
    {
    	String[]		toks = stringRepresentation.split(",");
    	boolean[]		ascending = new boolean[toks.length];
    	
    	for ( int n = 0 ; n < toks.length ; n++ )
    	{
    		if ( toks[n].charAt(0) == '-' )
    			ascending[n] = false;
    		else
        		ascending[n] = true;
    	}
    	
    	return ascending;
    }

	public static String formatTextRespresentation(String[] attributeNames, boolean[] ascending) {
		
		List<String>	attrs = new LinkedList<String>();
		
		for ( int n = 0 ; n < attributeNames.length ; n++ )
			attrs.add(ascending[n] ? attributeNames[n] : ("-" + attributeNames[n]));
		
		return StringUtils.join(attrs,  ",");
	}
}
