package org.broad.tribble;

import java.io.File;

import org.broad.igv.nrgene.api.ApiRequest;

/**
 * Common, tribble wide constants and static functions
 */
public class Tribble {
    private Tribble() { } // can't be instantiated

    public final static String STANDARD_INDEX_EXTENSION = ".idx";

    public static String indexFile(String filename) {
    	if ( !ApiRequest.isNiu() )
    		return filename + STANDARD_INDEX_EXTENSION;
    	else
    		return ApiRequest.buildIndexPath(filename, STANDARD_INDEX_EXTENSION);
    }

    public static File indexFile(File file) {
        return new File(file.getAbsoluteFile() + STANDARD_INDEX_EXTENSION);
    }
    
    static private String addIndex(String path)
    {
    	int			i = path.indexOf('?');
    	
    	if ( i < 0 )
    		return path + STANDARD_INDEX_EXTENSION;
    	else
    		return path.substring(0, i) + STANDARD_INDEX_EXTENSION + path.charAt(i) + STANDARD_INDEX_EXTENSION;
    }
}
