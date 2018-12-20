package org.broad.igv.track;

import java.awt.Color;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.broad.igv.feature.BasicFeature;
import org.broad.tribble.Feature;

public class SoftAttributeProvider_ByFeatures implements SoftAttributeProvider {
	
    static private Logger 						log = Logger.getLogger(SoftAttributeProvider_ByFeatures.class);

	public String getAttribute(AbstractTrack track, String attrName, String providerParam, String locus) throws Exception
	{
		log.debug("Test provider: track: " + track + ", attrName: " + attrName + ", providerParam: " + providerParam + ", locus:" + locus);
		
		// works only on feature tracks
		if ( !(track instanceof FeatureTrack) )
			return null;
		FeatureTrack		featureTrack = (FeatureTrack)track;
		
		// parse locus
		if ( locus == null || locus.indexOf(':') < 0 )
			return null;
		String[]		locusToks = locus.split(":");
		String			chr = locusToks[0];
		String[]		locusToks1 = locusToks[1].split("-");
		int				start = Integer.parseInt(locusToks1[0].replace(",", ""));
		int				end = Integer.parseInt(locusToks1[Math.max(1, locusToks1.length - 1)].replace(",", ""));
		
		// get packed features
		PackedFeatures<Feature> 	pf;
		@SuppressWarnings("unchecked")
		long						startMilli = System.currentTimeMillis();
		Iterator<Feature> iter = featureTrack.getSource().getFeatures(chr, start, end);
		log.debug("iter: " + (System.currentTimeMillis() - startMilli));
        if (iter == null) 
        	pf = new PackedFeatures<Feature>(chr, start, end);
        else
        	pf = new PackedFeatures<Feature>(chr, start, end, iter, track.getName());
		log.debug("new PackedFeatures: " + (System.currentTimeMillis() - startMilli));
		
        // make value string from packed features
        List<Feature>		features = pf.getFeatures();
        StringBuilder		textEncoding = new StringBuilder();
        int					featureLimit = Integer.parseInt(providerParam);
        for ( Feature feature : features )
        {
        	if ( featureLimit-- <= 0 )
        		break;
        	
        	String		content = "?";
        	
        	if ( feature instanceof BasicFeature )
        	{
        		BasicFeature	bf = (BasicFeature)feature;
        		Color			color = bf.getColor();
        		if ( color == null && bf.getExonCount() > 0 )
        			color = bf.getExons().get(0).getColor();
        		if ( color == null )
        			color = Color.BLACK;
 
        		content = String.format("%03d,%03d,%03d", color.getRed(), color.getGreen(), color.getBlue());
        	}
        	
        	textEncoding.append(String.format("%020d:%-20s:%020d ", feature.getStart(), content, feature.getEnd()));
        }
        
		String				value = textEncoding.toString();
		
		log.debug("ByFeatures provider: value: " + value);
		
		return value;
	}
}
