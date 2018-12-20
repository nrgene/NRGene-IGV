package org.broad.igv.track;

import java.util.Date;

import org.apache.log4j.Logger;

public class SoftAttributeCachedValue {
	
    static private Logger 						log = Logger.getLogger(SoftAttributeCachedValue.class);
	
	private SoftAttributeConf		conf;
	private String					trackKey;
	private String					locus;
	private Date					date;
	private String					value;
	
	public SoftAttributeCachedValue(SoftAttributeConf conf, String trackKey, String locus)
	{
		this.conf = conf;
		this.trackKey = trackKey;
		this.locus = locus;
		this.date = new Date();
	}
	
	public String getKey()
	{
		return conf.getKey() + "|" + trackKey + "|" + locus; 
	}
	
	public SoftAttributeConf getConf() {
		return conf;
	}
	public void setConf(SoftAttributeConf conf) {
		this.conf = conf;
	}
	public String getTrackKey() {
		return trackKey;
	}
	public void setTrackKey(String trackKey) {
		this.trackKey = trackKey;
	}
	public String getLocus() {
		return locus;
	}
	public void setLocus(String locus) {
		this.locus = locus;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	

}
