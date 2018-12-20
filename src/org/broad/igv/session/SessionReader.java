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

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.login.FailedLoginException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.broad.igv.Globals;
import org.broad.igv.feature.RegionOfInterest;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.feature.genome.GenomeManager;
import org.broad.igv.feature.genome.mapping.GenomeMapperException;
import org.broad.igv.lists.GeneList;
import org.broad.igv.lists.GeneListManager;
import org.broad.igv.renderer.ColorScale;
import org.broad.igv.renderer.ColorScaleFactory;
import org.broad.igv.renderer.ContinuousColorScale;
import org.broad.igv.renderer.DataRange;
import org.broad.igv.track.AttributeManager;
import org.broad.igv.track.EmptyTrack;
import org.broad.igv.track.Track;
import org.broad.igv.track.TrackManager;
import org.broad.igv.track.TrackType;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.IGVContentPane;
import org.broad.igv.ui.TrackFilter;
import org.broad.igv.ui.TrackFilterElement;
import org.broad.igv.ui.panel.AttributeHeaderPanel;
import org.broad.igv.ui.panel.FrameManager;
import org.broad.igv.ui.panel.ReferenceFrame;
import org.broad.igv.ui.panel.TrackPanel;
import org.broad.igv.ui.util.MessageUtils;
import org.broad.igv.util.ColorUtilities;
import org.broad.igv.util.FileUtils;
import org.broad.igv.util.FilterElement.BooleanOperator;
import org.broad.igv.util.FilterElement.Operator;
import org.broad.igv.util.ParsingUtils;
import org.broad.igv.util.ResourceLocator;
import org.broad.igv.variant.VariantMenu;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Class to parse an IGV session file
 */
public class SessionReader {

    private static Logger log = Logger.getLogger(SessionReader.class);
    private static String INPUT_FILE_KEY = "INPUT_FILE_KEY";
    // Temporary values used in processing

    private Collection<ResourceLocator> dataFiles;
    private Collection<ResourceLocator> missingDataFiles;
    private static Map<String, String> attributeSynonymMap = new HashMap();
    private boolean panelElementPresent = false;
    private int version;

    private IGV igv;
    
    private static boolean currentlyLoading;
    
    private String defaultGenome;
    
    private List<String>		failedGenomes = new LinkedList<String>();


    /**
     * Map of track id -> track.  It is important to maintin the order in which tracks are added, thus
     * the use of LinkedHashMap.
     */
    Map<String, List<Track>> trackDictionary = Collections.synchronizedMap(new LinkedHashMap());


    /**
     * Map of relative path -> full path
     */
    Map<String, String> fullToRelPathMap = new HashMap<String, String>();

    private Track geneTrack = null;
    private Track seqTrack = null;
    private boolean hasTrackElments;


    static {
        attributeSynonymMap.put("DATA FILE", "DATA SET");
        attributeSynonymMap.put("TRACK NAME", "NAME");
    }

    /**
     * Session Element types
     */
    public static enum SessionElement {


        PANEL("Panel"),
        PANEL_LAYOUT("PanelLayout"),
        TRACK("Track"),
        COLOR_SCALE("ColorScale"),
        COLOR_SCALES("ColorScales"),
        DATA_TRACK("DataTrack"),
        DATA_TRACKS("DataTracks"),
        FEATURE_TRACKS("FeatureTracks"),
        DATA_FILE("DataFile"),
        RESOURCE("Resource"),
        RESOURCES("Resources"),
        FILES("Files"),
        FILTER_ELEMENT("FilterElement"),
        FILTER("Filter"),
        SESSION("Session"),
        GLOBAL("Global"),
        REGION("Region"),
        REGIONS("Regions"),
        DATA_RANGE("DataRange"),
        PREFERENCES("Preferences"),
        PROPERTY("Property"),
        GENE_LIST("GeneList"),
        HIDDEN_ATTRIBUTES("HiddenAttributes"),
        VISIBLE_ATTRIBUTES("VisibleAttributes"),
        ATTRIBUTE("Attribute"),
        VISIBLE_ATTRIBUTE("VisibleAttribute"),
        FRAME("Frame");

        private String name;

        SessionElement(String name) {
            this.name = name;
        }

        public String getText() {
            return name;
        }

        @Override
        public String toString() {
            return getText();
        }

        static public SessionElement findEnum(String value) {

            if (value == null) {
                return null;
            } else {
                return SessionElement.valueOf(value);
            }
        }
    }

    /**
     * Session Attribute types
     */
    public static enum SessionAttribute {

        BOOLEAN_OPERATOR("booleanOperator"),
        COLOR("color"),
        ALT_COLOR("altColor"),
        COLOR_MODE("colorMode"),
        CUSTOM_COLOR("customColor"),
        CHROMOSOME("chromosome"),
        END_INDEX("end"),
        EXPAND("expand"),
        SQUISH("squish"),
        DISPLAY_MODE("displayMode"),
        FILTER_MATCH("match"),
        FILTER_SHOW_ALL_TRACKS("showTracks"),
        GENOME("genome"),
        TAB("tab"),
        GROUP_TRACKS_BY("groupTracksBy"),
        HEIGHT("height"),
        BLOCK_HEIGHT("blockHeight"),
        ID("id"),
        ITEM("item"),
        LOCUS("locus"),
        NAME("name"),
        SAMPLE_ID("sampleID"),
        RESOURCE_TYPE("resourceType"),
        OPERATOR("operator"),
        RELATIVE_PATH("relativePath"),
        RENDERER("renderer"),
        SCALE("scale"),
        START_INDEX("start"),
        VALUE("value"),
        VERSION("version"),
        VISIBLE("visible"),
        WINDOW_FUNCTION("windowFunction"),
        RENDER_NAME("renderName"),
        GENOTYPE_HEIGHT("genotypeHeight"),
        VARIANT_HEIGHT("variantHeight"),
        PREVIOUS_HEIGHT("previousHeight"),
        FEATURE_WINDOW("featureVisibilityWindow"),
        DISPLAY_NAME("displayName"),
        COLOR_SCALE("colorScale"),

        //RESOURCE ATTRIBUTES
        PATH("path"),
        LABEL("label"),
        SERVER_URL("serverURL"),
        HYPERLINK("hyperlink"),
        INFOLINK("infolink"),
        URL("url"),
        FEATURE_URL("featureURL"),
        DESCRIPTION("description"),
        TYPE("type"),
        COVERAGE("coverage"),
        TRACK_LINE("trackLine"),

