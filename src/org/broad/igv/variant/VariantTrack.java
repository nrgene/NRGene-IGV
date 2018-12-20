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


//chr2:128,565,093-128,565,156

package org.broad.igv.variant;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.broad.igv.PreferenceManager;
import org.broad.igv.nrgene.api.ApiRequest;
import org.broad.igv.renderer.GraphicUtils;
import org.broad.igv.session.SessionReader;
import org.broad.igv.track.AttributeManager;
import org.broad.igv.track.AttributeValue;
import org.broad.igv.track.FeatureSource;
import org.broad.igv.track.FeatureTrack;
import org.broad.igv.track.PackedFeatures;
import org.broad.igv.track.RenderContext;
import org.broad.igv.track.Track;
import org.broad.igv.track.TrackClickEvent;
import org.broad.igv.track.TribbleFeatureSource;
import org.broad.igv.ui.FontManager;
import org.broad.igv.ui.IGV;
import org.broad.igv.ui.TrackFilter;
import org.broad.igv.ui.panel.AttributeHeaderPanel;
import org.broad.igv.ui.panel.IGVPopupMenu;
import org.broad.igv.ui.panel.MouseableRegion;
import org.broad.igv.ui.panel.ReferenceFrame;
import org.broad.igv.util.FilterElement;
import org.broad.igv.util.ResourceLocator;
import org.broad.igv.variant.VariantMenu.AttributesComparator;
import org.broad.igv.variant.vcf.VCFGenotype;
import org.broad.tribble.Feature;
import org.broadinstitute.sting.utils.codecs.vcf.VCFFormatHeaderLine;
import org.broadinstitute.sting.utils.codecs.vcf.VCFHeader;
import org.broadinstitute.sting.utils.codecs.vcf.VCFHeaderLine;

/**
 * @author Jesse Whitworth, Jim Robinson, Fabien Campagne
 */
public class VariantTrack extends FeatureTrack {

    private static Logger log = Logger.getLogger(VariantTrack.class);

    static final DecimalFormat numFormat = new DecimalFormat("#.###");

    private static final Color OFF_WHITE = new Color(170, 170, 170);
    private static final int GROUP_BORDER_WIDTH = 3;
    private static final Color BAND1_COLOR = new Color(245, 245, 245);
    private static final Color BAND2_COLOR = Color.white;
    private static final Color borderGray = new Color(200, 200, 200);

    private final static int DEFAULT_EXPANDED_GENOTYPE_HEIGHT = 15;
    private final int DEFAULT_SQUISHED_GENOTYPE_HEIGHT = 4;
    private final static int DEFAULT_VARIANT_BAND_HEIGHT = 25;
    private final static int MAX_FILTER_LINES = 15;

    public static boolean floatingHeader = true;
    
    // list of selected samples
    private List<String>				selectedSamples = new LinkedList<String>();
    private Color						selectedSampleColor = PreferenceManager.getInstance().getAsColor(PreferenceManager.VR_COLOR_SELECTED, new Color(180,180,180));
    private int							lastSelectedSampleIndex = -1;
    

    /**
     * The renderer.
     */
    private VariantRenderer renderer = new VariantRenderer(this);

    /**
     * When this flag is true, we have detected that the VCF file contains the FORMAT MR column representing
     * methylation data. This will enable the "Color By/Methylation Rate" menu item.
     */
    private boolean enableMethylationRateSupport;


    /**
     * Top (y) position of this track.  This is updated whenever the track is drawn.
     */
    private int top;

    /**
     * The height of a single row in in squished mode
     */
    private int squishedHeight = DEFAULT_SQUISHED_GENOTYPE_HEIGHT;

    /**
     * The height of the top band representing the variant call
     */
    private int variantBandHeight = DEFAULT_VARIANT_BAND_HEIGHT;

    /**
     * List of all samples, in the order they appear in the file.
     */
    List<String> allSamples;
    List<String> allSamplesUnfiltered;

    /**
     * Boolean indicating if samples are grouped.
     */
    private boolean grouped;

    /**
     * The id of the group used to group samples.
     */
    private String groupByAttribute;

    /**
     * Map of group -> samples.  Each entry defines a group, the key is the group name and the value the list of
     * samles in the group.
     */
    LinkedHashMap<String, List<String>> samplesByGroups = new LinkedHashMap();


    /**
     * Current coloring option
     */
    private ColorMode coloring = ColorMode.GENOTYPE;
    private String coloringCustom = null;

    /**
     * When true, variants that are marked filtering are not drawn.
     */
    private boolean hideFiltered = false;

    /**
     * If true the variant ID, when present, will be rendered.
     */
    private boolean renderID = true;

    /**
     * The currently selected variant.  This is a transient variable, set only while the popup menu is up.
     */
    private Variant selectedVariant;

    /**
     * Transient list to keep track of the vertical bounds of each sample.  Set when rendering names, used to
     * select correct sample for popup text.  We use a list and linear lookup for now, some sort of tree structure
     * would be faster.
     */
    private List<SampleBounds> sampleBounds = new ArrayList();
//    private ZygosityCount zygosityCount;
    
    
    // DK - encoding of last sort/filter settings
    private String	lastSortComparatorSetting;
    private String	lastFilterSetting;
    private boolean lastShowAll;

	private boolean rejectTooltipOnRange = true;


