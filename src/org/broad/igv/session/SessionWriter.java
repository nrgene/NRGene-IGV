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
package org.broad.igv.session;

//~--- non-JDK imports --------------------------------------------------------

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.genome.mapping.GenomeMapperException;
import org.broad.igv.lists.GeneList;
import org.broad.igv.lists.GeneListManager;
import org.broad.igv.renderer.DataRange;
import org.broad.igv.session.SessionReader.SessionAttribute;
import org.broad.igv.session.SessionReader.SessionElement;
import org.broad.igv.track.AttributeManager;
import org.broad.igv.track.Track;
import org.broad.igv.track.TrackManager;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.IGVContentPane;
import org.broad.igv.ui.TrackFilter;
import org.broad.igv.ui.TrackFilterElement;
import org.broad.igv.ui.panel.AttributeHeaderPanel;
import org.broad.igv.ui.panel.FrameManager;
import org.broad.igv.ui.panel.MainPanel;
import org.broad.igv.ui.panel.ReferenceFrame;
import org.broad.igv.ui.panel.TrackPanel;
import org.broad.igv.util.FileUtils;
import org.broad.igv.util.ResourceLocator;
import org.broad.igv.variant.VariantMenu;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author jrobinso
 */
public class SessionWriter {

    static Logger log = Logger.getLogger(SessionWriter.class);

    Session session;
    private static int CURRENT_VERSION = 4;

