package org.broad.igv.track;

import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

public class ExternalToolConf {
	
    static private Logger 						log = Logger.getLogger(ExternalToolConf.class);
	
    private String					key;
	private String					name;
	private String					className;
	private String					providerParam;
	
	private boolean					blocking;
	private boolean					returnsTrack;
	
	private ExternalToolProvider	provider;

	public ExternalToolConf(Properties conf, String key) 
	{
		this.key = key;
		name = conf.getProperty(key, key);
		className = conf.getProperty(key + ".className", "org.broad.igv.track.ExternalToolProvider_Test");
		providerParam = conf.getProperty(key + ".providerParam", "");
		blocking = Boolean.parseBoolean(conf.getProperty(key + ".blocking", "true"));
		returnsTrack = Boolean.parseBoolean(conf.getProperty(key + ".returnsTrack", "true"));
		
		try
		{
			Class<?>		clazz = Class.forName(className);
			
			provider = (ExternalToolProvider)clazz.newInstance();
		}
		catch (Throwable e)
		{
			log.error("failed to load provider from key: " + key, e);
		}

	}

	public String invokeTool(List<AbstractTrack> tracks, String toolName, String locus) {
		
		// let provider do the hard work
		try {
			return provider.invokeTool(tracks, toolName, providerParam, locus);
		} catch (Exception e) {
			log.warn("provider throw exception:", e);
			return null;
		}
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getProviderParam() {
		return providerParam;
	}

	public void setProviderParam(String providerParam) {
		this.providerParam = providerParam;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public boolean isBlocking() {
		return blocking;
	}

	public void setBlocking(boolean blocking) {
		this.blocking = blocking;
	}

	public boolean isReturnsTrack() {
		return returnsTrack;
	}

	public void setReturnsTrack(boolean returnsTrack) {
		this.returnsTrack = returnsTrack;
	}

	public ExternalToolProvider getProvider() {
		return provider;
	}

	public void setProvider(ExternalToolProvider provider) {
		this.provider = provider;
	}

	
	

}