        CHR("chr"),
        START("start"),
        END("end");

        //TODO Add the following into the Attributes
        /*
        boolean shadeBases;
        boolean shadeCenters;
        boolean flagUnmappedPairs;
        boolean showAllBases;
        int insertSizeThreshold;
        boolean colorByStrand;
        boolean colorByAmpliconStrand;
         */


        private String name;

        SessionAttribute(String name) {
            this.name = name;
        }

        public String getText() {
            return name;
        }

        @Override
        public String toString() {
            return getText();
        }

        static public SessionAttribute findEnum(String value) {

            if (value == null) {
                return null;
            } else {
                return SessionAttribute.valueOf(value);
            }
        }
    }


    public SessionReader(IGV igv) {
        this.igv = igv;
    }

    public Set<String> getMissingGenomes(InputStream inputStream)
    {
    	Set<String>			genomes = new LinkedHashSet<String>();
    	Set<String>			missingGenomes = new LinkedHashSet<String>();
    	
    	// open document
        Document document = null;
        try {
            document = createDOMDocumentFromXmlFile(inputStream);
        } catch (Exception e) {
            log.error("Session Management Error", e);
            throw new RuntimeException(e);
        }
        
        // look for genome= attributes
        XPath 		xPath =  XPathFactory.newInstance().newXPath();
        NodeList nodeList;
		try {
			nodeList = (NodeList) xPath.compile("//*/@genome").evaluate(document, XPathConstants.NODESET);
		} catch (XPathExpressionException e) {
            log.error("XPath Error", e);
            throw new RuntimeException(e);
		}
        for (int i = 0; i < nodeList.getLength(); i++) 
        {
        	Node			node = nodeList.item(i);
        	log.info(node.getNodeName() + "=" + node.getNodeValue());	
        	
        	genomes.add(node.getNodeValue());
        }
        
        // check if missing
        GenomeManager			gm = IGV.getInstance().getGenomeManager();
        Set<String>				allGenomes = gm.getAllPotentialGenomeIDs();
        log.info("allGenomes=" + allGenomes);
        for ( String genome : genomes )
        {
        	if ( !allGenomes.contains(genome) )
       		missingGenomes.add(genome);
        }

        return missingGenomes;
    }
    

    /**
     * @param inputStream
     * @param session
     * @param sessionName @return
     * @throws RuntimeException
     */