    public VariantTrack(ResourceLocator locator, FeatureSource source, List<String> samples,
                    boolean enableMethylationRateSupport) {
        super(locator, source);
    	if ( source instanceof TribbleFeatureSource )
    		((TribbleFeatureSource)source).registerTrack(this);

        this.enableMethylationRateSupport = enableMethylationRateSupport;
        if (enableMethylationRateSupport) {
            // also set the default color mode to Methylation rate:
            coloring = ColorMode.METHYLATION_RATE;
        }

        this.allSamples = samples;
        this.allSamplesUnfiltered = new LinkedList<String>(allSamples);
        
        // setup standard attributes
        for ( String sample : allSamples )
        	AttributeManager.getInstance().addAttribute(sample, "TRACK_ID", sample, locator.getPath());
        
        // this handles the new attribute grouping mechanism:
        setupGroupsFromAttributes();

        setDisplayMode(DisplayMode.EXPANDED);

        setRenderID(false);

        // Estimate visibility window.
        // TODO -- set beta based on available memory
        int cnt = Math.max(1, allSamples.size());
        int beta = 10000000;
        double p = Math.pow(cnt, 1.5);
        int visWindow = (int) Math.min(500000, (beta / p) * 1000);
        setVisibilityWindow(visWindow);

        // IGV-251 - if has custom coloring, select the first
        Map<String, String>		colors = getTrackCustomColors();
        if ( colors.size() > 0 ) 
        {
        	String		color = colors.keySet().iterator().next();
        	
        	 setColorMode(VariantTrack.ColorMode.CUSTOM);
             setColoringCustom(color);
        }
    }


    /**
     * Set groups from global sample information attributes.
     */
    private void setupGroupsFromAttributes() {
        // setup groups according to the attribute used for sorting (loaded from a sample information file):

        AttributeManager manager = AttributeManager.getInstance();
        String newGroupByAttribute = IGV.getInstance().getTrackManager().getGroupByAttribute();

        // The first equality handles the case where both are null
        if ((newGroupByAttribute == groupByAttribute) ||
                (newGroupByAttribute != null && newGroupByAttribute.equals(groupByAttribute))) {
            // Nothing to do
            return;
        }


        samplesByGroups.clear();

        groupByAttribute = newGroupByAttribute;

        if (groupByAttribute == null) {
            grouped = false;
            return;
        }

        for (String sample : allSamples) {

            String sampleGroup = manager.getAttribute(sample, newGroupByAttribute);

            List<String> sampleList = samplesByGroups.get(sampleGroup);
            if (sampleList == null) {
                sampleList = new ArrayList();
                samplesByGroups.put(sampleGroup, sampleList);
            }
            sampleList.add(sample);
        }

        grouped = samplesByGroups.size() > 1;
        groupByAttribute = newGroupByAttribute;
    }

    /**
     * Sort samples.  Sort both the master list and groups, if any.
     *
     * @param comparator the comparator to sort by
     */
    public void sortSamples(Comparator<String> comparator) {
    	
    	lastSortComparatorSetting = comparator.toString();
   
    	Collections.sort(allSamples);
        Collections.sort(allSamples, comparator);
        for (List<String> samples : samplesByGroups.values()) {
        	Collections.sort(samples);
            Collections.sort(samples, comparator);
        }
    }
    
    public void filterSamples(TrackFilter filter, boolean showAll)
    {
		lastFilterSetting = filter.toString();
		lastShowAll = showAll;

		if ( showAll )
    	{
    		allSamples = new LinkedList<String>(allSamplesUnfiltered);
    	}
    	else
    	{
    		allSamples.clear();
    		for ( String sample : allSamplesUnfiltered )
    		{
                // Must start as null which means no previous results
                Boolean result = null;

                // Evaluate tracks
                Iterator iterator = filter.getFilterElements();
                while (iterator.hasNext()) {

                    FilterElement element = (FilterElement) iterator.next();
                    result = element.evaluate(sample, result);
                }
                
                if ( result != null && result.booleanValue() )
                	allSamples.add(sample);
    		}
    		
    	}
    	
    	// must reapply sort here
    	if ( lastSortComparatorSetting != null && lastSortComparatorSetting.length() != 0 )
    		sortSamples(new AttributesComparator(lastSortComparatorSetting));
    		
    }


    public boolean isEnableMethylationRateSupport() {
        return enableMethylationRateSupport;
    }


    /**
     * Returns the height of a single sample (genotype) band
     *
     * @return
     */
    public int getGenotypeBandHeight() {
        switch (getDisplayMode()) {
            case SQUISHED:
                return getSquishedHeight();
            case COLLAPSED:
                return 0;
            default:
                return DEFAULT_EXPANDED_GENOTYPE_HEIGHT;

        }
    }

    /**
     * Returns the total height of the track (including all sample/genotypes)
     *
     * @return
     */
    public int getHeight() {

        int sampleCount = allSamples.size();
        if (getDisplayMode() == Track.DisplayMode.COLLAPSED || sampleCount == 0) {
            return variantBandHeight;
        } else {
            final int groupCount = samplesByGroups.size();
            int margins = groupCount * 3;
            return variantBandHeight + margins + (sampleCount * getGenotypeBandHeight());
        }

    }

    /**
     * Set the height of the track.
     *
     * @param height
     */
    public void setHeight(int height) {

        final DisplayMode displayMode = getDisplayMode();

        // If collapsed there's nothing we can do to affect height
        if (displayMode == DisplayMode.COLLAPSED) {
            return;
        }

        // If height is < expanded height try "squishing" track, otherwise expand it
        final int groupCount = samplesByGroups.size();
        final int margins = (groupCount - 1) * 3;
        int sampleCount = allSamples.size();
        final int expandedHeight = variantBandHeight + margins + (sampleCount * getGenotypeBandHeight());
        if (height < expandedHeight) {
            setDisplayMode(DisplayMode.SQUISHED);
            squishedHeight = Math.max(1, (height - variantBandHeight - margins) / sampleCount);
        } else {
            if (displayMode != DisplayMode.EXPANDED) {
                setDisplayMode(DisplayMode.EXPANDED);
            }
        }
    }


