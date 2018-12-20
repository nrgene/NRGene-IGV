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

package org.broad.igv.variant;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.broad.igv.PreferenceManager;
import org.broad.igv.track.RenderContext;
import org.broad.igv.track.Track;
import org.broad.igv.ui.FontManager;
import org.broad.igv.util.ColorUtilities;

/**
 * User: Jesse Whitworth
 * Date: Jul 16, 2010
 */

public class VariantRenderer { //extends FeatureRenderer {

    private static Logger log = Logger.getLogger(VariantRenderer.class);

    private static float alphaValue = 0.2f;
    public static Color colorHomRef = new Color(235, 235, 235);
    public static Color colorHomRefAlpha = ColorUtilities.getCompositeColor(colorHomRef.getColorComponents(null), alphaValue);
    public static Color colorHomVar = new Color(0, 245, 255);
    public static Color colorHomVarAlpha = ColorUtilities.getCompositeColor(colorHomVar.getColorComponents(null), alphaValue);
    public static Color colorHet = Color.blue.brighter();  //new Color(107, 30, 115); //Color.blue;
    public static Color colorHetAlpha = ColorUtilities.getCompositeColor(colorHet.getColorComponents(null), alphaValue);
    public static Color colorNoCall = Color.white;
    public static Color colorNoCallAlpha = ColorUtilities.getCompositeColor(colorNoCall.getColorComponents(null), alphaValue);
    public static Color colorError = Color.black;
    public static Color colorErrorAlpha = ColorUtilities.getCompositeColor(colorError.getColorComponents(null), alphaValue);
    private static Color whiteTransparent = new Color(255,255,255,0);

    
    public static final Color colorAlleleBand = Color.red;
    public static Color colorAlleleBandAlpha = ColorUtilities.getCompositeColor(colorAlleleBand.getColorComponents(null), alphaValue);
    public static final Color colorAlleleRef = Color.gray;
    public static Color colorAlleleRefAlpha = ColorUtilities.getCompositeColor(colorAlleleRef.getColorComponents(null), alphaValue);
    private static final Color blue = new Color(0, 0, 220);
    public static Color blueAlpha = ColorUtilities.getCompositeColor(blue.getColorComponents(null), alphaValue);

    private static int variantWidth = 3;
    static Map<Character, Color> nucleotideColors = new HashMap<Character, Color>();
    private static final Color DARK_GREEN = new Color(30, 120, 30);
    
    // DK color counting for summary band
    private Map<Color, Integer>		bandColors = new LinkedHashMap<Color, Integer>();
    private boolean					bandColorsFloating = true;
    private int						bandColorsWidth = 0;
    
    private boolean					renderBandColors = true;
    private boolean					renderFeatures = true;
    private boolean					renderVariantBand = true;
    
    private static String			LENGTH_FIELD = "LN";

    private VariantTrack track;

    static {
        nucleotideColors.put('A', Color.GREEN);
        nucleotideColors.put('a', Color.GREEN);
        nucleotideColors.put('C', Color.BLUE);
        nucleotideColors.put('c', Color.BLUE);
        nucleotideColors.put('T', Color.RED);
        nucleotideColors.put('t', Color.RED);
        nucleotideColors.put('G', new Color(242, 182, 65));
        nucleotideColors.put('g', new Color(242, 182, 65));
        nucleotideColors.put('N', colorAlleleRef);
        nucleotideColors.put('n', colorAlleleRef);
        nucleotideColors.put('.', colorAlleleRef);
        nucleotideColors.put(null, Color.BLACK);
        
        // load dynamic colors
        PreferenceManager		pm = PreferenceManager.getInstance();
        colorHomRef = pm.getAsColor(PreferenceManager.VR_COLOR_HOM_REF, colorHomRef);
        colorHomRefAlpha = pm.getAsColor(PreferenceManager.VR_COLOR_HOM_REF_FILTERED, ColorUtilities.getCompositeColor(colorHomRef.getColorComponents(null), alphaValue));
        colorHomVar = pm.getAsColor(PreferenceManager.VR_COLOR_HOM_VAR, colorHomVar);
        colorHomVarAlpha = pm.getAsColor(PreferenceManager.VR_COLOR_HOM_VAR_FILTERED, ColorUtilities.getCompositeColor(colorHomVar.getColorComponents(null), alphaValue));
        colorHet = pm.getAsColor(PreferenceManager.VR_COLOR_HET, colorHet);
        colorHetAlpha = pm.getAsColor(PreferenceManager.VR_COLOR_HET_FILTERED, ColorUtilities.getCompositeColor(colorHet.getColorComponents(null), alphaValue));
        colorNoCall = pm.getAsColor(PreferenceManager.VR_COLOR_NO_CALL, colorNoCall);
        colorNoCallAlpha = pm.getAsColor(PreferenceManager.VR_COLOR_NO_CALL_FILTERED, ColorUtilities.getCompositeColor(colorNoCall.getColorComponents(null), alphaValue));
        colorError = pm.getAsColor(PreferenceManager.VR_COLOR_ERROR, colorError);
        colorErrorAlpha = pm.getAsColor(PreferenceManager.VR_COLOR_ERROR_FILTERED, ColorUtilities.getCompositeColor(colorError.getColorComponents(null), alphaValue));
    }

