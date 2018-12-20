package org.broad.igv.variant;

import org.apache.log4j.Logger;

public class TrackedObject {

    private static Logger log = Logger.getLogger(TrackedObject.class);

	
	public TrackedObject()
	{
		super();
		log.info("**CREATED** " + this.getClass().getName() + " " + this);
	}

	@Override
	protected void finalize() throws Throwable 
	{
		try
		{
			log.info("**FINALIZED** " + this.getClass().getName() + " " + this);			
		}
		finally {
			super.finalize();
		}
	}
	
	
}
