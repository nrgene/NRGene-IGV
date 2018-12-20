package org.broad.igv.track;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

public class SoftAttributeProvider_Shell implements SoftAttributeProvider {
	
    static private Logger 						log = Logger.getLogger(SoftAttributeProvider_Shell.class);

	public String getAttribute(AbstractTrack track, String attrName, String providerParam, String locus) throws Exception
	{
		String			trackKey = track.getResourceLocator().getPath();
		
		log.debug("Shell provider: trackKey: " + trackKey + ", attrName: " + attrName + ", providerParam: " + providerParam + ", locus:" + locus);
		
		String				value = null;
		if ( trackKey != null && providerParam != null )
		{
			String			cmd = String.format("%s \"%s\" \"%s\" \"%s\"", providerParam, locus, trackKey, attrName);
			log.debug("cmd: " + cmd);
			
			Runtime 		run = Runtime.getRuntime();
			Process 		pr = run.exec(cmd);
			pr.waitFor();
			BufferedReader 	buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			List<String>	lines = new LinkedList<String>();
			String			line;
			while ( (line = buf.readLine()) !=null ) 
			{
				lines.add(line);
			}
			
			if ( lines.size() > 0 )
				value = lines.get(0);
			
			pr.destroy();
		}

		log.debug("Shell provider: value: " + value);
		
		return value;
	}
}