    /**
     * Method description
     *
     * @param session
     * @param outputFile
     * @throws IOException
     */
    public void saveSession(Session session, File outputFile) throws IOException {

        if (session == null) {
            RuntimeException e = new RuntimeException("No session found to save!");
            log.error("Session Management Error", e);
        }

        this.session = session;

        if (outputFile == null) {
            RuntimeException e = new RuntimeException("Can't save session file: " + outputFile);
            log.error("Session Management Error", e);
        }

        String xmlString = createXmlFromSession(session, outputFile);

        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(outputFile);
            fileWriter.write(xmlString);
        }
        finally {
            if (fileWriter != null) {
                fileWriter.close();
            }
        }
    }


    public String createXmlFromSession(Session session, File outputFile) throws RuntimeException {

        String xmlString = null;

        try {

            // Create a DOM document
            DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document document = documentBuilder.newDocument();
            document.setStrictErrorChecking(true);

            // Global root element
            Element globalElement = document.createElement(SessionElement.SESSION.getText());

            globalElement.setAttribute(SessionAttribute.VERSION.getText(), String.valueOf(CURRENT_VERSION));

            String		genome = null;
            String		locus = null;
            if ( !IGVContentPane.isUseTabs() )
            {
            	genome = IGV.getInstance().getGenomeManager().getGenomeId();
            	locus = session.getLocusString();
            }
            else
            {
            	// since we are going to be communicating the locus on each genome seperatly and the active-tab, the top level genome 
            	// works best when it is the genome of the first tab. As to the locus, it is not needed here.
            	try {
            		Genome		genomeObj = IGV.getInstance().getContentPane().tabsMainPanelAt(0).getGenome();
            		if ( genomeObj == null )
            			genomeObj = IGV.getInstance().getGenomeManager().currentGenome;
            		if ( genomeObj != null )            		
            			genome = genomeObj.getId();
            	} catch (NullPointerException e) {
            		
            	}
            	
            }
            	
            if (genome != null)
                globalElement.setAttribute(SessionAttribute.GENOME.getText(), genome);
            if (locus != null && !FrameManager.isGeneListMode()) {
                globalElement.setAttribute(SessionAttribute.LOCUS.getText(), locus);
            }

            String groupBy = IGV.getInstance().getTrackManager().getGroupByAttribute();
            if (groupBy != null) {
                globalElement.setAttribute(SessionAttribute.GROUP_TRACKS_BY.getText(), groupBy);
            }

            if(session.isRemoveEmptyPanels()) {
                globalElement.setAttribute("removeEmptyTracks", "true");
            }
            
            // has sort order indicator?
            String		globalSort = AttributeHeaderPanel.getReportedSortTextRepresentation();
            if ( !StringUtils.isEmpty(globalSort) )
            	globalElement.setAttribute("globalSort", globalSort);
            
            // global filter?
            String		globalFilter = VariantMenu.getGlobalFilter();
            if ( !StringUtils.isEmpty(globalFilter) )
            	globalElement.setAttribute("globalFilter", globalFilter);

            // Resource Files
            writeResources(outputFile, globalElement, document);

            // Panels
            writePanels(globalElement, document);

            // Panel layout
            writePanelLayout(globalElement, document);


            // Regions of Interest
            writeRegionsOfInterest(globalElement, document);

            // Filter
            writeFilters(session, globalElement, document);

            if (FrameManager.isGeneListMode()) {
                writeGeneList(globalElement, document);
            }
            
            // write session related gene lists
            writeSessionGeneLists(globalElement, document);

            // Hidden attributes
            if(session.getHiddenAttributes() != null && session.getHiddenAttributes().size() > 0) {
                writeHiddenAttributes(session, globalElement, document);
            }


            document.appendChild(globalElement);

            // Transform document into XML
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

            StreamResult streamResult = new StreamResult(new StringWriter());
            DOMSource source = new DOMSource(document);
            transformer.transform(source, streamResult);

            xmlString = streamResult.getWriter().toString();
        } catch (Exception e) {
            String message = "An error has occurred while trying to create the session!";
            log.error(message, e);
            JOptionPane.showMessageDialog(IGV.getMainFrame(), message);
            throw new RuntimeException(e);
        }

        return xmlString;
    }


    private void writeFilters(Session session, Element globalElement, Document document) {
        TrackFilter trackFilter = session.getFilter();
        if (trackFilter != null) {

            Element filter = document.createElement(SessionElement.FILTER.getText());

            filter.setAttribute(SessionAttribute.NAME.getText(), trackFilter.getName());

            if (IGV.getInstance().isFilterMatchAll()) {
                filter.setAttribute(SessionAttribute.FILTER_MATCH.getText(), "all");
            } else if (!IGV.getInstance().isFilterMatchAll()) {
                filter.setAttribute(SessionAttribute.FILTER_MATCH.getText(), "any");
            } else {    // Defaults to match all
                filter.setAttribute(SessionAttribute.FILTER_MATCH.getText(), "all");
            }

            if (IGV.getInstance().isFilterShowAllTracks()) {
                filter.setAttribute(SessionAttribute.FILTER_SHOW_ALL_TRACKS.getText(), "true");
            } else {    // Defaults
                filter.setAttribute(SessionAttribute.FILTER_SHOW_ALL_TRACKS.getText(), "false");
            }
            globalElement.appendChild(filter);

            // Process FilterElement elements
            Iterator iterator = session.getFilter().getFilterElements();
            while (iterator.hasNext()) {

                TrackFilterElement trackFilterElement = (TrackFilterElement) iterator.next();

                Element filterElementElement =
                        document.createElement(SessionElement.FILTER_ELEMENT.getText());
                filterElementElement.setAttribute(SessionAttribute.ITEM.getText(),
                        trackFilterElement.getSelectedItem());
                filterElementElement.setAttribute(
                        SessionAttribute.OPERATOR.getText(),
                        trackFilterElement.getComparisonOperator().getValue());
                filterElementElement.setAttribute(SessionAttribute.VALUE.getText(),
                        trackFilterElement.getValue());
                filterElementElement.setAttribute(
                        SessionAttribute.BOOLEAN_OPERATOR.getText(),
                        trackFilterElement.getBooleanOperator().getValue());
                filter.appendChild(filterElementElement);
            }
        }
    }

    private void writeRegionsOfInterest(Element globalElement, Document document) {
        Collection<RegionOfInterest> regions = session.getAllRegionsOfInterest();
        if ((regions != null) && !regions.isEmpty()) {

            Element regionsElement = document.createElement(SessionElement.REGIONS.getText());
            for (RegionOfInterest region : regions) {
                Element regionElement = document.createElement(SessionElement.REGION.getText());
                regionElement.setAttribute(SessionAttribute.GENOME.getText(), region.getGenome());
                regionElement.setAttribute(SessionAttribute.CHROMOSOME.getText(), region.getChr());
                regionElement.setAttribute(SessionAttribute.START_INDEX.getText(), String.valueOf(region.getStart()));
                regionElement.setAttribute(SessionAttribute.END_INDEX.getText(), String.valueOf(region.getEnd()));
                
                if ( region.getPreferedTab() != null )
                    regionElement.setAttribute(SessionAttribute.TAB.getText(), region.getPreferedTab());
                
                if (region.getDescription() != null) {
                    regionElement.setAttribute(SessionAttribute.DESCRIPTION.getText(), region.getDescription());
                }
                regionsElement.appendChild(regionElement);
            }
            globalElement.appendChild(regionsElement);
        }
    }

    private void writeHiddenAttributes(Session session, Element globalElement, Document document) {
        Element hiddenAttributes = document.createElement(SessionElement.HIDDEN_ATTRIBUTES.getText());
        for (String attribute : session.getHiddenAttributes()) {
             Element regionElement = document.createElement(SessionElement.ATTRIBUTE.getText());
             regionElement.setAttribute(SessionReader.SessionAttribute.NAME.getText(), attribute);
             hiddenAttributes.appendChild(regionElement);
         }
         globalElement.appendChild(hiddenAttributes);

    }
    private void writeGeneList(Element globalElement, Document document) {

        GeneList geneList = session.getCurrentGeneList();

        if (geneList != null) {

            Element geneListElement = document.createElement(SessionElement.GENE_LIST.getText());
            geneListElement.setAttribute(SessionReader.SessionAttribute.NAME.getText(), geneList.getName());

            StringBuffer genes = new StringBuffer();
            for (String gene : geneList.getLoci()) {
                genes.append(gene);
                genes.append("\n");
            }

            geneListElement.setTextContent(genes.toString());

            globalElement.appendChild(geneListElement);


            // Now store the list of frames visible
            for (ReferenceFrame frame : FrameManager.getFrames()) {

                Element frameElement = document.createElement(SessionElement.FRAME.getText());
                frameElement.setAttribute(SessionReader.SessionAttribute.NAME.getText(), frame.getName());
                frameElement.setAttribute(SessionReader.SessionAttribute.CHR.getText(), frame.getChrName());
                frameElement.setAttribute(SessionReader.SessionAttribute.START.getText(), String.valueOf(frame.getOrigin()));
                frameElement.setAttribute(SessionReader.SessionAttribute.END.getText(), String.valueOf(frame.getEnd()));

                geneListElement.appendChild(frameElement);

            }
        }
    }

    private void writeSessionGeneLists(Element globalElement, Document document) {

    	GeneListManager		manager = GeneListManager.getInstance();
    	
    	for ( GeneList geneList : manager.getGeneLists().values() )
    	{
    		if ( !manager.isSessionGeneList(geneList) )
    			continue;
    		

            Element geneListElement = document.createElement(SessionElement.GENE_LIST.getText());
            geneListElement.setAttribute(SessionReader.SessionAttribute.NAME.getText(), geneList.getName());
            geneListElement.setAttribute("type", "session");
            if ( geneList.isReadonly() )
                geneListElement.setAttribute("readonly", "true");

            StringBuffer genes = new StringBuffer();
            for (String gene : geneList.getLoci()) {
                genes.append(gene);
                genes.append("\n");
            }

            geneListElement.setTextContent(genes.toString());

            globalElement.appendChild(geneListElement);
        }
    }

    private void writeResources(File outputFile, Element globalElement, Document document) {

        Collection<ResourceLocator> resourceLocators = getResourceLocatorSet();

        if ((resourceLocators != null) && !resourceLocators.isEmpty()) {

            Element filesElement = document.createElement(SessionElement.RESOURCES.getText());
            String isRelativeDataFile = "false";
            String filepath = null;

            for (ResourceLocator resourceLocator : resourceLocators) {
                if (resourceLocator.exists() || !(resourceLocator.getPath() == null)) {

                    //RESOURCE ELEMENT
                    Element dataFileElement =
                            document.createElement(SessionElement.RESOURCE.getText());

                    if ( resourceLocator.getGenomeId() != null )
                    	dataFileElement.setAttribute("genome", resourceLocator.getGenomeId());
                    
                    // check for relative resources
                    if ( resourceLocator.getRelativePath() != null )
                    {
                    	// file loaded as relative. generate a relative path to the session new session path if possible
                    	String		relativePath = buildRelativePath(outputFile.getParent(), resourceLocator.getPath());
                    	if ( !StringUtils.isEmpty(relativePath) )
                    	{
                    		resourceLocator.setWrittenRelativePath(relativePath);
                    		dataFileElement.setAttribute(SessionAttribute.RELATIVE_PATH.getText(), "true");
    	                    dataFileElement.setAttribute(SessionAttribute.PATH.getText(), relativePath);
                    	}
                    	else
                    		resourceLocator.setWrittenRelativePath(null);
                    		
                    }
                    
                    // resource has not been written yet?
                    if ( StringUtils.isEmpty(dataFileElement.getAttribute(SessionAttribute.PATH.getText())) )
                    {
	                    //TODO Decide whether to keep this in here.. Not really necessary.
	                    if (resourceLocator.isLocal()) {
	                        filepath = FileUtils.getRelativePath(outputFile.getParentFile(),
	                                resourceLocator.getPath());
	                        if (!(filepath.equals(resourceLocator.getPath()))) {
	                            dataFileElement.setAttribute(SessionAttribute.RELATIVE_PATH.getText(),
	                                    isRelativeDataFile);
	                        }
	                    }
	                    
	                    //REQUIRED ATTRIBUTES - Cannot be null
	                    dataFileElement.setAttribute(SessionAttribute.PATH.getText(), resourceLocator.getPath());
                    }

                    //OPTIONAL ATTRIBUTES

                    if (resourceLocator.getName() != null) {
                        dataFileElement.setAttribute(SessionAttribute.NAME.getText(), resourceLocator.getName());
                    }
                    if (resourceLocator.getServerURL() != null) {
                        dataFileElement.setAttribute(SessionAttribute.SERVER_URL.getText(), resourceLocator.getServerURL());
                    }
                    if (resourceLocator.getInfolink() != null) {
                        dataFileElement.setAttribute(SessionAttribute.HYPERLINK.getText(), resourceLocator.getInfolink());
                    }
                    if (resourceLocator.getUrl() != null) {
                        dataFileElement.setAttribute(SessionAttribute.FEATURE_URL.getText(), resourceLocator.getUrl());
                    }
                    if (resourceLocator.getDescription() != null) {
                        dataFileElement.setAttribute(SessionAttribute.DESCRIPTION.getText(), resourceLocator.getDescription());
                    }
                    if (resourceLocator.getType() != null) {
                        dataFileElement.setAttribute(SessionAttribute.TYPE.getText(), resourceLocator.getType());
                    }
                    if (resourceLocator.getCoverage() != null) {
                        dataFileElement.setAttribute(SessionAttribute.COVERAGE.getText(), resourceLocator.getCoverage());
                    }
                    if (resourceLocator.getTrackLine() != null) {
                        dataFileElement.setAttribute(SessionAttribute.TRACK_LINE.getText(), resourceLocator.getTrackLine());
                    }
                    filesElement.appendChild(dataFileElement);
                }
            }
            globalElement.appendChild(filesElement);
        }
    }

    private static String buildRelativePath(String parent, String path) 
    {
    	if ( (parent == null) || (path == null) || !path.startsWith(parent) )
    		return null;
    
    	// correctly handle X:\ case
    	return path.substring(parent.length() + (parent.endsWith(FileUtils.separator) ? 0 : 1));
	}


	private void writePanels(Element globalElement, Document document) throws DOMException {

    	Map<String, List<TrackPanel>> 	  trackPanels;
    	Map<String, MainPanel>			  mainPanels;
    	if ( !IGVContentPane.isUseTabs() )
    	{
    		trackPanels = new java.util.LinkedHashMap<String, List<TrackPanel>>();
    		trackPanels.put("tab", IGV.getInstance().getTrackPanels());
    		mainPanels = null;
    	}
    	else
    	{
    		trackPanels = IGV.getInstance().getContentPane().tabsMapTrackPanels();
    		mainPanels = IGV.getInstance().getContentPane().tabsMapMainPanels();
    	}
    	
    	for ( String tab : trackPanels.keySet() )
	        for (TrackPanel trackPanel :  trackPanels.get(tab) ) {
	
	            // TODO -- loop through panels groups, rather than skipping groups to tracks
	
	            List<Track> tracks = trackPanel.getTracks();
	            if ((tracks != null) && !tracks.isEmpty()) {
	
	                Element panelElement = document.createElement(SessionElement.PANEL.getText());
	                panelElement.setAttribute("name", trackPanel.getName());
	                panelElement.setAttribute("height", String.valueOf(trackPanel.getHeight()));
	                panelElement.setAttribute("width", String.valueOf(trackPanel.getWidth()));
	                
	                if ( IGVContentPane.isUseTabs() )
	                {
	                	panelElement.setAttribute("tab", tab);
	                	
	                	if ( mainPanels != null && mainPanels.containsKey(tab) )
	                	{
	                		Genome			genome = mainPanels.get(tab).getGenome();
	                		
	                		if ( genome != null )
	                		{
	                			panelElement.setAttribute("genome", genome.getId());
	                			
	                			try
	                			{
		                			String	locus = IGV.getInstance().getContentPane().tabsGenomeTabLocus(genome, true);
		                			
		                			// cover for the current genome
		                			Genome		currentGenome = IGV.getInstance().getGenomeManager().currentGenome;
		                			if ( genome.equals(currentGenome) )
		                				locus = session.getLocusString();
		                			
		                			if ( locus != null )
		                				panelElement.setAttribute("genome-locus", locus);
	                			}
	                			catch (GenomeMapperException e)
	                			{
	                				throw new RuntimeException(e);
	                			}
	                		}
	                	}
	                }
	
	                for (Track track : tracks) {
	
	                    Element 			trackElement = document.createElement(SessionElement.TRACK.getText());
	                    ResourceLocator		resourceLocator = track.getResourceLocator();
	                    if ( resourceLocator != null && !StringUtils.isEmpty(resourceLocator.getWrittenRelativePath()) )
		                    trackElement.setAttribute(SessionReader.SessionAttribute.ID.getText(), resourceLocator.getWrittenRelativePath());
	                    else
	                    	trackElement.setAttribute(SessionReader.SessionAttribute.ID.getText(), track.getId());
	                    trackElement.setAttribute(SessionReader.SessionAttribute.NAME.getText(), track.getName());
	                    for (Map.Entry<String, String> attrValue : track.getPersistentState().entrySet()) {
	                        trackElement.setAttribute(attrValue.getKey(), attrValue.getValue());
	                    }
	
	                    // TODO -- DataRange element,  create element, append as child to track
	                    if (track.hasDataRange()) {
	                        DataRange dr = track.getDataRange();
	                        if (dr != null) {
	                            Element drElement = document.createElement(SessionElement.DATA_RANGE.getText());
	                            for (Map.Entry<String, String> attrValue : dr.getPersistentState().entrySet()) {
	                                drElement.setAttribute(attrValue.getKey(), attrValue.getValue());
	                            }
	                            trackElement.appendChild(drElement);
	                        }
	                    }
	
	                    panelElement.appendChild(trackElement);
	                }
	                globalElement.appendChild(panelElement);
	            }
	        }
    }

    private void writePanelLayout(Element globalElement, Document document) {

    	Map<String, MainPanel>	mainPanels;
    	if ( !IGVContentPane.isUseTabs() )
    	{
    		mainPanels = new LinkedHashMap<String, MainPanel>();
    		mainPanels.put("tab", IGV.getInstance().getMainPanel());
    	}
    	else
    		mainPanels = IGV.getInstance().getContentPane().tabsMapMainPanels();
    	
    	for ( String tab : mainPanels.keySet() )
    	{    	
	        double[] dividerFractions = mainPanels.get(tab).getDividerFractions();
	        if (dividerFractions.length > 0) {
	
	            Element panelLayout = document.createElement(SessionElement.PANEL_LAYOUT.getText());
	            globalElement.appendChild(panelLayout);
	
	            StringBuffer locString = new StringBuffer();
	            locString.append(String.valueOf(dividerFractions[0]));
	            for (int i = 1; i < dividerFractions.length; i++) {
	                locString.append("," + dividerFractions[i]);
	            }
	            panelLayout.setAttribute("dividerFractions", locString.toString());
	            
	            
	            if ( IGVContentPane.isUseTabs() )
	            {
	            	panelLayout.setAttribute("tab", tab);
	            	if ( IGV.getInstance().getContentPane().tabsCurrentTab().equals(tab) )
	            		panelLayout.setAttribute("active-tab", "true");
	            }
	
	        }
    	}

    }

    /**
     * @return A set of the load data files.
     */
    public Collection<ResourceLocator> getResourceLocatorSet() {

        Collection<ResourceLocator> locators = new ArrayList();

        // handle case of multiple track managers (per tab)
        Collection<ResourceLocator> currentTrackFileLocators;
        if ( !IGVContentPane.isUseTabs() )
        	currentTrackFileLocators = IGV.getInstance().getTrackManager().getDataResourceLocators();
        else
        {
        	currentTrackFileLocators = new LinkedList<ResourceLocator>();
        	for ( TrackManager trackManager : IGV.getInstance().getContentPane().tabsListTrackManagers() )
        	{
        		Collection<ResourceLocator> l = trackManager.getDataResourceLocatorsWithVariantDuplicates();
        		if ( l != null )
        			currentTrackFileLocators.addAll(l);
        	}
        }

        if (currentTrackFileLocators != null) {
            for (ResourceLocator locator : currentTrackFileLocators) {
                locators.add(locator);
            }
        }

        Collection<ResourceLocator> loadedAttributeResources =
                AttributeManager.getInstance().getLoadedResources();

        if (loadedAttributeResources != null) {
            for (ResourceLocator attributeLocator : loadedAttributeResources) {
                locators.add(attributeLocator);
            }
        }

        return locators;
    }

    /*
    static public void main(String args[])
    {
    	String			testFiles[] = {
    		"X:\\session.xml",
    		"X:\\IGV\\Dataversion_data\\nrgene_public_maize_data_v2_run3\\mo17_vs_b73v4__ver100.annotation.filter.gff3",
    		"X:/session.xml",
    		"X:/IGV/Dataversion_data/nrgene_public_maize_data_v2_run3/mo17_vs_b73v4__ver100.annotation.filter.gff3"
    	};
    	
    	for ( int n = 0 ; n < testFiles.length ; n += 2 )
    	{
	    	File			sessionFile = new File(testFiles[n]);
	    	File			trackFile = new File(testFiles[n + 1]);
	    	System.out.println("sessionFile: " + sessionFile);
	    	System.out.println("trackFile: " + trackFile);
	    	
	    	String			relativePath = buildRelativePath(sessionFile.getParent(), trackFile.getPath());   
	    	System.out.println("relativePath: " + relativePath);
    	}
    	
    }
    */
    
}

