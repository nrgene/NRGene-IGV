package org.broad.igv.track;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

@SuppressWarnings("serial")
public class AttributeValue extends LinkedHashMap<String, Set<String>>{

	// a value is a map of value->path+
	
	public AttributeValue()
	{
		
	}
	
	public AttributeValue(String value, String path)
	{
		this();
		addValue(value, path);
	}
	
	public boolean isMultiValue()
	{
		return size() > 1;
	}
	
	public boolean isNan()
	{
		return containsKey("NaN");
	}
	
	public String getStringValue()
	{
		if ( size() == 0 )
			return "";
		else if ( size() == 1 )
		{
			String		value = keySet().iterator().next();
			int			origins = get(value).size();
			
			if ( origins <= 1 )
				return value;
			else
				return value + " (" + origins + " Origins)";
		}
		else
			return "Conflict: " + StringUtils.join(keySet(), ",");
	}
	
	public Set<String> getAllValues()
	{
		return keySet();
	}
	
	public Set<String> getAllPaths()
	{
		Set<String>		allPaths = new LinkedHashSet<String>();
		
		for ( Set<String> paths : values() )
			allPaths.addAll(paths);
		
		return allPaths;
	}
	
	public void addValue(String value, String path)
	{
		Set<String>			paths = get(value);
		if ( paths == null )
			put(value, paths = new LinkedHashSet<String>());
		
		paths.add(path);
	}
	
	public void removeValue(String value)
	{
		remove(value);
	}
	
	public void removeValue(String value, String path)
	{
		Set<String>			paths = get(value);
		if ( paths == null )
			return;
		
		paths.remove(path);
		if ( paths.size() == 0 )
			remove(value);
	}
	
	public void removePath(String path)
	{
		for ( String value : new ArrayList<String>(keySet()) )
			removeValue(value, path);
	}
	
	public String toString()
	{
		return getStringValue();
	}

	public String getRepresentativeValue() 
	{
		if ( size() == 0 )
			return "";
		else
			return keySet().iterator().next();
	}
}