    /**
     * Render the features in the supplied rectangle.
     *
     * @param context
     * @param trackRectangle
     * @param packedFeatures
     */
    @Override
    protected void renderFeatureImpl(RenderContext context, Rectangle trackRectangle, PackedFeatures packedFeatures) {

        Graphics2D g2D = context.getGraphics();

        top = trackRectangle.y;
        final int left = trackRectangle.x;
        final int right = (int) trackRectangle.getMaxX();

        Rectangle visibleRectangle = context.getVisibleRect();

        // A disposable rect -- note this gets modified all over the place, bad practice
        Rectangle rect = new Rectangle(trackRectangle);
        rect.height = getGenotypeBandHeight();
        rect.y = trackRectangle.y + variantBandHeight;
        drawBackground(g2D, rect, visibleRectangle, BackgroundType.DATA);

        if (top > visibleRectangle.y && top < visibleRectangle.getMaxY()) {
            g2D.drawLine(left, top + 1, right, top + 1);
        }

        List<Feature> features = packedFeatures.getFeatures();
        if (features.size() > 0) {

            final double locScale = context.getScale();
            final double origin = context.getOrigin();
            ;
            int lastPX = -1;
            final double pXMin = rect.getMinX();
            final double pXMax = rect.getMaxX();

            for (Feature feature : features) {

                Variant variant = (Variant) feature;
                //char ref = getReference(variant, windowStart, reference);

                if (hideFiltered && variant.isFiltered()) {
                    continue;
                }

                // 1 -> 0 based coordinates
                int start = variant.getStart() - 1;
                int end = variant.getEnd();                
                int pX = (int) (((double)start + variant.getStartFraction() - origin) / locScale);
                int dX = (int) Math.max(2, (end - start) / locScale);
                
                int		org_dX = dX;
                if ( allSamples.size() > 0 )
                {
                    Genotype genotype = variant.getGenotype(allSamples.get(0));

                	dX = VariantRenderer.adjustLength(dX, genotype, context, pX, variant);
                }

                if (pX + dX < pXMin) {
                    continue;
                }
                if (pX > pXMax) {
                    break;
                }
                int w = dX;
                int org_w = org_dX;
                int x = pX;
                if (w < 3) {
                    org_w = w = 3;
                    x--;
                }

//                if (pX + dX > lastPX) {  // draw rectangle only if its endpoint is bigger than last endpoint.

                    rect.y = top;
                    rect.height = variantBandHeight;
                    Rectangle	variantBandRect = new Rectangle(rect);	// save for later drawing
                    renderer.resetBandColors();
                    
                    if (getDisplayMode() != Track.DisplayMode.COLLAPSED) {
                        rect.y += rect.height;
                        rect.height = getGenotypeBandHeight();

                        // Loop through groups
                        if (grouped) {
                            for (Map.Entry<String, List<String>> entry : samplesByGroups.entrySet()) {
                                for (String sample : entry.getValue()) {
                                    renderer.renderGenotypeBandSNP(variant, context, rect, x, org_w, sample, coloring,
                                            hideFiltered, !rect.intersects(visibleRectangle));
                                    rect.y += rect.height;
                                }
                                g2D.setColor(OFF_WHITE);
                                g2D.fillRect(rect.x, rect.y, rect.width, GROUP_BORDER_WIDTH);
                                rect.y += GROUP_BORDER_WIDTH;
                            }
                        } else {
                            for (String sample : allSamples) {
                                renderer.renderGenotypeBandSNP(variant, context, rect, x, org_w, sample, coloring,
                                        hideFiltered, !rect.intersects(visibleRectangle));
                                rect.y += rect.height;
                            }

                        }
                    }


                    if (floatingHeader || variantBandRect.intersects(visibleRectangle)) {
                        renderer.renderVariantBand(variant, variantBandRect, x, w, context, hideFiltered, floatingHeader ? visibleRectangle : null);
                    }

                    boolean isSelected = selectedVariant != null && selectedVariant == variant;
                    if (isSelected) {
                        Graphics2D selectionGraphics = context.getGraphic2DForColor(Color.black);
                        selectionGraphics.drawRect(x, top, w, getHeight());
                    }

                    lastPX = pX + dX;

//                }

            }
        } else {
            rect.height = variantBandHeight;
            rect.y = trackRectangle.y;
            g2D.setColor(Color.gray);
            GraphicUtils.drawCenteredText("No Variants Found", trackRectangle, g2D);
        }

        // Variant band border
        if (allSamples.size() > 0) {
            int variantBandY = trackRectangle.y + variantBandHeight;
            if ( (variantBandY >= visibleRectangle.y && variantBandY <= visibleRectangle.getMaxY())) {
                Graphics2D borderGraphics = context.getGraphic2DForColor(Color.black);
                borderGraphics.drawLine(left, variantBandY, right, variantBandY);
            }
        }

        // Bottom border
        int bottomY = trackRectangle.y + trackRectangle.height;
        if (bottomY >= visibleRectangle.y && bottomY <= visibleRectangle.getMaxY()) {
            g2D.drawLine(left, bottomY, right, bottomY);
        }


    }


    /**
     * Render the name panel.
     * <p/>
     * NOTE:  The sample names are actually drawn in the drawBackground method!
     *
     * @param g2D
     * @param trackRectangle
     * @param visibleRectangle
     */
    @Override
    public void renderName(Graphics2D g2D, Rectangle trackRectangle, Rectangle visibleRectangle) {

        top = trackRectangle.y;
        final int left = trackRectangle.x;
        final int right = (int) trackRectangle.getMaxX();

        Rectangle rect = new Rectangle(trackRectangle);
        Font	  font = getNameFont();
        if ( font == null )
        	font = FontManager.getFont(fontSize);
        g2D.setFont(font);
        g2D.setColor(getNameColor());

        if (top > visibleRectangle.y && top < visibleRectangle.getMaxY()) {
            g2D.setColor(Color.black);
            g2D.drawLine(left, top + 1, right, top + 1);
        }


        g2D.setColor(getNameColor());
        rect.height = variantBandHeight;
        if (rect.intersects(visibleRectangle)) {
            GraphicUtils.drawWrappedText(getName(), rect, g2D, false);
        }

        rect.y += rect.height;
        rect.height = getGenotypeBandHeight();
        if (getDisplayMode() != Track.DisplayMode.COLLAPSED) {

            // The sample bounds list will get reset when  the names are drawn.
            sampleBounds.clear();
            drawBackground(g2D, rect, visibleRectangle, BackgroundType.NAME);

        }

        // Bottom border
        int bottomY = trackRectangle.y + trackRectangle.height;
        if (bottomY >= visibleRectangle.y && bottomY <= visibleRectangle.getMaxY()) {
            g2D.setColor(borderGray);
            g2D.drawLine(left, bottomY, right, bottomY);
        }

        // Variant / Genotype border
        if (allSamples.size() > 0) {
            int variantBandY = trackRectangle.y + variantBandHeight;
            if (variantBandY >= visibleRectangle.y && variantBandY <= visibleRectangle.getMaxY()) {
                g2D.setColor(Color.black);
                g2D.drawLine(left, variantBandY, right, variantBandY);
            }
        }

    }

