package org.broad.igv.variant.vcf;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


@SuppressWarnings({ "unchecked", "rawtypes" })
public class ReadyOnlyMap2 implements Map {
	
	private Object		key1;
	private Object		value1;
	private Object		key2;
	private Object		value2;
	
	class Entry implements Map.Entry
	{
		int				index;
		
		Entry(int index)
		{
			this.index = index;
			
		}
		
		@Override
		public Object getKey() 
		{
			return (index == 0) ? key1 : key2;
		}

		@Override
		public Object getValue() 
		{
			return (index == 0) ? value1 : value2;
		}

		@Override
		public Object setValue(Object value) 
		{
			throw new UnsupportedOperationException();
		}
	}
	
	public ReadyOnlyMap2(Object key1, Object value1, Object key2, Object value2)
	{
		this.key1 = key1;
		this.value1 = value1;
		this.key2 = key2;
		this.value2 = value2;
	}	
	
	@Override
	public String toString() {
		return "ReadyOnlyMap2 [key1=" + key1 + ", value1=" + value1 + ", key2="
				+ key2 + ", value2=" + value2 + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((key1 == null) ? 0 : key1.hashCode());
		result = prime * result + ((key2 == null) ? 0 : key2.hashCode());
		result = prime * result + ((value1 == null) ? 0 : value1.hashCode());
		result = prime * result + ((value2 == null) ? 0 : value2.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ReadyOnlyMap2 other = (ReadyOnlyMap2) obj;
		if (key1 == null) {
			if (other.key1 != null)
				return false;
		} else if (!key1.equals(other.key1))
			return false;
		if (key2 == null) {
			if (other.key2 != null)
				return false;
		} else if (!key2.equals(other.key2))
			return false;
		if (value1 == null) {
			if (other.value1 != null)
				return false;
		} else if (!value1.equals(other.value1))
			return false;
		if (value2 == null) {
			if (other.value2 != null)
				return false;
		} else if (!value2.equals(other.value2))
			return false;
		return true;
	}

	@Override
	public boolean isEmpty() 
	{
		return false;
	}

	@Override
	public boolean containsKey(Object obj) 
	{
		return key1.equals(obj) || key2.equals(obj);
	}
	@Override
	public boolean containsValue(Object obj) 
	{
		return value1.equals(obj) || value2.equals(obj);
	}
	
	@Override
	public Object get(Object obj) 
	{
		if ( key1.equals(obj) )
			return value1;
		else if ( key2.equals(obj) )
			return value2;
		else
			return null;
	}
	@Override
	public void clear() 
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Set keySet() 
	{
		return new HashSet(Arrays.asList(key1, key2));
	}
	
	@Override
	public Set entrySet() 
	{
		return new HashSet(Arrays.asList(new Entry(0), new Entry(1)));
	}
	
	@Override
	public Object put(Object key, Object value) 
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int size() 
	{
		return 2;
	}
	
	@Override
	public Object remove(Object obj) 
	{
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void putAll(Map map) 
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection values() 
	{
		return new HashSet(Arrays.asList(value1, value2));
	}
}
