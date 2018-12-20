package org.broad.igv.track;

import java.util.List;

public interface ExternalToolProvider {

	String		invokeTool(List<AbstractTrack> tracks, String toolName, String providerParam, String locus) throws Exception;
}