    public VariantRenderer(VariantTrack track) {
        this.track = track;
    }


    public void renderVariantBandByColors(Variant variant,
            Rectangle bandRectangle,
            int pX0, int dX,
            RenderContext context,
            boolean hideFiltered,
            Rectangle visibleRectangle) 
    {
    	if ( !renderBandColors )
    		return;
    	
        final int bottomMargin = 0;
        final int topMargin = 3;
        
        final int bottomY = bandRectangle.y + bandRectangle.height - bottomMargin;
        
        final int barHeight = bandRectangle.height - topMargin - bottomMargin;

        final int 		total = getTotalBandColorCount();
        float			y = bottomY - barHeight;
        for ( Color color : getBandColors() )
        {
        	float		percentage = (float)getBandColorCount(color) / total;
        	float		height = percentage * barHeight;
        			
            Graphics2D g = context.getGraphic2DForColor(color);
            
            //System.out.println("trans: " + g.getTransform());
            g.fillRect(pX0, Math.round(y), Math.max(dX, bandColorsWidth), Math.round(height));
            y += height;
        }
    }
    
    public void renderVariantBand(Variant variant,
                                  Rectangle bandRectangle,
                                  int pX0, int dX,
                                  RenderContext context,
                                  boolean hideFiltered,
                                  Rectangle visibleRectangle) {

    	
    	if ( bandColors.size() > 0 )
    	{
    		renderVariantBandByColors(variant, bandRectangle, pX0, dX, context, hideFiltered, visibleRectangle);
    		return;
    	}

    	if ( !renderVariantBand )
    		return;
    	
    	final int bottomMargin = 0;
        final int topMargin = 3;

        final int bottomY = bandRectangle.y + bandRectangle.height - bottomMargin;

        final int barHeight = bandRectangle.height - topMargin - bottomMargin;

        final boolean filtered = variant.isFiltered();
        final Color alleleColor = filtered ? colorAlleleBandAlpha : colorAlleleBand;

        final double allelePercent = Math.min(1, track.getAllelePercent(variant));
        final int alleleBarHeight;
        final int remainderHeight;

        final Color refColor;
        if (allelePercent <= 0) {
            alleleBarHeight = 0;
            remainderHeight = barHeight;
            refColor = filtered ? colorAlleleRefAlpha : colorAlleleRef;
        } else {
            alleleBarHeight = (int) (allelePercent * barHeight);
            remainderHeight = barHeight - alleleBarHeight;

            refColor = filtered ? blueAlpha : blue;
        }

        Graphics2D g = context.getGraphic2DForColor(alleleColor);
        g.fillRect(pX0, bottomY - alleleBarHeight, dX, alleleBarHeight);

        g = context.getGraphic2DForColor(refColor);
        g.fillRect(pX0, bottomY - alleleBarHeight - remainderHeight, dX, remainderHeight);
        
    	
    }


