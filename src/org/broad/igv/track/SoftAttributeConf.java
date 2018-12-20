package org.broad.igv.track;

import java.util.Properties;

import org.apache.log4j.Logger;

public class SoftAttributeConf {
	
    static private Logger 						log = Logger.getLogger(SoftAttributeConf.class);
	
    private String					key;
	private String					name;
	private String					className;
	private String					providerParam;
	private boolean					visible;
	private boolean					longRunning;
	private boolean					locusDependant;
	
	private SoftAttributeProvider	provider;

	public SoftAttributeConf(Properties conf, String key) 
	{
		this.key = key;
		name = conf.getProperty(key, key).trim();
		className = conf.getProperty(key + ".className", "org.broad.igv.track.SoftAttributeProvider_Test").trim();
		providerParam = conf.getProperty(key + ".providerParam", "").trim();
		visible = Boolean.parseBoolean(conf.getProperty(key + ".visible", "true").trim());
		longRunning = Boolean.parseBoolean(conf.getProperty(key + ".longRunning", "false").trim());
		locusDependant = Boolean.parseBoolean(conf.getProperty(key + ".locusDependant", "false").trim());
		
		try
		{
			Class<?>		clazz = Class.forName(className);
			
			provider = (SoftAttributeProvider)clazz.newInstance();
		}
		catch (Throwable e)
		{
			log.error("failed to load provider from key: " + key, e);
		}

	}

	public String getAttribute(AbstractTrack track, String attrName, String locus) {
		
		// let provider do the hard work
		try {
			return provider.getAttribute(track, attrName, providerParam, locus);
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

	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	public boolean isLongRunning() {
		return longRunning;
	}

	public void setLongRunning(boolean longRunning) {
		this.longRunning = longRunning;
	}

	public boolean isLocusDependant() {
		return locusDependant;
	}

	public void setLocusDependant(boolean locusDependant) {
		this.locusDependant = locusDependant;
	}

	public SoftAttributeProvider getProvider() {
		return provider;
	}

	public void setProvider(SoftAttributeProvider provider) {
		this.provider = provider;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	
	

}
