package org.broad.igv.renderer;

import java.awt.Color;
import java.util.LinkedHashMap;
import java.util.Map;

import com.meapsoft.gui.ColorMap;

public class JetColorScale extends AbstractColorScale {

    double 		minValue;
    double 		maxValue;
    double		range;
    
    int	 		steps;
    boolean		logscale = false;
    ColorMap	colorMap;
    
    static private Map<Integer, ColorMap>	colorMaps = new LinkedHashMap<Integer, ColorMap>();

    public JetColorScale(float x1, float x2, boolean logscale) {
    	this(x1, x2, 64, logscale);
    }
    
    public JetColorScale(float x1, float x2) {
    	this(x1, x2, 64, false);
    }
    
    public JetColorScale(float x1, float x2, int steps, boolean logscale) {

        minValue = Math.min(x1, x2);
        maxValue = Math.max(x1, x2);
        this.steps = steps;
        this.logscale = logscale;
        
        if ( logscale )
        {
        	minValue = Math.exp(minValue);
			maxValue = Math.exp(maxValue);
        }
        
        range = maxValue - minValue;
        
        colorMap = colorMaps.get(steps);
        if ( colorMap == null )
        	colorMaps.put(steps, colorMap = ColorMap.getJet(steps));
    }
     
    public Color getColor(float value) 
    {
    	if ( logscale )
    		value = (float)Math.exp(value);
    	
    	// check range
    	if ( value < minValue && value > maxValue )
    		return super.getColor(value);
    	
    	// translate value to color index
    	int		index = 0;
    	if ( range > 0 )
    		index = (int)(((value - minValue) / range) * steps);
		index = Math.max(0, index);
		index = Math.min(steps - 1, index);
    	
    	// get color rgb;
    	int		rgb = colorMap.getColor(index);
    	
    	// convert to color
    	return new Color(rgb);
    }
    
    // TODO -- implement this so the class can be serialized
    public String asString() {
        return null;
    }

    public boolean isDefault() {
        return false;  
    }


}
