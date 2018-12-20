package org.broad.igv.util;

import java.io.File;
import java.util.Random;

public class FilenameUtils {

	public static String userifyFilename(String name) 
	{
		String		uname = System.getProperty("user.name");
		
		int			rand = (new Random(System.currentTimeMillis())).nextInt();
		name = Integer.toString(Math.abs(rand)) + "_" + name;
		
		if ( uname != null )
			name = uname + "_" + name;
		
		return name;
	}

	public static File userifyFolder(File folder) 
	{
		String		path = folder.getAbsolutePath();
		
		String		uname = System.getProperty("user.name");
		if ( uname != null )
			path = path + "/" + uname;
		
		int			rand = (new Random(System.currentTimeMillis())).nextInt();
		path = path + "/" + Integer.toString(Math.abs(rand));
		
		folder = new File(path);
		folder.mkdirs();
		
		return folder;
	}

}