    /**
     * Render sample attributes, if any.
     *
     * @param g2D
     * @param trackRectangle
     * @param visibleRectangle
     * @param attributeNames
     * @param mouseRegions
     */
    public void renderAttributes(Graphics2D g2D, Rectangle trackRectangle, Rectangle visibleRectangle,
                                 List<String> attributeNames, List<MouseableRegion> mouseRegions) {

        top = trackRectangle.y;
        final int left = trackRectangle.x;
        final int right = (int) trackRectangle.getMaxX();
        Rectangle rect = new Rectangle(trackRectangle);

        g2D.setColor(Color.black);
        if (top > visibleRectangle.y && top < visibleRectangle.getMaxY()) {
            g2D.drawLine(left, top + 1, right, top + 1);
        }

        rect.height = variantBandHeight;
        if (rect.intersects(visibleRectangle)) {
            super.renderAttributes(g2D, rect, visibleRectangle, attributeNames, mouseRegions);
        }

        if (getDisplayMode() == Track.DisplayMode.COLLAPSED) {
            return;
        }

        rect.y += rect.height;
        rect.height = getGenotypeBandHeight();
        Rectangle bandRectangle = new Rectangle(rect);  // Make copy for later use

        drawBackground(g2D, rect, visibleRectangle, BackgroundType.ATTRIBUTE);

        if (grouped) {
            for (List<String> sampleList : samplesByGroups.values()) {
                renderAttibuteBand(g2D, bandRectangle, visibleRectangle, attributeNames, sampleList, mouseRegions);
                bandRectangle.y += GROUP_BORDER_WIDTH;

            }
        } else {
            renderAttibuteBand(g2D, bandRectangle, visibleRectangle, attributeNames, allSamples, mouseRegions);

        }

        // Bottom border
         int bottomY = trackRectangle.y + trackRectangle.height;
         if (bottomY >= visibleRectangle.y && bottomY <= visibleRectangle.getMaxY()) {
             g2D.setColor(borderGray);
             g2D.drawLine(left, bottomY, right, bottomY);
         }

         // Variant / Genotype border
         if (allSamples.size() > 0) {
             int variantBandY = trackRectangle.y + variantBandHeight;
             if (variantBandY >= visibleRectangle.y && variantBandY <= visibleRectangle.getMaxY()) {
                 g2D.setColor(Color.black);
                 g2D.drawLine(left, variantBandY, right, variantBandY);
             }
         }

    }

    /**
     * Render attribues for a sample.   This is mostly a copy of AbstractTrack.renderAttibutes().
     * TODO -- refactor to eliminate duplicate code from AbstractTrack
     *
     * @param g2D
     * @param bandRectangle
     * @param visibleRectangle
     * @param attributeNames
     * @param sampleList
     * @param mouseRegions
     * @return
     */
    private void renderAttibuteBand(Graphics2D g2D, Rectangle bandRectangle, Rectangle visibleRectangle,
                                    List<String> attributeNames, List<String> sampleList, List<MouseableRegion> mouseRegions) {


        for (String sample : sampleList) {

            if (bandRectangle.intersects(visibleRectangle)) {

                int x = bandRectangle.x;

                for (String name : attributeNames) {

                    String key = name.toUpperCase();
                    AttributeValue attributeValue = AttributeManager.getInstance().getAttributeValue(sample, key);
                    if (attributeValue != null) {
                        Rectangle rect = new Rectangle(x, bandRectangle.y, AttributeHeaderPanel.ATTRIBUTE_COLUMN_WIDTH,
                                bandRectangle.height);
                        drawAttributeValue(attributeValue, key, g2D, rect);
                        mouseRegions.add(new MouseableRegion(rect, key, attributeValue.getStringValue(), this));
                    }
                    x += AttributeHeaderPanel.ATTRIBUTE_COLUMN_WIDTH + AttributeHeaderPanel.COLUMN_BORDER_WIDTH;
                }

            }
            bandRectangle.y += bandRectangle.height;

        }
    }


