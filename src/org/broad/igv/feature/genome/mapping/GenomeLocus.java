package org.broad.igv.feature.genome.mapping;

import org.apache.commons.lang.StringUtils;
import org.broad.igv.feature.Locus;

public class GenomeLocus extends Locus implements Comparable<GenomeLocus> {

	public GenomeLocus(String locusString) {
		super(locusString);
	}
	
	public GenomeLocus(String chr, int start, int end) {
		super(chr, start, end);
	}
	
	public int compareTo(GenomeLocus o) 
	{
		int		diff = chr.compareTo(o.chr);
		if ( diff != 0 )
			return diff;
		
		diff = start - o.start;
		if ( diff != 0 )
			return diff;
		
		diff = end - o.end;
		return diff;
	}

	@Override
	public boolean equals(Object obj) 
	{
		if ( obj == null || !(obj instanceof GenomeLocus) )
			return super.equals(obj);
		
		return compareTo((GenomeLocus)obj) == 0;
	}

	public boolean isChromosomeOnly() 
	{
		return !StringUtils.isEmpty(chr) && (start <= 0) && (end <= 0); 
	}

	public GenomeLocus flipStartEnd() 
	{
		return new GenomeLocus(getChr(), getEnd(), getStart());
	}
}
