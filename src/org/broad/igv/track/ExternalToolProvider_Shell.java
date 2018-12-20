package org.broad.igv.track;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

public class ExternalToolProvider_Shell implements ExternalToolProvider {
	
    static private Logger 						log = Logger.getLogger(ExternalToolProvider_Shell.class);

    public String invokeTool(List<AbstractTrack> tracks, String toolName, String providerParam, String locus) throws Exception
	{
		log.debug("Test provider: tracks: " + tracks + ", toolName: " + toolName + ", providerParam: " + providerParam + ", locus:" + locus);
		
		String				value = null;
		if ( tracks != null && tracks.size() > 0 && providerParam != null )
		{
			String			cmd = String.format("%s \"%s\" \"%s\" \"%s\"", providerParam, locus, tracks.size(), toolName);
			for ( AbstractTrack track : tracks )
				cmd += String.format(" %s", track.getResourceLocator().getPath());
			log.debug("cmd: " + cmd);
			
			Runtime 		run = Runtime.getRuntime();
			Process 		pr = run.exec(cmd);
			int				result = pr.waitFor();
			
			BufferedReader 	buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			List<String>	lines = new LinkedList<String>();
			String			line;
			while ( (line = buf.readLine()) !=null ) 
			{
				lines.add(line);
			}
			
			buf = new BufferedReader(new InputStreamReader(pr.getErrorStream()));
			List<String>	errors = new LinkedList<String>();
			while ( (line = buf.readLine()) !=null ) 
			{
				errors.add(line);
			}
			
			if ( lines.size() > 0 )
				value = lines.get(0);
		}

		log.debug("Shell provider: value: " + value);
		
		return value;
	}
}
