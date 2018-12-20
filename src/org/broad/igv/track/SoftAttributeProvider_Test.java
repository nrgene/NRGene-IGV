package org.broad.igv.track;

import java.io.File;

import org.apache.log4j.Logger;

public class SoftAttributeProvider_Test implements SoftAttributeProvider {
	
    static private Logger 						log = Logger.getLogger(SoftAttributeProvider_Test.class);

	public String getAttribute(AbstractTrack track, String attrName, String providerParam, String locus) throws Exception
	{
		String		trackKey = track.getResourceLocator().getPath();
		
		log.debug("Test provider: trackKey: " + trackKey + ", attrName: " + attrName + ", providerParam: " + providerParam + ", locus:" + locus);
		
		String				value = null;
		if ( trackKey != null )
		{
			if ( "length".equals(providerParam) )
				value = Integer.toString(trackKey.length());
			else if ( "size".equals(providerParam) )
					value = Long.toString((new File(trackKey)).length());
			else if ( "locus".equals(providerParam) )
				value = locus;
			else
				value = "test";
		}

		log.debug("Test provider: value: " + value);
		
		return value;
	}
}