    public void renderGenotypeBandSNP(Variant variant, RenderContext context, Rectangle bandRectangle, int pX0, int dX,
                                      String sampleName, VariantTrack.ColorMode coloring, boolean hideFiltered, boolean colorAccountingOnly) {

    	if ( !renderFeatures )
    		return;
    	
        int pY = (int) bandRectangle.getY();
        int dY = (int) bandRectangle.getHeight();

        int tOffset = 6;
        int bOffset = 8;

        boolean isFiltered = variant.isFiltered() && hideFiltered;

        Genotype genotype = variant.getGenotype(sampleName);
        if (genotype == null) {
            log.error("Now what?");
        } else {
            Color b1Color = Color.gray;
            Color b2Color = Color.gray;
            char b1 = ' ';
            char b2 = ' ';
            //Assign proper coloring
            switch (coloring) {
                case GENOTYPE:

                    b1Color = getGenotypeColor(genotype, isFiltered);
                    b2Color = b1Color;
                    break;

                case ALLELE:
                    final List<Allele> alleleList = genotype.getAlleles();
                    if (alleleList.size() > 0) {
                        b1 = getFirstBase(alleleList.get(0));
                        b1Color = nucleotideColors.get(b1);
                    }
                    if (alleleList.size() > 1) {
                        b2 = getFirstBase(alleleList.get(1));
                        b2Color = nucleotideColors.get(b2);
                    }
                    break;
                case METHYLATION_RATE:

                    final Double goodBaseCount = genotype.getAttributeAsDouble("GB");

                    final Double value = genotype.getAttributeAsDouble("MR");
                    if (goodBaseCount < 10 || value == null) {
                        b1Color = colorNoCall;
                        b2Color = b1Color;

                    } else {
                        float mr = (float) value.doubleValue();
                        //   System.out.printf("position %d methylation-rate: %f%n", variant.getStart(), mr);
                        mr /= 100f;
                        b1Color = convertMethylationRateToColor(mr);
                        b2Color = b1Color;
                    }
                    break;

                case CUSTOM:

                	if ( isFiltered || ((b1Color = getCustomColor(genotype)) == null) )
                		b1Color = getGenotypeColor(genotype, isFiltered);
                    if (b1Color.equals(Color.white)) { // white color actually means we do not want
                                                       // to draw a rectangle, i.e. use "transparent" color
                      b1Color = whiteTransparent;
                    }
                    b2Color = b1Color;
                    break;

                default:
                    b1Color = colorNoCall;
                    b2Color = b1Color;
            }


            int y0 = track.getDisplayMode() == Track.DisplayMode.EXPANDED ? pY + 1 : pY;
            int h = Math.max(1, track.getDisplayMode() == Track.DisplayMode.EXPANDED ? dY - 2 : dY);

            // override width?
            dX = adjustLength(dX, genotype, context, pX0, variant);
            
            addBandColor(b1Color, dX);
            if ( colorAccountingOnly )
            	return;
            
            Graphics2D g = (Graphics2D) context.getGraphics().create();

            if (dX >= 10) {
                if (dY > 24) {
                    Font f = FontManager.getFont(Font.BOLD, Math.min(dX, 12));
                    g.setFont(f);
                } else if (dY > 18) {
                    Font f = FontManager.getFont(Font.BOLD, Math.min(dX, 8));
                    tOffset = 4;
                    bOffset = 5;
                    g.setFont(f);
                }
            }

            if (coloring == VariantTrack.ColorMode.GENOTYPE || coloring == VariantTrack.ColorMode.CUSTOM) {
                g.setColor(b1Color);
                g.fillRect(pX0, y0, dX, h);
            } else {
                // Color by allele
                g.setColor(b1Color);
                g.fillRect(pX0, y0, (dX / 2), h);
                g.setColor(b2Color);
                g.fillRect(pX0 + (dX / 2), y0, (dX / 2), h);
            }


            if ((dX >= 10) && (dY >= 18)) {
                if (b1Color == Color.blue) {
                    g.setColor(Color.white);
                } else {
                    g.setColor(Color.black);
                }
                drawCenteredText(g, new char[]{b1}, pX0, pY - tOffset, dX, dY);
                drawCenteredText(g, new char[]{b2}, pX0, pY + (dY / 2) - bOffset, dX, dY);
            }
        }
//        g.dispose();
    }

    static public int adjustLength(int dX, Genotype genotype, RenderContext context, int x, Variant variant) 
    {
    	int			org_dX = dX;
    	
        String	lengthAttr = genotype.getAttributeAsString(LENGTH_FIELD);
        if ( !StringUtils.isEmpty(lengthAttr) && !".".equals(lengthAttr) )
        {
        	double	length = Double.parseDouble(lengthAttr);
        	if ( length >= 0 )
        	{
        		dX = (int)Math.max(dX, Math.ceil(length / context.getScale()));
        	
        		if ( log.isDebugEnabled() )
        			log.debug("start " + variant.getStart() + " x " + x + " dX " + org_dX + " -> " + dX);
        	}
        }
        
        return dX;
	}


