package org.broad.igv.feature.genome.mapping;

public class GenomeLocusPair {

	GenomeLocus		left;
	GenomeLocus		right;

	public GenomeLocusPair(GenomeLocus left, GenomeLocus right)
	{
		this.left = left;
		this.right = right;
	}
	
	@Override
	public String toString() 
	{
		return left + " -> " + right;
	}
}
