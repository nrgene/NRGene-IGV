package org.broad.igv.log;

import org.apache.log4j.FileAppender;

public class WinPathFileAppender extends FileAppender {
	
	String		winfile;

	public WinPathFileAppender()
	{
		super();
	}
	
	@Override
	public void activateOptions() 
	{
		if ( System.getProperty("os.name").toLowerCase().indexOf("win") >= 0 )
			setFile(getWinfile());
		
		super.activateOptions();
	}
	
	public String getWinfile() {
		return winfile;
	}

	public void setWinfile(String winfile) {
		this.winfile = winfile;
	}
}
