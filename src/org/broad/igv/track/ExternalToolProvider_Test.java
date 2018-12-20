package org.broad.igv.track;

import java.io.File;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

public class ExternalToolProvider_Test implements ExternalToolProvider {
	
    static private Logger 						log = Logger.getLogger(ExternalToolProvider_Test.class);

	public String invokeTool(List<AbstractTrack> tracks, String toolName, String providerParam, String locus) throws Exception
	{
		log.debug("Test provider: tracks: " + tracks + ", toolName: " + toolName + ", providerParam: " + providerParam + ", locus:" + locus);
		
		String				value = null;
		
		// provider param is the index of the track to copy and return
		int					index = Math.min(tracks.size(), Integer.parseInt(providerParam));
		String				sourceTrackKey = tracks.get(index).getResourceLocator().getPath();
		String				suffix = ".tmp";
		int					extensionIndex;
		if ( (extensionIndex = sourceTrackKey.lastIndexOf('.')) > 0 )
			suffix = sourceTrackKey.substring(extensionIndex);
		File				destinationTrackKey = File.createTempFile("ExternalToolProvider_Test", suffix);

		// copy the file
		FileUtils.copyFile(new File(sourceTrackKey), destinationTrackKey);
		
		value = destinationTrackKey.getAbsolutePath();
		log.debug("Test provider: value: " + value);
		
		return value;
	}
}
