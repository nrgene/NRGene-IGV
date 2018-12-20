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

package org.broad.igv.lists;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.log4j.Logger;
import org.broad.igv.Globals;
import org.broad.igv.ui.util.MessageUtils;
import org.broad.igv.util.FileUtils;
import org.broad.igv.util.ParsingUtils;

/**
 * @author jrobinso
 * @date Sep 26, 2010
 */
public class GeneListManager {

    private static Logger log = Logger.getLogger(GeneListManager.class);

    public static final String[] DEFAULT_GENE_LISTS = {
            /*"biocarta_cancer_cp.gmt",*/  "examples.gmt", "reactome_cp.gmt", "kegg_cancer_cp.gmt"};

    public static final String USER_GROUP = "My lists";
    public static final String SESSION_GROUP = "Session lists";

    private LinkedHashSet<String> groups = new LinkedHashSet();

    private HashMap<String, File> importedFiles = new HashMap();

    private LinkedHashMap<String, GeneList> geneLists = new LinkedHashMap();

    static GeneListManager theInstance;

    public static GeneListManager getInstance() {
        if (theInstance == null) {
            theInstance = new GeneListManager();
        }
        return theInstance;
    }

    private GeneListManager() {
        loadDefaultLists();
        loadUserLists();
        loadSessionLists(null);
    }


    public GeneList getGeneList(String listID) {
        return geneLists.get(listID);
    }

    public LinkedHashMap<String, GeneList> getGeneLists() {
        return geneLists;
    }
    // Gene lists -- these don't belong here obviously


    public void addGeneList(GeneList genes) {
        geneLists.put(genes.getName(), genes);
        groups.add(genes.getGroup());
    }


    private void loadDefaultLists() {

        for (String geneListFile : DEFAULT_GENE_LISTS) {
            InputStream is = GeneListManager.class.getResourceAsStream(geneListFile);
            if (is == null) {
                log.info("Could not find gene list resource: " + geneListFile);
                return;
            }
            BufferedReader reader = null;

            try {
                reader = new BufferedReader(new InputStreamReader(is));
                new BufferedReader(new InputStreamReader(is));
                List<GeneList> lists = loadGMT(geneListFile, reader);
                for (GeneList gl : lists) {
                    gl.setEditable(false);
                    addGeneList(gl);
                }
            } catch (IOException e) {
                log.error("Error loading default gene lists", e);
                MessageUtils.showMessage("<html>Error encountered loading gene lists (" + e.toString() + ")" +
                        "<br/>See log for more details");
            } finally {
                try {
                    reader.close();
                } catch (IOException e) {

                }
            }
        }
    }

    private void loadUserLists() {
    	loadUserLists(USER_GROUP);
    }
    
    private void loadUserLists(String groupId) {
        File dir = Globals.getGeneListDirectory();
        if (dir.exists()) {
            for (File f : dir.listFiles()) {
                try {
                    if (f.getName().toLowerCase().endsWith(".gmt")) {
                        importGMTFile(f);
                    } else {
                        GeneList geneList = loadGRPFile(f);
                        geneList.setGroup(groupId);
                        if (geneList != null) {
                            addGeneList(geneList);
                        }
                    }
                } catch (IOException e) {
                    log.error("Error loading user gene lists: ", e);
                    MessageUtils.showMessage("<html>Error encountered loading user gene lists (" + e.toString() + ")" +
                            "<br/>See log for more details");
                }
            }

        }

        // Add empty group if there are no lists
        if (!groups.contains(groupId)) {
            groups.add(groupId);
        }
    }