	private Color getCustomColor(Genotype genotype) 
    {
    	String		field = track.getColoringCustom();
    	String		co = genotype.getAttributeAsString(field);
    	if ( co != null && co.length() >= 5 )
    		return ColorUtilities.stringToColor(co);
    	else
    		return Color.LIGHT_GRAY;
	}


	private Color convertMethylationRateToColor(float mr) {
        Color color;

        if (mr >= .25) {
            return Color.getHSBColor((mr - .25f) * (1f / 0.75f), 1, 1);
        } else {
            // use a light grey between 0 and 0.25 brightness to indicate moderate methylation at the site.
            return new Color(1f - mr, 1f - mr, 1f - mr);
        }


    }

    public char getFirstBase(Allele allele) {
        byte[] bases = allele.getBases();
        if (bases.length > 0) {
            return (char) bases[0];
        } else {
            return '.';
        }
    }

    public Color getGenotypeColor(Genotype genotype, boolean isFiltered) {
    	
    	try
    	{
	    	String				ge = genotype.getAttributeAsString("GE");
	    	if ( ge != null && ge.length() > 0 && ge.charAt(0) != '.' && Double.parseDouble(ge) >= 1 )
	            return isFiltered ? colorErrorAlpha : colorError;
    	} catch (NumberFormatException e)
    	{
    		e.printStackTrace();
    	}
    	
        if (genotype.isNoCall()) {
            return isFiltered ? colorNoCallAlpha : colorNoCall;
        } else if (genotype.isHomRef()) {
            return isFiltered ? colorHomRefAlpha : colorHomRef;
        } else if (genotype.isHomVar()) {
            return isFiltered ? colorHomVarAlpha : colorHomVar;
        } else if (genotype.isHet()) {
            return isFiltered ? colorHetAlpha : colorHet;
        }
        return Color.white;
    }

    private void drawCenteredText(Graphics2D g, char[] chars, int x, int y,
                                  int w, int h) {

        // Get measures needed to center the message
        FontMetrics fm = g.getFontMetrics();

        // How many pixels wide is the string
        int msg_width = fm.charsWidth(chars, 0, 1);

        // How far above the baseline can the font go?
        int ascent = fm.getMaxAscent();

        // How far below the baseline?
        int descent = fm.getMaxDescent();

        // Use the string width to find the starting point
        int msgX = x + w / 2 - msg_width / 2;

        // Use the vertical height of this font to find
        // the vertical starting coordinate
        int msgY = y + h / 2 - descent / 2 + ascent / 2;

        g.drawChars(chars, 0, 1, msgX, msgY);

    }


	public void resetBandColors() {
		bandColors.clear();
		bandColorsWidth = 0;
	}
	
	private void addBandColor(Color color, int colorWidth)
	{
		Integer		count = bandColors.get(color);
		
		bandColors.put(color, count != null ? (count + 1) : 1);
		bandColorsWidth = Math.max(bandColorsWidth, colorWidth);
	}
	
	private List<Color> getBandColors()
	{
		List<Color>		colors = new LinkedList<Color>(bandColors.keySet());
		
		Collections.sort(colors, new Comparator<Color>() {

			@Override
			public int compare(Color o1, Color o2) {
				
				int		code1 = getColorSortCode(o1);
				int		code2 = getColorSortCode(o2);
				
				if ( code1 == 0 && code2 == 0 )
					return o2.getRGB() - o1.getRGB();
				else
					return code2 - code1;
			}
		});
		
		return colors;
	}
	
	private int getColorSortCode(Color c)
	{
		if ( c.equals(colorHomRef) )
			return 10;
		else if ( c.equals(colorHomRefAlpha) )
			return 9;
		else if ( c.equals(colorHet) )
			return 8;
		else if ( c.equals(colorHetAlpha) )
			return 7;
		else if ( c.equals(colorHomVar) )
			return 6;
		else if ( c.equals(colorHomVarAlpha) )
			return 5;
		else if ( c.equals(colorNoCall) )
			return 4;
		else if ( c.equals(colorNoCallAlpha) )
			return 3;
		else if ( c.equals(colorError) )
			return 2;
		else if ( c.equals(colorErrorAlpha) )
			return 1;
		else
			return 0;
	}
	
	private int getTotalBandColorCount()
	{
		int		total = 0;
		
		for ( Integer count : bandColors.values() )
			total += count;
		
		return total;
	}
	
	private int getBandColorCount(Color color)
	{
		return bandColors.get(color);
	}
}