    private void drawAttributeValue(AttributeValue attributeValue, String key, Graphics2D g2D, Rectangle rect) 
    {
    	boolean			isEmpty = attributeValue.isEmpty();
    	boolean			isMulti = attributeValue.isMultiValue();
    	boolean			isNan = attributeValue.isNan();
    	
    	// get a representative value to get a color
    	String			value = "";
    	if ( !isEmpty )
    		value = attributeValue.getRepresentativeValue();
        Color color = AttributeManager.getInstance().getColor(key, value);
        g2D.setColor(color);
        g2D.fill(rect);

        if ( isEmpty || isMulti )
        {
        	g2D.setColor(Color.GRAY);
        	
        	g2D.setColor(Color.GRAY);
        	g2D.drawLine(rect.x + rect.width, rect.y, rect.x, rect.y + rect.height);
        	g2D.drawLine(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
        }
        else if ( isNan )
        {
        	g2D.setColor(Color.GRAY);
        	g2D.drawLine(rect.x, rect.y, rect.x + rect.width, rect.y + rect.height);
        }
	}


	private void drawBackground(Graphics2D g2D, Rectangle bandRectangle, Rectangle visibleRectangle,
                                BackgroundType type) {


        if (getDisplayMode() == Track.DisplayMode.COLLAPSED) {
            return;
        }

        boolean coloredLast = true;
        Rectangle textRectangle = new Rectangle(bandRectangle);
        textRectangle.height--;

        int bandFontSize = Math.min(fontSize, (int) bandRectangle.getHeight() - 1);
        Font font = getNameFont();
        if ( font == null )
        	font = FontManager.getFont(bandFontSize);
        Font oldFont = g2D.getFont();
        g2D.setFont(font);
        
        Color			oldColor = getNameColor();
        g2D.setColor(getNameColor());

        if (grouped) {
            for (Map.Entry<String, List<String>> sampleGroup : samplesByGroups.entrySet()) {
                int y0 = bandRectangle.y;

                List<String> sampleList = sampleGroup.getValue();
                coloredLast = colorBand(g2D, bandRectangle, visibleRectangle, coloredLast, textRectangle, sampleList, type);

                g2D.setColor(OFF_WHITE);
                g2D.fillRect(bandRectangle.x, bandRectangle.y, bandRectangle.width, GROUP_BORDER_WIDTH);
                bandRectangle.y += GROUP_BORDER_WIDTH;

                if (type == BackgroundType.NAME && bandRectangle.height < 3) {
                    String group = sampleGroup.getKey();
                    if (group != null) {
                        g2D.setColor(getNameColor());
                        g2D.setFont(oldFont);
                        int y2 = bandRectangle.y;
                        Rectangle textRect = new Rectangle(bandRectangle.x, y0, bandRectangle.width, y2 - y0);
                        GraphicUtils.drawWrappedText(group, textRect, g2D, true);
                    }
                }

            }

        } else {
            coloredLast = colorBand(g2D, bandRectangle, visibleRectangle, coloredLast, textRectangle, allSamples, type);

        }
        g2D.setFont(oldFont);
    }

    private boolean colorBand(Graphics2D g2D, Rectangle bandRectangle, Rectangle visibleRectangle,
                              boolean coloredLast, Rectangle textRectangle, List<String> sampleList,
                              BackgroundType type) {

        boolean supressFill = (getDisplayMode() == Track.DisplayMode.SQUISHED && squishedHeight < 4);

        for (String sample : sampleList) {

            if (coloredLast) {
                g2D.setColor(BAND1_COLOR);
                coloredLast = false;
            } else {
                g2D.setColor(BAND2_COLOR);
                coloredLast = true;
            }
        	if ( selectedSamples.contains(sample) )
        		g2D.setColor(selectedSampleColor);

            if (bandRectangle.intersects(visibleRectangle)) {
                if (!supressFill) {
                    g2D.fillRect(bandRectangle.x, bandRectangle.y, bandRectangle.width, bandRectangle.height);
                }

                if (type == BackgroundType.NAME) {
                    sampleBounds.add(new SampleBounds(bandRectangle.y, bandRectangle.y + bandRectangle.height, sample));
                    if (bandRectangle.height >= 3) {

                        String printName = sample;
                        textRectangle.y = bandRectangle.y + 1;
                        g2D.setColor(getNameColor());
                        GraphicUtils.drawWrappedText(printName, bandRectangle, g2D, false);
                    }

                } else if (type == BackgroundType.ATTRIBUTE) {

                }
            }
            bandRectangle.y += bandRectangle.height;

        }
        return coloredLast;
    }

    public void setRenderID(boolean value) {
        this.renderID = value;
    }


    public boolean getHideFiltered() {
        return hideFiltered;
    }

    public void setHideFiltered(boolean value) {
        this.hideFiltered = value;
    }


    public ColorMode getColorMode() {
        return coloring;
    }

    public void setColorMode(ColorMode mode) {
        this.coloring = mode;
    }


    /**
     * Return popup text for the given position
     *
     * @param chr
     * @param position in genomic coordinates
     * @param y        - pixel position in panel coordinates (i.e. not track coordinates)
     * @param frame
     * @return
     */
    public String getValueStringAt(String chr, double position, int y, ReferenceFrame frame) {

        try {
            Variant variant = (Variant) getFeatureClosest(position, y, frame); //getVariantAtPosition(chr, (int) position, frame);
            if (variant != null) {

            	if ( rejectTooltipOnRange )
            	{
            		if ( position < variant.getStart() )
            		{
            			if ( Math.abs((position - variant.getStart()) / frame.getScale()) > 5 )
            				return null;
            		}
            		else if ( position > variant.getEnd() )
            		{
            			if ( Math.abs((position - variant.getEnd()) / frame.getScale()) > 5 )
            				return null;            			
            		}
            	}
            	else
            	{
	                // If more than ~ 10 pixels distance reject
	                double pixelDist = Math.abs((position - variant.getStart()) / frame.getScale());
	                if (pixelDist > 10) return null;
            	}

                if (y < top + variantBandHeight) {
                    return getVariantToolTip(variant);
                } else {
                    final int sampleCount = sampleBounds.size();
                    if (sampleCount == 0) return null;

                    String sample = null;

                    // Estimate the sample index
                    int firstSampleY = sampleBounds.get(0).top;
                    int idx = Math.min((y - firstSampleY) / getGenotypeBandHeight(), sampleCount - 1);

                    SampleBounds bounds = sampleBounds.get(idx);
                    if (bounds.contains(y)) {
                        sample = bounds.sample;
                    } else if (bounds.top > y) {
                        while (idx > 0) {
                            idx--;
                            bounds = sampleBounds.get(idx);
                            if (bounds.contains(y)) {
                                sample = bounds.sample;
                            }
                        }
                    } else {
                        while (idx < sampleCount - 1) {
                            idx++;
                            bounds = sampleBounds.get(idx);
                            if (bounds.contains(y)) {
                                sample = bounds.sample;
                            }
                        }
                    }

                    if (sample != null)
                        return getSampleToolTip(sample, variant);
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            return null;
        }
    }


    private String getVariantToolTip(Variant variant) {
        String id = variant.getID();

        StringBuffer toolTip = new StringBuffer();
        toolTip.append("Chr:" + variant.getChr());
        toolTip.append("<br>Position:" + variant.getStart());
        toolTip.append("<br>ID: " + id);
        toolTip.append("<br>Reference: " + variant.getReference());
        Set<Allele> alternates = variant.getAlternateAlleles();
        if (alternates.size() > 0) {
            toolTip.append("<br>Alternate: " + alternates.toString());
        }

        toolTip.append("<br>Qual: " + numFormat.format(variant.getPhredScaledQual()));
        toolTip.append("<br>Type: " + variant.getType());
        if (variant.isFiltered()) {
            toolTip.append("<br>Is Filtered Out: Yes</b>");
            toolTip = toolTip.append(getFilterTooltip(variant));
        } else {
            toolTip.append("<br>Is Filtered Out: No</b><br>");
        }
        toolTip.append("<br><b>Alleles:</b>");
        toolTip.append(getAlleleToolTip(variant));

        double af = getAlleleFreq(variant);
        if (af < 0 && variant.getSampleNames().size() > 0) {
            af = getAlleleFraction(variant);
        }
        toolTip.append("<br>Allele Frequency: " + (af >= 0 ? numFormat.format(af) : "Unknown") + "<br>");

        if (variant.getSampleNames().size() > 0) {
            double afrac = getAlleleFraction(variant);
            toolTip = toolTip.append("<br>Minor Allele Fraction: " + numFormat.format(afrac) + "<br>");
        }

        toolTip.append("<br><b>Genotypes:</b>");
        toolTip.append(getGenotypeToolTip(variant) + "<br>");
        toolTip.append(getVariantInfo(variant) + "<br>");
                
        return toolTip.toString();
    }

    protected String getVariantInfo(Variant variant) {
        Set<String> keys = variant.getAttributes().keySet();
        if (keys.size() > 0) {
            String toolTip = "<br><b>Variant Attributes</b>";
            int count = 0;

            // Put AF and GMAF and put at the top, if present
            String k = "AF";
            String afValue = variant.getAttributeAsString(k);
            if (afValue != null && afValue.length() > 0 && !afValue.equals("null")) {
                toolTip = toolTip.concat("<br>" + getFullName(k) + ": " + variant.getAttributeAsString(k));
            }
            k = "GMAF";
            afValue = variant.getAttributeAsString(k);
            if (afValue != null && afValue.length() > 0 && !afValue.equals("null")) {
                toolTip = toolTip.concat("<br>" + getFullName(k) + ": " + variant.getAttributeAsString(k));
            }

            for (String key : keys) {
                count++;

                if (key.equals("AF") || key.equals("GMAF")) continue;

                if (count > MAX_FILTER_LINES) {
                    toolTip = toolTip.concat("<br>....");
                    break;
                }
                toolTip = toolTip.concat("<br>" + getFullName(key) + ": " + variant.getAttributeAsString(key));

            }
            return toolTip;
        }
        return " ";
    }

    private String getSampleInfo(Genotype genotype) {
        Set<String> keys = genotype.getAttributes().keySet();
        if (keys.size() > 0) {
            String tooltip = "<br><b>Sample Attributes:</b>";
            for (String key : keys) {
                try {
                    tooltip = tooltip.concat("<br>" + getFullName(key) + ": " + genotype.getAttributeAsString(key));
                } catch (IllegalArgumentException iae) {
                    tooltip = tooltip.concat("<br>" + key + ": " + genotype.getAttributeAsString(key));
                }
            }
            return tooltip;
        }
        return null;
    }

    private String getAttributeInfo(Genotype genotype) {
    	
    	if ( genotype instanceof VCFGenotype )
    	{
    		String				sample = ((VCFGenotype)genotype).getSampleName();
    		AttributeManager	am = AttributeManager.getInstance();
    		
    		String tooltip = "<br><b>Attributes:</b>";
    		for ( String key : am.getAttributeNames() )
    		{
    			String		value = am.getAttribute(sample, key);
    			if ( value != null )
                    tooltip = tooltip.concat("<br>" + getFullName(key) + ": " + value);
    		}
    	
    		return tooltip;
    	}
    	else
    		return null;
    }

    public void clearSelectedVariant() {
        selectedVariant = null;
    }

    public List<String> getAllSamples() {
        return allSamples;
    }

    public int getSquishedHeight() {
        return squishedHeight;
    }

    public void setSquishedHeight(int squishedHeight) {
        this.squishedHeight = squishedHeight;
    }

    public static enum ColorMode {
        GENOTYPE, METHYLATION_RATE, ALLELE, CUSTOM
    }

    public static enum BackgroundType {
        NAME, ATTRIBUTE, DATA;
    }


    static Map<String, String> fullNames = new HashMap();

    static {
        fullNames.put("AA", "Ancestral Allele");
        fullNames.put("AC", "Allele Count in Genotypes");
        fullNames.put("AN", "Total Alleles in Genotypes");
        fullNames.put("AF", "Allele Frequency");
        fullNames.put("DP", "Depth");
        fullNames.put("MQ", "Mapping Quality");
        fullNames.put("NS", "Number of Samples with Data");
        fullNames.put("BQ", "RMS Base Quality");
        fullNames.put("SB", "Strand Bias");
        fullNames.put("DB", "dbSNP Membership");
        fullNames.put("GQ", "Genotype Quality");
        fullNames.put("GL", "Genotype Likelihoods");  //Hom-ref, het, hom-var break this down into a group
    }

    static String getFullName(String key) {
        return fullNames.containsKey(key) ? fullNames.get(key) : key;
    }


    private String getSampleToolTip(String sample, Variant variant) {
        String id = variant.getID();
        StringBuffer toolTip = new StringBuffer();
        toolTip = toolTip.append("Chr:" + variant.getChr());
        toolTip = toolTip.append("<br>Position:" + variant.getStart());
        toolTip = toolTip.append("<br>ID: " + id + "<br>");
        toolTip = toolTip.append("<br><b>Sample Information:</b>");
        toolTip = toolTip.append("<br>Sample: " + sample);
        toolTip = toolTip.append("<br>Position:" + variant.getStart());

        Genotype genotype = variant.getGenotype(sample);
        if (genotype != null) {
            toolTip = toolTip.append("<br>Bases: " + genotype.getGenotypeString());
            toolTip = toolTip.append("<br>Quality: " + numFormat.format(genotype.getPhredScaledQual()));
            toolTip = toolTip.append("<br>Type: " + genotype.getType());
        }
        if (variant.isFiltered()) {
            toolTip = toolTip.append("<br>Is Filtered Out: Yes</b>");
            toolTip = toolTip.append(getFilterTooltip(variant));
        } else {
            toolTip = toolTip.append("<br>Is Filtered Out: No</b><br>");
        }

        if (genotype != null) {
            toolTip = toolTip.append(getSampleInfo(genotype) + "<br>");
        }

        if (genotype != null) {
        	toolTip = toolTip.append(getAttributeInfo(genotype) + "<br>");
        }

        return toolTip.toString();
    }


    private String getFilterTooltip(Variant variant) {
        Collection filters = variant.getFilters();
        String toolTip = "<br>";
        for (Object filter : filters) {
            toolTip = toolTip.concat("- " + (String) filter + "<br>");
        }

        return toolTip;
    }

    /**
     * Return the allele frequency as annotated with an AF or GMAF attribute.  A value of -1 indicates
     * no annotation (unknown allele frequency).
     */
    public double getAlleleFreq(Variant variant) {
        return variant.getAlleleFreq();
    }

    /**
     * Return the allele fraction for this variant.  The allele fraction is similiar to allele frequency, but is based
     * on the samples in this VCF as opposed to an AF or GMAF annotation.
     * <p/>
     * A value of -1 indicates unknown
     */
    public double getAlleleFraction(Variant variant) {
        return variant.getAlleleFraction();
    }

    public double getAllelePercent(Variant variant) {

        double af = getAlleleFraction(variant);
        return af < 0 ? getAlleleFreq(variant) : af;

    }

    private String getAlleleToolTip(Variant counts) {
        double noCall = counts.getNoCallCount() * 2;
        double aNum = (counts.getHetCount() + counts.getHomRefCount() + counts.getHomVarCount()) * 2;
        double aCount = (counts.getHomVarCount() * 2 + counts.getHetCount()) * 2;

        String toolTip = "<br>No Call: " + (int) noCall;
        toolTip = toolTip.concat("<br>Allele Num: " + (int) aNum);
        toolTip = toolTip.concat("<br>Allele Count: " + (int) aCount);
        return toolTip;
    }

    private String getGenotypeToolTip(Variant counts) {
        int noCall = counts.getNoCallCount();
        int homRef = counts.getHomRefCount();
        int nonVar = noCall + homRef;
        int het = counts.getHetCount();
        int homVar = counts.getHomVarCount();
        int var = het + homVar;

        String toolTip = "<br>Non Variant: " + nonVar;
        toolTip = toolTip.concat("<br> - No Call: " + noCall);
        toolTip = toolTip.concat("<br> - Hom Ref: " + homRef);
        toolTip = toolTip.concat("<br>Variant: " + var);
        toolTip = toolTip.concat("<br> - Het: " + het);
        toolTip = toolTip.concat("<br> - Hom Var: " + homVar);
        return toolTip;
    }


    public IGVPopupMenu getPopupMenu(final TrackClickEvent te) {

        final ReferenceFrame referenceFrame = te.getFrame();
        selectedVariant = null;
        if (referenceFrame != null && referenceFrame.getName() != null) {
            final double position = te.getChromosomePosition();
            Variant f = (Variant) getFeatureClosest(position, te.getMouseEvent().getY(), referenceFrame);
            // If more than ~ 20 pixels distance reject
            if (f != null) {
                double pixelDist = Math.abs((position - f.getStart()) / referenceFrame.getScale());
                if (pixelDist < 20) {
                    selectedVariant = f;
                    IGV.getInstance().doRefresh();
                }
            }
        }
        return new VariantMenu(this, selectedVariant);
    }

    @Override
    public void refreshData(long timestamp) {
        super.refreshData(timestamp);
        setupGroupsFromAttributes();
    }


    /**
     * Return the current state of this object as map of key-value pairs.  Used to store session state.
     * <p/>
     * // TODO -- this whole scheme could probably be more elegantly handled with annotations.
     *
     * @return
     */
    public Map<String, String> getPersistentState() {

        Map<String, String> attributes = super.getPersistentState();
        attributes.put(SessionReader.SessionAttribute.RENDER_NAME.getText(), String.valueOf(renderID));

        ColorMode mode = getColorMode();
        if (mode != null) {
            attributes.put(SessionReader.SessionAttribute.COLOR_MODE.getText(), mode.toString());
        }
        
        String customColor = getColoringCustom();
        if ( customColor != null )
            attributes.put(SessionReader.SessionAttribute.CUSTOM_COLOR.getText(), customColor);        	

        if (squishedHeight != DEFAULT_SQUISHED_GENOTYPE_HEIGHT) {
            attributes.put("SQUISHED_ROW_HEIGHT", String.valueOf(squishedHeight));
        }
        
        if ( variantBandHeight != DEFAULT_VARIANT_BAND_HEIGHT ) {
        	attributes.put("VARIANT_BAND_HEIGHT", String.valueOf(variantBandHeight));
        }

        if ( lastFilterSetting != null && lastFilterSetting.length() != 0 )
        	attributes.put("filterAttributes", lastFilterSetting);        
        
        if ( lastSortComparatorSetting != null && lastSortComparatorSetting.length() != 0 )
        	attributes.put("sortAttributes", lastSortComparatorSetting);        

        return attributes;
    }


    public void restorePersistentState(Map<String, String> attributes) {
        super.restorePersistentState(attributes);

        String rendername = attributes.get(SessionReader.SessionAttribute.RENDER_NAME.getText());
        if (rendername != null) {
            setRenderID(rendername.equalsIgnoreCase("true"));
        }

        String colorModeText = attributes.get(SessionReader.SessionAttribute.COLOR_MODE.getText());
        if (colorModeText != null ) {
            try {
            	// ignore setting to genotype since it is the default anyway (allow auto selection of custom coloring)
            	if ( ApiRequest.isNiu() && colorModeText.equals("GENOTYPE") )
            		;
            	else
            		setColorMode(ColorMode.valueOf(colorModeText));
            }
            catch (Exception e) {
                log.error("Error interpreting display mode: " + colorModeText);
            }
        }

        String customColor = attributes.get(SessionReader.SessionAttribute.CUSTOM_COLOR.getText());
        if (customColor != null) {
            try {
                setColoringCustom(customColor);
            }
            catch (Exception e) {
                log.error("Error interpreting custom color: " + customColor);
            }
        }

        String squishedHeightText = attributes.get("SQUISHED_ROW_HEIGHT");
        if (squishedHeightText != null) {
            try {
                squishedHeight = Integer.parseInt(squishedHeightText);
            }
            catch (Exception e) {
                log.error("Error restoring squished height: " + squishedHeightText);
            }
        }
        
        // DK restore sort
        String variantBandHeightText = attributes.get("VARIANT_BAND_HEIGHT");
        if (variantBandHeightText != null) {
            try {
                variantBandHeight = Integer.parseInt(variantBandHeightText);
            }
            catch (Exception e) {
                log.error("Error restoring variant band height: " + variantBandHeightText);
            }
        }

        
        String 		sortAttributes = attributes.get("sortAttributes");
        if ( sortAttributes != null && sortAttributes.length() != 0 )
        {
        	AttributesComparator	comparator = new AttributesComparator(sortAttributes);
        	
        	this.sortSamples(comparator);
        }
        
        // restore filter
        String		filterAttributes = attributes.get("filterAttributes");
        if ( filterAttributes != null && filterAttributes.length() != 0 )
        {
        	TrackFilter				filter = new TrackFilter(filterAttributes);
        	
        	this.filterSamples(filter, false);
        }
    }


    static class SampleBounds {
        int top;
        int bottom;
        String sample;

        SampleBounds(int top, int bottom, String sample) {
            this.top = top;
            this.bottom = bottom;
            this.sample = sample;
        }

        boolean contains(int y) {
            return y >= top && y <= bottom;
        }
    }
    
    public void sortOnAttribute(String[] attributeNames, boolean[] ascending)
    {
    	AttributesComparator compare = new AttributesComparator(attributeNames, ascending);
        this.sortSamples(compare);
        IGV.getInstance().getContentPane().repaint();
    }


	public String getLastSortComparatorSetting() {
		return lastSortComparatorSetting;
	}


	public String getLastFilterSetting() {
		return lastFilterSetting;
	}


	public String getColoringCustom() {
		return coloringCustom;
	}


	public void setColoringCustom(String coloringCustom) {
		this.coloringCustom = coloringCustom;
	}
	
	public void clearSelectedSamples()
	{
		selectedSamples.clear();
		lastSelectedSampleIndex = -1;	
	}
	
	public void setSelectedSample(String sample)
	{
		selectedSamples.clear();
		selectedSamples.add(sample);
		lastSelectedSampleIndex = allSamples.indexOf(sample);
	}
	
	public void addSelectedSample(String sample)
	{
		selectedSamples.add(sample);
		lastSelectedSampleIndex = allSamples.indexOf(sample);
	}
	
	public void toggleSelectedSample(String sample)
	{
		if ( selectedSamples.contains(sample) )
			selectedSamples.remove(sample);
		else
			selectedSamples.add(sample);
		
		lastSelectedSampleIndex = allSamples.indexOf(sample);
	}
	
	public void extendSelectedSample(String sample)
	{
		// no selection?
		if ( selectedSamples.size() == 0 )
			setSelectedSample(sample);
		else
		{
			int			i = allSamples.indexOf(sample);
			if ( i < 0 )
				return;
			
			selectedSamples.clear();
			
			int		min = Math.min(i, lastSelectedSampleIndex);
			int		max = Math.max(i, lastSelectedSampleIndex);
			
			for ( int i1 = min ; i1 <= max ; i1++ )
				selectedSamples.add(allSamples.get(i1));			
		}
	}

    public String getSampleAtY(int y) {

        int sampleCount = allSamples.size();
        if (getDisplayMode() == Track.DisplayMode.COLLAPSED || sampleCount == 0) {
            return null; // no samples (displayed)
        } else {
        	
        	// ignoring groups ...
        	y -= variantBandHeight;
        	if ( y < 0 )
        		return null;
        	int			index = y /= getGenotypeBandHeight();
        	if ( index < allSamples.size() )
        		return allSamples.get(index);
        	else
        		return null;
        }
    }
    
    public void setSelected(boolean selected, MouseEvent e) 
    {
    	super.setSelected(selected);
    	
    	if ( selected )
    	{
    		String		sample = getSampleAtY(e.getY());
    		
    		if ( sample != null )
    			setSelectedSample(sample);
    	}
    }


	public boolean isLastShowAll() {
		return lastShowAll;
	}


	public void removed()
	{
		if ( source instanceof TribbleFeatureSource )
		{
			((TribbleFeatureSource)source).deregisterTrack(this);
		}
	}
	
    public Map<String, String> getTrackCustomColors()
    {
    	Map<String, String>			colors = new LinkedHashMap<String, String>();
    	
    	@SuppressWarnings("rawtypes")
		FeatureSource		source = getSource();
    	if ( source instanceof TribbleFeatureSource )
    	{
    		TribbleFeatureSource		tSource = (TribbleFeatureSource)source;
    		if ( tSource.getHeader() instanceof VCFHeader )
    		{
    			VCFHeader		header = (VCFHeader)tSource.getHeader();
    			
    			for ( VCFHeaderLine line : header.getMetaData() )
    			{
    				if ( line instanceof VCFFormatHeaderLine )
    				{
    					try
    					{
	    					VCFFormatHeaderLine	format = (VCFFormatHeaderLine)line;
	    					
	    					if ( format.getID().startsWith("CO") )
	    					{
	    						colors.put(format.getID(), format.getDescription());
	    					}
    					}
    					catch (Throwable e)
    					{
    						log.warn("caught exception during VCFFormatHeaderLine.getID(): " + e.getMessage());
    					}
    					
    				}
    				else if ( line.getKey().equals("customcolor") )
    				{
    					String[]		toks = line.getValue().split("\\|");

    					colors.put(toks[0], toks[1]);
    				}

    			}
    		}
    		
    	}
    	
    	return colors;
    }


	public int getVariantBandHeight() {
		return variantBandHeight;
	}


	public void setVariantBandHeight(int variantBandHeight) {
		this.variantBandHeight = variantBandHeight;
	}
    
}