    public void loadSessionLists(String path) {
    	
    	// create group
        if (!groups.contains(SESSION_GROUP)) 
            groups.add(SESSION_GROUP);
        
        // clear it
        
        
        // load?
        if ( path != null )
        {
        	File		f = new File(path);
        	try
        	{
	        	if ( f.exists() )
	        	{
	                GeneList geneList = loadGRPFile(f);
	                geneList.setGroup(SESSION_GROUP);
	                if (geneList != null) {
	                    addGeneList(geneList); 
	                }
	        	}
        	} catch (IOException e)
        	{
                log.error("Error loading user gene lists: ", e);
                MessageUtils.showMessage("<html>Error encountered loading user gene lists (" + e.toString() + ")" +
                        "<br/>See log for more details");        		
        	}
        }
    }


    
    GeneList loadGRPFile(File grpFile) throws IOException {
    
    	return loadGRPFile(grpFile, USER_GROUP);
    }

    
    GeneList loadGRPFile(File grpFile, String groupId) throws IOException {

        // First copy file to gene list directory
        File f = grpFile;
        File dir = Globals.getGeneListDirectory();
        if (!dir.equals(grpFile.getParentFile())) {
            f = new File(dir, grpFile.getName());
            FileUtils.copyFile(grpFile, f);
        }

        String name = f.getName();
        String group = groupId;
        String description = null;
        boolean readonly = false;
        List<String> genes = new ArrayList();
        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(f));
            String nextLine;
            while ((nextLine = reader.readLine()) != null) {
                if (nextLine.startsWith("#")) {

                    if (nextLine.startsWith("#name")) {
                        String[] tokens = nextLine.split("=");
                        if (tokens.length > 1) {
                            name = tokens[1];
                        }
                    } else if (nextLine.startsWith("#description")) {
                        String[] tokens = nextLine.split("=");
                        if (tokens.length > 1) {
                            description = tokens[1];
                        }

                    } else if (nextLine.startsWith("#readonly")) {
	                    String[] tokens = nextLine.split("=");
	                    if (tokens.length > 1) {
	                        readonly = Boolean.parseBoolean(tokens[1]);
	                    }
                    }
                } else {
                    String[] tokens = nextLine.split("\\s+");
                    for (String s : tokens) {
                        genes.add(s);
                    }
                }
            }
            if (genes.size() > 0) {
                if (name == null) {
                    name = f.getName();
                }
                importedFiles.put(name, f);
                return new GeneList(name, description, group, genes, readonly);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {

                }
            }
        }
        return null;
    }

    public void exportGMTFile(String group, File gmtFile) throws IOException
    {
    	if ( group == null )
    		throw new IOException("can't export null group");
    	
    	PrintWriter		pw = new PrintWriter(gmtFile);
    	
    	// header
    	pw.println("#group=" + group);
    	
    	// loop over all, saving ones belonging to list
    	for ( GeneList gl : getGeneLists().values() )
    	{
    		// check group
    		if ( !group.equals(gl.getGroup()) )
    			continue;

    		// build line
        	StringBuilder		sb = new StringBuilder();
        	sb.append(gl.getName());
        	if ( gl.isReadonly() )
        		sb.append(" (ro)");
        	sb.append("\t");
        	sb.append(gl.getDescription());
        	for ( String loc : gl.getLoci() )
        	{
        		sb.append("\t");
        		sb.append(loc);
        	}
        	pw.println(sb);
    	}
    	
    	pw.close();
    }
    
    public void importGMTFile(File gmtFile) throws IOException {

    	boolean			copyPending = false;
    	
        File f = gmtFile;
        File dir = Globals.getGeneListDirectory();
        if (!dir.equals(gmtFile.getParentFile())) {
            f = new File(dir, gmtFile.getName());
            copyPending = true;
        }

        String group = f.getName().replace(".gmt", "");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(gmtFile));
            importedFiles.put(group, f);
            List<GeneList> lists = loadGMT(group, reader);
            for (GeneList gl : lists) {
                gl.setEditable(!isSessionGeneList(gl));
                addGeneList(gl);
            }
            
            if ( copyPending && geneLists.size() > 0 && !isSessionGeneList(lists.get(0)) )
                FileUtils.copyFile(gmtFile, f);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {

                }
            }
        }
    }

    public List<GeneList> importGMTFile(String path) throws IOException {

        BufferedReader reader = null;
        try {
            String group = new File(path).getName();
            reader = ParsingUtils.openBufferedReader(path);
            List<GeneList> lists = loadGMT(group, reader);
            for (GeneList gl : lists) {
                gl.setEditable(false);
                addGeneList(gl);
            }
            return lists;

        }
        finally {
            if (reader != null) reader.close();
        }

    }

    private List<GeneList> loadGMT(String group, BufferedReader reader) throws IOException {
        String nextLine;
        List<GeneList> lists = new ArrayList<GeneList>();
        while ((nextLine = reader.readLine()) != null) {
            if (nextLine.startsWith("#")) {
                if (nextLine.startsWith("#group") || nextLine.startsWith("#name")) {
                    String[] tokens = nextLine.split("=");
                    if (tokens.length > 1) {
                        group = tokens[1];
                    }
                }
            } else {
                String[] tokens = nextLine.split("\t");
                if (tokens.length > 2) {
                    String name = tokens[0];
                    boolean readonly = name.endsWith(" (ro)");
                    name = trimRO(name);
                    String description = tokens[1].replaceFirst(">", "");
                    List<String> genes = new ArrayList();
                    for (int i = 2; i < tokens.length; i++) {
                        genes.add(tokens[i]);
                    }
                    lists.add(new GeneList(name, description, group, genes, readonly));
                }
            }
        }
        return lists;
    }

    public void saveGeneList(GeneList geneList) {

    	if ( !USER_GROUP.equals(geneList.getGroup()) )
    		return;
    	
        File file = null;
        PrintWriter pw = null;
        try {
            final String listName = geneList.getName();
            String description = geneList.getDescription();
            List<String> genes = geneList.getLoci();

            if (listName != null && genes != null) {
                file = new File(Globals.getGeneListDirectory(), getLegalFilename(listName) + ".grp");
                pw = new PrintWriter(new BufferedWriter(new FileWriter(file)));
                pw.println("#name=" + listName);
                if (description != null) pw.println("#description=" + description);
                if ( geneList.isReadonly() )
                	pw.println("#readonly=true");
                for (String s : genes) {
                    pw.println(s);
                }
                pw.close();
                importedFiles.put(listName, file);
            }

        } catch (IOException e) {
            if (file != null) {
                MessageUtils.showMessage("Error writing gene list file: " + file.getAbsolutePath() + " " + e.getMessage());
            }
            log.error("Error saving gene list", e);
        }
        finally {
            if (pw != null) {
                pw.close();
            }
        }
    }


    /**
     * Return a legal filename derived from the input string.
     * todo Move this to a utility class
     *
     * @param s
     * @return
     */
    private static String getLegalFilename(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return s;
        }
    }


    /**
     * Test to see of group was imported by the user  Needed to determine if group can be removed.
     */
    public boolean isImported(String groupName) {
        return importedFiles.containsKey(groupName);
    }

    public boolean isUserGeneList(GeneList gl)
    {
    	return USER_GROUP.equals(gl.getGroup());
    }
    
    public boolean isSessionGeneList(GeneList gl)
    {
    	return SESSION_GROUP.equals(gl.getGroup());
    }
    
    public LinkedHashSet<String> getGroups() {
        return groups;
    }

    public void deleteGroup(String selectedGroup) {
        File f = importedFiles.get(selectedGroup);
        if (f.exists()) {
            f.delete();
        }
        groups.remove(selectedGroup);
        importedFiles.remove(selectedGroup);

        Collection<GeneList> tmp = new ArrayList(geneLists.values());
        for (GeneList gl : tmp) {
            if (gl.getGroup().equals(selectedGroup)) {
                geneLists.remove(gl.getName());
            }
        }
    }

    public void clearSessionGroup() {

        Collection<GeneList> tmp = new ArrayList(geneLists.values());
        for (GeneList gl : tmp) {
            if (isSessionGeneList(gl) ) {
                geneLists.remove(gl.getName());
            }
        }
    }

    /**
     * Return true if a group was also removed.
     *
     * @param listName
     */
    public boolean deleteList(String listName) {


        File f = importedFiles.get(listName);
        if (f != null && f.exists()) {
            f.delete();
        }
        importedFiles.remove(listName);

        if (geneLists.containsKey(listName)) {
            String group = geneLists.get(listName).getGroup();
            geneLists.remove(listName);

            // If the group is empty remove it as well, except for user group
            if (!group.equals(USER_GROUP) && !group.equals(SESSION_GROUP)) {
                for (GeneList gl : geneLists.values()) {
                    if (gl.getGroup().equals(group)) {
                        return false;
                    }
                }
                groups.remove(group);
                return true;
            }
        }
        return false;
    }
    
    public String trimRO(String text) {
    	
    	// protect against null
    	if ( text == null )
    		return text;
    	
        if ( text.endsWith(" (ro)") )
        	text = text.substring(0, text.length() - 5);		
        
        return text;
	}


}
