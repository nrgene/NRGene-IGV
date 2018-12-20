package org.broad.igv.ui.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ActivityStatusTracker {
	
	static final String SEP = "    ";

	class ActivityStatus
	{
		String			name;
		String			verb;
		long			count, sum, first, last, min, max;
		boolean			running;
		
		
		public ActivityStatus(String name, String verb) {
			this.name = name;
			this.verb = verb;
		}
		
		public ActivityStatus(ActivityStatus other)
		{
			this.name = other.name;
			this.verb = other.verb;
			this.count = other.count;
			this.sum = other.sum;
			this.first = other.first;
			this.last = other.last;
			this.min = other.min;
			this.max = other.max;
		}
		
		public String toString()
		{
			StringBuilder		sb = new StringBuilder();
			
			sb.append(name + SEP + verb);
			
			sb.append("(" + count + ")");
			
			if ( count > 0 )
				sb.append(SEP + "min/avg/max" + SEP + min + "ms/" + sum/count + "ms/" + max + "ms");
			
			if ( running )
				sb.append(SEP + "[" + running + "]");
			
			return sb.toString();
		}
		
		public String toCSV()
		{			
			StringBuilder		sb = new StringBuilder();
			
			sb.append(name + "\t" + verb + "\t" + count + "\t");
			if ( count > 0 )
				sb.append(first + "\t" + last + "\t" + min + "\t" + sum/count + "\t" + max);
			
			return sb.toString();

		}
		
		String getKey()
		{
			return makeKey(name, verb);
		}
	}
	
	static String makeKey(String name, String verb)
	{
		return name + "." + verb;
	}
	
	
	private		Map<String, ActivityStatus>		activities = new LinkedHashMap<String, ActivityStatusTracker.ActivityStatus>();
	
	public synchronized void startActivity(String name, String verb)
	{
		ActivityStatus		as = getActivityStatus(name, verb);
		
		as.running = true;
	}

	public synchronized void finishActivity(String name, String verb, long elapsed)
	{
		ActivityStatus		as = getActivityStatus(name, verb);

		as.running = false;
		as.count++;
		as.sum += elapsed;
		as.last = elapsed;
		if ( as.count == 1 )
		{
			as.first = elapsed;
			as.min = as.max = elapsed;			
		}
		else
		{
			as.min = Math.min(as.min, elapsed);
			as.max = Math.max(as.max, elapsed);
		}
	}
	
	public synchronized void resetAll()
	{
		activities.clear();
	}

	private synchronized ActivityStatus getActivityStatus(String name, String verb) 
	{
		String				key = makeKey(name, verb);
		ActivityStatus		as = activities.get(key);
		
		if ( as == null )
			activities.put(key, as = new ActivityStatus(name, verb));
		
		return as;
	}
	
	public String toString()
	{
		StringBuilder		sb = new StringBuilder();

		List<String>		keys = new LinkedList<String>(activities.keySet());
		Collections.sort(keys);
		
		for ( String key : keys )
		{
			sb.append(activities.get(key));
			sb.append("\n");
		}
		
		return sb.toString();
	}
	
	public String toCSV()
	{
		StringBuilder		sb = new StringBuilder();

		// emit header line
		sb.append("Track Name\tOperation\tCount\tFirst (ms)\tLast (ms)\tMinimum (ms)\tAverage (ms)\tMaximum (ms)\n"); 
		
		// emit entries (sort keys first)
		List<String>		keys = new LinkedList<String>(activities.keySet());
		Collections.sort(keys);
		for ( String key : keys )
		{
			sb.append(activities.get(key).toCSV());
			sb.append("\n");
		}
		
		return sb.toString();
	}
}