    public void loadSession(InputStream inputStream, Session session, String sessionName, boolean merge) {


    	try
    	{
    		currentlyLoading = true;
    		
	        log.debug("Load session");
	
	
	        Document document = null;
	        try {
	            document = createDOMDocumentFromXmlFile(inputStream);
	        } catch (Exception e) {
	            log.error("Session Management Error", e);
	            throw new RuntimeException(e);
	        }
	
	        NodeList tracks = document.getElementsByTagName("Track");
	        hasTrackElments = tracks.getLength() > 0;
	
	        HashMap additionalInformation = new HashMap();
	        additionalInformation.put(INPUT_FILE_KEY, sessionName);
	
	        NodeList nodes = document.getElementsByTagName(SessionElement.GLOBAL.getText());
	        if (nodes == null || nodes.getLength() == 0) {
	            nodes = document.getElementsByTagName(SessionElement.SESSION.getText());
	        }
	
	        clearTabsOrder(session);
	        processRootNode(session, nodes.item(0), additionalInformation, merge);
	        enforceTabsOrder(session);
	
	        // Add tracks not explicitly set in file.  It is legal to define sessions with the DataFile section only (no
	        // Panel or Track elements).
	        addLeftoverTracks(trackDictionary.values());
	
	        if (session.getGroupTracksBy() != null && session.getGroupTracksBy().length() > 0) {
	            IGV.getInstance().getTrackManager().setGroupByAttribute(session.getGroupTracksBy());
	        }
	
	        if (session.isRemoveEmptyPanels()) {
	            IGV.getInstance().getMainPanel().removeEmptyDataPanels();
	        }
	
	        IGV.getInstance().getTrackManager().resetOverlayTracks();
	        
	        IGV.getInstance().getContentPane().tabsCloseEmptyTabs();
	        IGV.getInstance().getContentPane().tabsCleanupSequenceFromGM();
	        
	        // if only one tab left, make sure locus is actually set
	        if ( IGV.getInstance().getContentPane().tabsCount() == 1 )
	        {
	        	try {
					String		locus = IGV.getInstance().getContentPane().tabsGenomeTabLocus(IGV.getInstance().getGenomeManager().currentGenome, true);
					if ( locus != null )
						IGV.getInstance().goToLocus(locus);
				} catch (GenomeMapperException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	        }
	        
	        // now is a good time to free some memory
	        IGV.getInstance().getContentPane().getStatusBar().setMessage("Compacting memory ...");
	        IGV.getInstance().runDelayedRunnable(2000, new Runnable() {
				
				@Override
				public void run() {
			        //System.gc(); 
				}
			});
	        IGV.getInstance().getContentPane().getStatusBar().setMessage("Done");
    	}
    	finally
    	{
    		currentlyLoading = false;
    	}

    }


    private void processRootNode(Session session, Node node, HashMap additionalInformation, boolean merge) {

        if ((node == null) || (session == null)) {
            MessageUtils.showMessage("Invalid session file: root node not found");
            return;
        }

        String nodeName = node.getNodeName();
        if (!(nodeName.equalsIgnoreCase(SessionElement.GLOBAL.getText()) || nodeName.equalsIgnoreCase(SessionElement.SESSION.getText()))) {
            MessageUtils.showMessage("Session files must begin with a \"Global\" or \"Session\" element.  Found: " + nodeName);
        }
        process(session, node, additionalInformation, merge);

        Element element = (Element) node;

        // Load the genome, which can be an ID, or a path or URL to a .genome or indexed fasta file.
        String genome = getAttribute(element, SessionAttribute.GENOME.getText());
        IGV.getInstance().getContentPane().tabsResetSingleGenome();
        selectGenomeSync(genome, session);
        setDefaultGenome(genome);

        session.setLocus(getAttribute(element, SessionAttribute.LOCUS.getText()));
        session.setGroupTracksBy(getAttribute(element, SessionAttribute.GROUP_TRACKS_BY.getText()));

        String removeEmptyTracks = getAttribute(element, "removeEmptyTracks");
        if (removeEmptyTracks != null) {
            try {
                Boolean b = Boolean.parseBoolean(removeEmptyTracks);
                session.setRemoveEmptyPanels(b);
            } catch (Exception e) {
                log.error("Error parsing removeEmptyTracks string: " + removeEmptyTracks, e);
            }
        }

        String versionString = getAttribute(element, SessionAttribute.VERSION.getText());
        try {
            version = Integer.parseInt(versionString);
        } catch (NumberFormatException e) {
            log.error("Non integer version number in session file: " + versionString);
        }
        session.setVersion(version);

        geneTrack = IGV.getInstance().getTrackManager().getGeneTrack();
        if (geneTrack != null) {
            trackDictionary.put(geneTrack.getId(), Arrays.asList(geneTrack));
        }
        seqTrack = IGV.getInstance().getTrackManager().getSequenceTrack();
        if (seqTrack != null) {
            trackDictionary.put(seqTrack.getId(), Arrays.asList(seqTrack));
        }


        NodeList elements = element.getChildNodes();
        process(session, elements, additionalInformation);

        // ReferenceFrame.getInstance().invalidateLocationScale();
        
        String	globalSort = getAttribute(element, "globalSort");
        if ( StringUtils.isEmpty(globalSort) )
        	AttributeHeaderPanel.reportSort(null, null);
        else
        	AttributeHeaderPanel.reportSort(AttributeManager.parseTextRepresentationKeys(globalSort), 
        									AttributeManager.parseTextRepresentationAscending(globalSort));
        
        String globalFilter = getAttribute(element, "globalFilter");
        if ( StringUtils.isEmpty(globalFilter) )
        	VariantMenu.clearGlobalFilter();
        else
        	VariantMenu.setGlobalFilter(globalFilter);
    }

    //TODO Check to make sure tracks are not being created twice
    //TODO -- DONT DO THIS FOR NEW SESSIONS

    private void addLeftoverTracks(Collection<List<Track>> tmp) {
        Map<String, TrackPanel> trackPanelCache = new HashMap();
        if (version < 3 || !panelElementPresent) {
            for (List<Track> tracks : tmp) {
                for (Track track : tracks) {
                    if (track != geneTrack && track != seqTrack && track.getResourceLocator() != null) {

                        TrackPanel panel = trackPanelCache.get(track.getResourceLocator().getPath());
                        if (panel == null) {
                            panel = IGV.getInstance().getTrackManager().getPanelFor(track.getResourceLocator());
                            trackPanelCache.put(track.getResourceLocator().getPath(), panel);
                        }
                        panel.addTrack(track);
                    }
                }
            }
        }

    }


    /**
     * Process a single session element node.
     *
     * @param session
     * @param element
     */
    private void process(Session session, Node element, HashMap additionalInformation, boolean merge) {

        if ((element == null) || (session == null)) {
            return;
        }

        String nodeName = element.getNodeName();

        if (nodeName.equalsIgnoreCase(SessionElement.RESOURCES.getText()) ||
                nodeName.equalsIgnoreCase(SessionElement.FILES.getText())) {
            processResources(session, (Element) element, additionalInformation);
        } else if (nodeName.equalsIgnoreCase(SessionElement.RESOURCE.getText()) ||
                nodeName.equalsIgnoreCase(SessionElement.DATA_FILE.getText())) {
            processResource(session, (Element) element, additionalInformation);
        } else if (nodeName.equalsIgnoreCase(SessionElement.REGIONS.getText())) {
            processRegions(session, (Element) element, additionalInformation);
        } else if (nodeName.equalsIgnoreCase(SessionElement.REGION.getText())) {
            processRegion(session, (Element) element, additionalInformation);
        } else if (nodeName.equalsIgnoreCase(SessionElement.GENE_LIST.getText())) {
            processGeneList(session, (Element) element, additionalInformation);
        } else if (nodeName.equalsIgnoreCase(SessionElement.FILTER.getText())) {
            processFilter(session, (Element) element, additionalInformation);
        } else if (nodeName.equalsIgnoreCase(SessionElement.FILTER_ELEMENT.getText())) {
            processFilterElement(session, (Element) element, additionalInformation);
        } else if (nodeName.equalsIgnoreCase(SessionElement.COLOR_SCALES.getText())) {
            processColorScales(session, (Element) element, additionalInformation);
        } else if (nodeName.equalsIgnoreCase(SessionElement.COLOR_SCALE.getText())) {
            processColorScale(session, (Element) element, additionalInformation);
        } else if (nodeName.equalsIgnoreCase(SessionElement.PREFERENCES.getText())) {
            processPreferences(session, (Element) element, additionalInformation);
        } else if (nodeName.equalsIgnoreCase(SessionElement.DATA_TRACKS.getText()) ||
                nodeName.equalsIgnoreCase(SessionElement.FEATURE_TRACKS.getText()) ||
                nodeName.equalsIgnoreCase(SessionElement.PANEL.getText())) {
            processPanel(session, (Element) element, additionalInformation);
        } else if (nodeName.equalsIgnoreCase(SessionElement.PANEL_LAYOUT.getText())) {
            processPanelLayout(session, (Element) element, additionalInformation);
        } else if (nodeName.equalsIgnoreCase(SessionElement.HIDDEN_ATTRIBUTES.getText())) {
            processHiddenAttributes(session, (Element) element, additionalInformation);
        } else if (nodeName.equalsIgnoreCase(SessionElement.VISIBLE_ATTRIBUTES.getText())) {
            processVisibleAttributes(session, (Element) element, additionalInformation);
        }

    }

    private void processResources(Session session, Element element, HashMap additionalInformation) {
        dataFiles = new ArrayList();
        missingDataFiles = new ArrayList();
        NodeList elements = element.getChildNodes();
        process(session, elements, additionalInformation);

        if (missingDataFiles.size() > 0) {
            StringBuffer message = new StringBuffer();
            message.append("<html>The following data file(s) could not be located.<ul>");
            for (ResourceLocator file : missingDataFiles) {
                if (file.isLocal()) {
                    message.append("<li>");
                    message.append(file.getPath());
                    message.append("</li>");
                } else {
                    message.append("<li>Server: ");
                    message.append(file.getServerURL());
                    message.append("  Path: ");
                    message.append(file.getPath());
                    message.append("</li>");
                }
            }
            message.append("</ul>");
            message.append("Common reasons for this include: ");
            message.append("<ul><li>The session or data files have been moved.</li> ");
            message.append("<li>The data files are located on a drive that is not currently accessible.</li></ul>");
            message.append("</html>");

            MessageUtils.showMessage(message.toString());
        }
        
        // make sure all genomes required for these files are loaded, make sure that the first genome (the one for the first tab) is last so it will be the current)
        Set<String>			genomesToLoadSet = new LinkedHashSet<String>();
        boolean				firstLocator = true;
        for (final ResourceLocator locator : dataFiles)
        	if ( firstLocator || (locator.getGenomeId() != null && IGV.getInstance().getGenomeManager().getCachedGenomeById(locator.getGenomeId()) == null) )
        	{
        		if ( locator.getGenomeId() != null )
        			genomesToLoadSet.add(locator.getGenomeId());
        		firstLocator = false;
        	}
        List<String>		genomesToLoadList = new LinkedList<String>(genomesToLoadSet);
        /*
        if ( genomesToLoadList.size() > 1 )
        {
        	String		id = genomesToLoadList.remove(0);
        	
        	genomesToLoadList.add(id);
        }
        */
        for ( String genomeId : genomesToLoadList )
        {
        		log.info("*** resource genome loading: " + genomeId);
        		selectGenomeSync(genomeId, session);
        }
        
        if (dataFiles.size() > 0) {

            final List<String> errors = new ArrayList<String>();

            // Load files concurrently -- TODO, put a limit on # of threads?
            List<Thread> threads = new ArrayList(dataFiles.size());
            long t0 = System.currentTimeMillis();
            int i = 0;
            List<Runnable> synchronousLoads = new ArrayList<Runnable>();
            for (final ResourceLocator locator : dataFiles) {

                final String suppliedPath = locator.getPath();
                final String relPath = fullToRelPathMap.get(suppliedPath);

                Runnable runnable = new Runnable() {
                    public void run() {
                        List<Track> tracks = null;
                        try {
                            final TrackManager tm = igv.getTrackManager();
                            if (tm == null) log.info("TrackManager is null!");

                            /*
                            long started = System.currentTimeMillis();
                            log.debug("loading: " + locator.getFileName());
                            IGV.getStatusTracker().startActivity(locator.getFileName(), "loading");
                            */
                            tracks = tm.load(locator);
                            /*
                            log.debug("loaded: " + locator.getFileName() + " (" + (System.currentTimeMillis() - started) + "ms)");
                            IGV.getStatusTracker().finishActivity(locator.getFileName(), "loading", System.currentTimeMillis() - started);
                            */
                            for (Track track : tracks) {
                                if (track == null) {
                                    log.info("Null track for resource " + locator.getPath());
                                    continue;
                                }

                                 String id = track.getId();
                                if (id == null) {
                                    log.info("Null track id for resource " + locator.getPath());
                                    continue;
                                }

                                if(relPath != null) {
                                    id = id.replace(suppliedPath, relPath);
                                }

                                synchronized (trackDictionary) {
	                                List<Track> trackList = trackDictionary.get(id);
	                                if (trackList == null) {
	                                    trackList = new ArrayList();
	                                    trackDictionary.put(id, trackList);
	                                }
	                                trackList.add(track);
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error loading resource " + locator.getPath(), e);
                            String ms = "<b>" + locator.getPath() + "</b><br>&nbs;p&nbsp;" + e.toString() + "<br>";
                            errors.add(ms);
                        }
                    }
                };

                boolean isAlignment = locator.getPath().endsWith(".bam") || locator.getPath().endsWith(".entries") ||
                        locator.getPath().endsWith(".sam");


                // Run synchronously if in batch mode or if there are no "track" elments, or if this is an alignment file
                if (isAlignment || Globals.isBatch() || !hasTrackElments) {
                    synchronousLoads.add(runnable);
                } else {
                    Thread t = new Thread(runnable);
                    threads.add(t);
                    t.start();
                }
                i++;

            }

            // Wait for all threads to complete
            for (Thread t : threads) {
                try {
                    t.join();
                } catch (InterruptedException ignore) {
                }
            }

            // Now load data that must be loaded synchronously
            for (Runnable runnable : synchronousLoads) {
                runnable.run();
            }

            long dt = System.currentTimeMillis() - t0;
            log.info("Total load time = " + dt);
            
            if ( genomesToLoadList.size() != 0 )
            {
            	selectGenomeSync(genomesToLoadList.get(0), session);
            	IGV.getInstance().getContentPane().tabsReset();
            }

            if (errors.size() > 0) {
                StringBuffer buf = new StringBuffer();
                buf.append("<html>Errors were encountered loading the session:<br>");
                for (String msg : errors) {
                    buf.append(msg);
                }
                MessageUtils.showMessage(buf.toString());
            }

        }
        dataFiles = null;
    }

    private void processResource(Session session, Element element, HashMap additionalInformation) {

        String nodeName = element.getNodeName();
        boolean oldSession = nodeName.equals(SessionElement.DATA_FILE.getText());

        String label = getAttribute(element, SessionAttribute.LABEL.getText());
        String name = getAttribute(element, SessionAttribute.NAME.getText());
        String sampleId = getAttribute(element, SessionAttribute.SAMPLE_ID.getText());
        String description = getAttribute(element, SessionAttribute.DESCRIPTION.getText());
        String type = getAttribute(element, SessionAttribute.TYPE.getText());
        String coverage = getAttribute(element, SessionAttribute.COVERAGE.getText());
        String trackLine = getAttribute(element, SessionAttribute.TRACK_LINE.getText());
        String colorString = getAttribute(element, SessionAttribute.COLOR.getText());

        String relPathValue = getAttribute(element, SessionAttribute.RELATIVE_PATH.getText());
        boolean isRelativePath = ((relPathValue != null) && relPathValue.equalsIgnoreCase("true"));
        String serverURL = getAttribute(element, SessionAttribute.SERVER_URL.getText());
        
        String genomeId = getAttribute(element, "genome");

        // Older sessions used the "name" attribute for the path.
        String path = getAttribute(element, SessionAttribute.PATH.getText());

        if (oldSession && name != null) {
            path = name;
            int idx = name.lastIndexOf("/");
            if (idx > 0 && idx + 1 < name.length()) {
                name = name.substring(idx + 1);
            }
        }


        ResourceLocator resourceLocator;
        if (isRelativePath) {
            final String sessionPath = session.getPath();

            String absolutePath;
            if (sessionPath == null) {
                log.error("Null session path -- this is not expected");
                MessageUtils.showMessage("Unexpected error loading session: null session path");
                return;
            }
            absolutePath = getAbsolutePath(path, sessionPath);
            fullToRelPathMap.put(absolutePath, path);
            resourceLocator = new ResourceLocator(serverURL, absolutePath, path);

            // If the resourceLocator is relative, we assume coverage is as well
            if (coverage != null) {
                String absoluteCoveragePath = getAbsolutePath(coverage, sessionPath);
                resourceLocator.setCoverage(absoluteCoveragePath);
            }
        } else {
            resourceLocator = new ResourceLocator(serverURL, path);
            resourceLocator.setCoverage(coverage);
        }

        String url = getAttribute(element, SessionAttribute.URL.getText());
        if (url == null) {
            url = getAttribute(element, SessionAttribute.FEATURE_URL.getText());
        }
        resourceLocator.setUrl(url);

        String infolink = getAttribute(element, SessionAttribute.HYPERLINK.getText());
        if (infolink == null) {
            infolink = getAttribute(element, SessionAttribute.INFOLINK.getText());
        }
        resourceLocator.setInfolink(infolink);


        // Label is deprecated in favor of name.
        if (name != null) {
            resourceLocator.setName(name);
        } else {
            resourceLocator.setName(label);
        }

        resourceLocator.setSampleId(sampleId);


        resourceLocator.setDescription(description);
        // This test added to get around earlier bug in the writer
        if (type != null && !type.equals("local")) {
            resourceLocator.setType(type);
        }
        resourceLocator.setTrackLine(trackLine);

        if (colorString != null) {
            try {
                Color c = ColorUtilities.stringToColor(colorString);
                resourceLocator.setColor(c);
            } catch (Exception e) {
                log.error("Error setting color: ", e);
            }
        }

        if ( genomeId != null )
        	resourceLocator.setGenomeId(genomeId);
        
        dataFiles.add(resourceLocator);

        NodeList elements = element.getChildNodes();
        process(session, elements, additionalInformation);

    }

	static private String getAbsolutePath(String path, String sessionPath) {
        String absolutePath;
        if (FileUtils.isRemote(sessionPath)) {
            int idx = sessionPath.lastIndexOf("/");
            String basePath = sessionPath.substring(0, idx);
            absolutePath = basePath + "/" + path;
        } else {
            File parent = new File(sessionPath).getParentFile();
            File file = new File(parent, path);
            absolutePath = file.getAbsolutePath();
        }
        return absolutePath;
    }

    private void processRegions(Session session, Element element, HashMap additionalInformation) {

        session.clearRegionsOfInterest();
        NodeList elements = element.getChildNodes();
        process(session, elements, additionalInformation);
    }

    private void processRegion(Session session, Element element, HashMap additionalInformation) {

        String tab = getAttribute(element, SessionAttribute.TAB.getText());
        String genome = getAttribute(element, SessionAttribute.GENOME.getText());
        String chromosome = getAttribute(element, SessionAttribute.CHROMOSOME.getText());
        String start = getAttribute(element, SessionAttribute.START_INDEX.getText());
        String end = getAttribute(element, SessionAttribute.END_INDEX.getText());
        String description = getAttribute(element, SessionAttribute.DESCRIPTION.getText());

        if ( genome == null )
        	genome = getDefaultGenome();
        RegionOfInterest region = new RegionOfInterest(genome, chromosome, new Integer(start), new Integer(end), description);
        
        region.setPreferedTab(tab);

        IGV.getInstance().addRegionOfInterest(region);

        NodeList elements = element.getChildNodes();
        process(session, elements, additionalInformation);
    }


    private void processHiddenAttributes(Session session, Element element, HashMap additionalInformation) {

//        session.clearRegionsOfInterest();
        NodeList elements = element.getChildNodes();
        if (elements.getLength() > 0) {
            Set<String> attributes = new HashSet();
            for (int i = 0; i < elements.getLength(); i++) {
                Node childNode = elements.item(i);
                if (childNode.getNodeName().equals(SessionReader.SessionElement.ATTRIBUTE.getText())) {
                    attributes.add(((Element) childNode).getAttribute(SessionReader.SessionAttribute.NAME.getText()));
                }
            }
            session.setHiddenAttributes(attributes);
        }
    }


    /**
     * For backward compatibility
     *
     * @param session
     * @param element
     * @param additionalInformation
     */
    private void processVisibleAttributes(Session session, Element element, HashMap additionalInformation) {

//        session.clearRegionsOfInterest();
        NodeList elements = element.getChildNodes();
        if (elements.getLength() > 0) {
            Set<String> visibleAttributes = new HashSet();
            for (int i = 0; i < elements.getLength(); i++) {
                Node childNode = elements.item(i);
                if (childNode.getNodeName().equals(SessionReader.SessionElement.VISIBLE_ATTRIBUTE.getText())) {
                    visibleAttributes.add(((Element) childNode).getAttribute(SessionReader.SessionAttribute.NAME.getText()));
                }
            }

            final List<String> attributeNames = AttributeManager.getInstance().getAttributeNames();
            Set<String> hiddenAttributes = new HashSet<String>(attributeNames);
            hiddenAttributes.removeAll(visibleAttributes);
            session.setHiddenAttributes(hiddenAttributes);

        }
    }

    private void processGeneList(Session session, Element element, HashMap additionalInformation) {

        String 	name = getAttribute(element, SessionAttribute.NAME.getText());
        String 	type = getAttribute(element, "type");
        String 	description = getAttribute(element, "description");
        String	readonly = getAttribute(element, "readonly");
        
        boolean isSession = (type != null) && type.equals("session");
        boolean isReadonly = (readonly != null) && readonly.equals("true");

        String txt = element.getTextContent();
        String[] genes = txt.trim().split("\\s+");
        GeneList gl = new GeneList(name, Arrays.asList(genes));
        gl.setReadonly(isReadonly);
        if ( description != null )
        	gl.setDescription(description);
        if ( isSession )
        	gl.setGroup(GeneListManager.SESSION_GROUP);
        
        GeneListManager.getInstance().addGeneList(gl);
        
        if ( !isSession )
        {
        	session.setCurrentGeneList(gl);

        	// Adjust frames
        	processFrames(element);
        }
    }

    private void processFrames(Element element) {
        NodeList elements = element.getChildNodes();
        if (elements.getLength() > 0) {
            Map<String, ReferenceFrame> frames = new HashMap();
            for (ReferenceFrame f : FrameManager.getFrames()) {
                frames.put(f.getName(), f);
            }
            List<ReferenceFrame> reorderedFrames = new ArrayList();

            for (int i = 0; i < elements.getLength(); i++) {
                Node childNode = elements.item(i);
                if (childNode.getNodeName().equalsIgnoreCase(SessionElement.FRAME.getText())) {
                    String frameName = getAttribute((Element) childNode, SessionAttribute.NAME.getText());

                    ReferenceFrame f = frames.get(frameName);
                    if (f != null) {
                        reorderedFrames.add(f);
                        try {
                            String chr = getAttribute((Element) childNode, SessionAttribute.CHR.getText());
                            int start = (int) Double.parseDouble(getAttribute((Element) childNode, SessionAttribute.START.getText()));
                            int end = (int) Double.parseDouble(getAttribute((Element) childNode, SessionAttribute.END.getText()));
                            f.setInterval(chr, start, end);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                        }
                    }

                }
            }
            if (reorderedFrames.size() > 0) {
                FrameManager.setFrames(reorderedFrames);
            }
        }
        IGV.getInstance().resetFrames();
    }

    private void processFilter(Session session, Element element, HashMap additionalInformation) {

        String match = getAttribute(element, SessionAttribute.FILTER_MATCH.getText());
        String showAllTracks = getAttribute(element, SessionAttribute.FILTER_SHOW_ALL_TRACKS.getText());

        String filterName = getAttribute(element, SessionAttribute.NAME.getText());
        TrackFilter filter = new TrackFilter(filterName, null);
        additionalInformation.put(SessionElement.FILTER, filter);

        NodeList elements = element.getChildNodes();
        process(session, elements, additionalInformation);

        // Save the filter
        session.setFilter(filter);

        // Set filter properties
        if ("all".equalsIgnoreCase(match)) {
            IGV.getInstance().setFilterMatchAll(true);
        } else if ("any".equalsIgnoreCase(match)) {
            IGV.getInstance().setFilterMatchAll(false);
        }

        if ("true".equalsIgnoreCase(showAllTracks)) {
            IGV.getInstance().setFilterShowAllTracks(true);
        } else {
            IGV.getInstance().setFilterShowAllTracks(false);
        }
    }

    private void processFilterElement(Session session, Element element,
                                      HashMap additionalInformation) {

        TrackFilter filter = (TrackFilter) additionalInformation.get(SessionElement.FILTER);
        String item = getAttribute(element, SessionAttribute.ITEM.getText());
        String operator = getAttribute(element, SessionAttribute.OPERATOR.getText());
        String value = getAttribute(element, SessionAttribute.VALUE.getText());
        String booleanOperator = getAttribute(element, SessionAttribute.BOOLEAN_OPERATOR.getText());

        TrackFilterElement trackFilterElement = new TrackFilterElement(filter, item,
                Operator.findEnum(operator), value,
                BooleanOperator.findEnum(booleanOperator));
        filter.add(trackFilterElement);

        NodeList elements = element.getChildNodes();
        process(session, elements, additionalInformation);
    }

    /**
     * A counter to generate unique panel names.  Needed for backward-compatibility of old session files.
     */
    private int panelCounter = 1;

    private void processPanel(Session session, Element element, HashMap additionalInformation) {
    	
    	boolean		isFirstPanel = !panelElementPresent;
    	
        panelElementPresent = true;
        String panelName = element.getAttribute("name");
        String tab = getTabElement(element);
        String genomeId = getGenomeElement(element);
        String genomeLocus = getGenomeLocusElement(element);
        
        addTabToTabsOrder(session, tab);
        
        // load the genome?
        if ( genomeId != null )
        {
        	if ( genomeLocus != null )
        	{
        		Genome		genome = IGV.getInstance().getGenomeManager().getCachedGenomeById(genomeId);
        		if ( genome != null )
        			IGV.getInstance().getContentPane().tabsGenomeTabLocus(genome, genomeLocus);
        	}
        	selectGenomeSync(genomeId, session);
        }
        		
        if (panelName == null) {
            panelName = "Panel" + panelCounter++;
        }

        List<Track> panelTracks = new ArrayList();
        NodeList elements = element.getChildNodes();
        for (int i = 0; i < elements.getLength(); i++) {
            Node childNode = elements.item(i);
            if (childNode.getNodeName().equalsIgnoreCase(SessionElement.DATA_TRACK.getText()) ||  // Is this a track?
                    childNode.getNodeName().equalsIgnoreCase(SessionElement.TRACK.getText())) {

                List<Track> tracks = processTrack(session, (Element) childNode, additionalInformation);
                if (tracks != null) {
                    panelTracks.addAll(tracks);
                }
            } else {
                process(session, childNode, additionalInformation, false);
            }
        }

        TrackPanel panel;
        // many different cases to create seamless backwards compatability with no-tab session files 
        if ( IGVContentPane.isUseTabs() )
        {
        	if ( tab == null )
        	{
        		// tabs used, no table is given - default to current
        		panel = IGV.getInstance().getDataPanel(panelName);
        	}
        	else
        	{
        		if ( isFirstPanel && (genomeId == null) )
        		{
        			// tabs used, first panel, use tab name to rename the first tab
        			IGV.getInstance().getContentPane().tabsSwitchTo(0);
        			IGV.getInstance().getContentPane().tabsRename(tab);
        		}

        		// tabs used, normal, tab name is provided
        		panel = IGV.getInstance().getDataPanel(panelName, tab, genomeId);
        	}
        }
        else
        {
        	// tabs not used, pour into default data panel
        	panel = IGV.getInstance().getDataPanel(panelName);
        }
        
        panel.addTracks(panelTracks);
    }

  
	private String getGenomeElement(Element element) {
		return getTabElement(element, "genome");
	}


	private String getGenomeLocusElement(Element element) {
		return getTabElement(element, "genome-locus");
	}


	private String getTabElement(Element element) {
		return getTabElement(element, "tab");
	}


	private String getTabElement(Element element, String name) {
		
		String		tab = element.getAttribute(name);
		
        if ( tab != null )
        {
        	tab = tab.trim();
        	if ( tab.length() == 0 )
        		tab =  null;
        }
        
        return tab;
	}
	private void processPanelLayout(Session session, Element element, HashMap additionalInformation) {

        String nodeName = element.getNodeName();
        String tab = getTabElement(element);
        String activeTab = getTabElement(element, "active-tab");
        String panelName = nodeName;

        addTabToTabsOrder(session, tab);

        NamedNodeMap tNodeMap = element.getAttributes();
        for (int i = 0; i < tNodeMap.getLength(); i++) {
            Node node = tNodeMap.item(i);
            String name = node.getNodeName();
            if (name.equals("dividerFractions")) {
                String value = node.getNodeValue();
                String[] tokens = value.split(",");
                double[] divs = new double[tokens.length];
                try {
                    for (int j = 0; j < tokens.length; j++) {
                        divs[j] = Double.parseDouble(tokens[j]);
                    }
                    if ( !IGVContentPane.isUseTabs() || tab == null )                    
                    	session.setDividerFractions(divs);
                    else if ( tab != null )
                    {
                    	session.setDividerFractions(divs, tab);
                    	if ( activeTab != null && activeTab.equals("true") )
                    		session.setActiveTabMarker(tab);
                    }
                } catch (NumberFormatException e) {
                    log.error("Error parsing divider locations", e);
                }
            }
        }
    }



	/**
     * Process a track element.  This should return a single track, but could return multiple tracks since the
     * uniqueness of the track id is not enforced.
     *
     * @param session
     * @param element
     * @param additionalInformation
     * @return
     */

    private List<Track> processTrack(Session session, Element element, HashMap additionalInformation) {

        String id = getAttribute(element, SessionAttribute.ID.getText());

        // TODo -- put in utility method, extacts attributes from element **Definitely need to do this
        HashMap<String, String> tAttributes = new HashMap();
        HashMap<String, String> drAttributes = null;

        NamedNodeMap tNodeMap = element.getAttributes();
        for (int i = 0; i < tNodeMap.getLength(); i++) {
            Node node = tNodeMap.item(i);
            String value = node.getNodeValue();
            if (value != null && value.length() > 0) {
                tAttributes.put(node.getNodeName(), value);
            }
        }


        if (element.hasChildNodes()) {
            drAttributes = new HashMap();
            Node childNode = element.getFirstChild();
            Node sibNode = childNode.getNextSibling();
            String sibName = sibNode.getNodeName();
            if (sibName.equals(SessionElement.DATA_RANGE.getText())) {
                NamedNodeMap drNodeMap = sibNode.getAttributes();
                for (int i = 0; i < drNodeMap.getLength(); i++) {
                    Node node = drNodeMap.item(i);
                    String value = node.getNodeValue();
                    if (value != null && value.length() > 0) {
                        drAttributes.put(node.getNodeName(), value);
                    }
                }
            }
        }

        // Get matching tracks.  The trackNameDictionary is used for pre V 2 files, where ID was loosely defined
        List<Track> matchedTracks = null;
        if ( EmptyTrack.isEmptyTrackId(id) )
        {
        	matchedTracks = new LinkedList<Track>();
        	matchedTracks.add(new EmptyTrack(id));
        }
        else
        	matchedTracks = trackDictionary.get(id);
        
        
        if (matchedTracks == null) {
            log.info("Warning.  No tracks were found with id: " + id + " in session file");
        } else {

        	// in case there is more then 1 track with the same id leave it there
        	// REMOVED: && (matchedTracks.get(0) instanceof VariantTrack)
            if ( matchedTracks.size() > 1 )
            {
            	// copy list out
            	matchedTracks = new LinkedList<Track>(matchedTracks);
            	
            	// remove all but first
            	trackDictionary.get(id).remove(0);
            	while ( matchedTracks.size() > 1 )
            		matchedTracks.remove(1);
            		
            	// do not remove from trackDictionary
            	id = null;		
            }

            for (final Track track : matchedTracks) {

                // Special case for sequence & gene tracks,  they need to be removed before being placed.
                if (version >= 4 && track == geneTrack || track == seqTrack) {
                    igv.getTrackManager().removeTracks(Arrays.asList(track));
                }

                track.restorePersistentState(tAttributes);
                if (drAttributes != null) {
                    DataRange dr = track.getDataRange();
                    dr.restorePersistentState(drAttributes);
                    track.setDataRange(dr);
                }
            }
            if ( id != null )
            	trackDictionary.remove(id);


        }

        NodeList elements = element.getChildNodes();
        process(session, elements, additionalInformation);

        return matchedTracks;
    }

	private void processColorScales(Session session, Element element, HashMap additionalInformation) {

        NodeList elements = element.getChildNodes();
        process(session, elements, additionalInformation);
    }

    private void processColorScale(Session session, Element element, HashMap additionalInformation) {

        String trackType = getAttribute(element, SessionAttribute.TYPE.getText());
        String value = getAttribute(element, SessionAttribute.VALUE.getText());

        setColorScaleSet(session, trackType, value);

        NodeList elements = element.getChildNodes();
        process(session, elements, additionalInformation);
    }

    private void processPreferences(Session session, Element element, HashMap additionalInformation) {

        NodeList elements = element.getChildNodes();
        for (int i = 0; i < elements.getLength(); i++) {
            Node child = elements.item(i);
            if (child.getNodeName().equalsIgnoreCase(SessionElement.PROPERTY.getText())) {
                Element childNode = (Element) child;
                String name = getAttribute(childNode, SessionAttribute.NAME.getText());
                String value = getAttribute(childNode, SessionAttribute.VALUE.getText());
                session.setPreference(name, value);
            }
        }
    }


    /**
     * Process a list of session element nodes.
     *
     * @param session
     * @param elements
     */
    private void process(Session session, NodeList elements, HashMap additionalInformation) {
        for (int i = 0; i < elements.getLength(); i++) {
            Node childNode = elements.item(i);
            process(session, childNode, additionalInformation, false);
        }
    }


    /**
     * Reads an xml from an input file and creates DOM document.
     *
     * @param
     * @return
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     */
    private Document createDOMDocumentFromXmlFile(InputStream inputStream)
            throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document xmlDocument = documentBuilder.parse(inputStream);
        return xmlDocument;
    }


    public void setColorScaleSet(Session session, String type, String value) {

        if (type == null | value == null) {
            return;
        }

        TrackType trackType = TrackType.OTHER;

        if (TrackType.ALLELE_SPECIFIC_COPY_NUMBER.name().equalsIgnoreCase(type)) {
            trackType = TrackType.ALLELE_SPECIFIC_COPY_NUMBER;
        } else if (TrackType.CHIP.name().equalsIgnoreCase(type)) {
            trackType = TrackType.CHIP;
        } else if (TrackType.COPY_NUMBER.name().equalsIgnoreCase(type)) {
            trackType = TrackType.COPY_NUMBER;
        } else if (TrackType.DNA_METHYLATION.name().equalsIgnoreCase(type)) {
            trackType = TrackType.DNA_METHYLATION;
        } else if (TrackType.OTHER.name().equalsIgnoreCase(type)) {
            trackType = TrackType.OTHER;
        } else if (TrackType.GENE_EXPRESSION.name().equalsIgnoreCase(type)) {
            trackType = TrackType.GENE_EXPRESSION;
        } else if (TrackType.LOH.name().equalsIgnoreCase(type)) {
            trackType = TrackType.LOH;
        } else if (TrackType.MUTATION.name().equalsIgnoreCase(type)) {
            trackType = TrackType.MUTATION;
        } else if (TrackType.PHASTCON.name().equalsIgnoreCase(type)) {
            trackType = TrackType.PHASTCON;
        } else if (TrackType.TILING_ARRAY.name().equalsIgnoreCase(type)) {
            trackType = TrackType.TILING_ARRAY;
        }

        // TODO -- refactor to remove instanceof / cast.  Currently only ContinuousColorScale is handled
        ColorScale colorScale = ColorScaleFactory.getScaleFromString(value);
        if (colorScale instanceof ContinuousColorScale) {
            session.setColorScale(trackType, (ContinuousColorScale) colorScale);
        }

        // ColorScaleFactory.setColorScale(trackType, colorScale);
    }

    private String getAttribute(Element element, String key) {
        String value = element.getAttribute(key);
        if (value != null) {
            if (value.trim().equals("")) {
                value = null;
            }
        }
        return value;
    }
    
    public void selectGenomeSync(String genome, Session session)
    {
    	GenomeManager			gm = IGV.getInstance().getGenomeManager();
    	
    	log.info("selecting genome: " + genome + ", current: " + gm.currentGenome);
    	
    	if ( gm.currentGenome != null && gm.currentGenome.getId().equals(genome) )
    	{
        	log.info("genome alreading selected: " + genome);
        	return;
    	}
    	IGV.getInstance().getContentPane().setSuspendMapping(true);
    	
		if ( failedGenomes.contains(genome) || !selectGenome(genome, session) )
		{
			failedGenomes.add(genome);
	    	IGV.getInstance().getContentPane().setSuspendMapping(false);
	    	log.info("failed selecting genome: " + genome + ", current: " + gm.currentGenome);
	    	
	    	// selecting default genome
	    	String		defaultGenome = null;
	    	try {
	    		if ( gm.getUserDefinedGenomeArchiveList().size() > 0 )
	    			defaultGenome = gm.getUserDefinedGenomeArchiveList().get(0).getId();
	    	} catch (IOException e) {
	    		
	    	}
	    	if ( defaultGenome == null )
	    		defaultGenome = IGV.getInstance().getGenomeIds().iterator().next();
	    	
	    	selectGenomeSync(defaultGenome, session);
	    	
	    	return;
		}
		
		while ( (gm.currentGenome == null || !gm.currentGenome.getId().equalsIgnoreCase(genome)) && (genome != null) )
		{
			try
			{
				log.info("waiting for genome to load: " + genome);
				Thread.sleep(250);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
		}
		
    	log.info("waiting for locus to settle on: " + genome);
		try
		{
			Thread.sleep(1000);
		}
		catch (InterruptedException e)
		{
			Thread.currentThread().interrupt();
		}

    	IGV.getInstance().getContentPane().setSuspendMapping(false);

    	log.info("done selecting genome: " + genome + ", current: " + gm.currentGenome);
    }

    static private boolean selectGenome(String genome, Session session)
    {
        if (genome != null && genome.length() > 0) {
            // THis is a hack, and a bad one, but selecting a genome will actually "reset" the session so we have to
            // save the path and restore it.
            String sessionPath = session.getPath();
            if (IGV.getInstance().getGenomeIds().contains(genome)) {
                IGV.getInstance().selectGenomeFromList(genome);
            } else {
                String genomePath = genome;
                if (!ParsingUtils.pathExists(genomePath)) {
                    genomePath = getAbsolutePath(genome, session.getPath());
                }
                if (ParsingUtils.pathExists(genomePath)) {
                    try {
                        IGV.getInstance().loadGenome(genomePath, null);
                    } catch (IOException e) {
                        throw new RuntimeException("Error loading genome: " + genome);
                    }
                } else {
                    MessageUtils.showMessage("Warning: Could not locate genome: " + genome);
                    return false;
                }
            }
            session.setPath(sessionPath);
        }
        
        //IGV.getInstance().getMainPanel().resetPanels(false);
        return true;
    }

	public static boolean isCurrentlyLoading() 
	{
		return currentlyLoading;
	}


	private void clearTabsOrder(Session session)
	{
		session.getTabsOrder().clear();
	}
	
    private void addTabToTabsOrder(Session session, String tab) 
    {
    	if ( !session.getTabsOrder().contains(tab) )
    		session.getTabsOrder().add(tab);
	}
    
    private void enforceTabsOrder(Session session)
    {
    	if ( !IGVContentPane.isUseTabs() )
    		return;
    	
    	IGVContentPane	contentPane = IGV.getInstance().getContentPane();
    	List<String>	desiredOrder = session.getTabsOrder();
    	List<String>	currentOrder = contentPane.tabsNameList();
    	
    	log.info("desiredOrder: " + desiredOrder);
    	log.info("currentOrder: " + currentOrder);
    	if ( desiredOrder.size() == currentOrder.size() && desiredOrder.size() > 1 )
    	{
    		int			size = desiredOrder.size();
    		boolean		deltaDetected = false;
    		int[]		newIndex = new int[size];
    		
    		for ( int n = 0 ; n < size ; n++ )
    		{
    			newIndex[n] = currentOrder.indexOf(desiredOrder.get(n));
    			if ( newIndex[n] != n )
    				deltaDetected = true;
    		}
    		
    		if ( deltaDetected )
    			contentPane.tabsReorder(newIndex);
    	}
    }


	public String getDefaultGenome() {
		return defaultGenome;
	}


	public void setDefaultGenome(String defaultGenome) {
		this.defaultGenome = defaultGenome;
	}


}
