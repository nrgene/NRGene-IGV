package org.broad.igv.feature.genome.mapping;

@SuppressWarnings("serial")
public class GenomeMapperException extends Exception {

	public GenomeMapperException(GenomeLocus locus)
	{
		super("failed to map locus: " + locus);
	}

	public GenomeMapperException(GenomeLocus locus, String message)
	{
		super("failed to map locus: " + locus + ", " + message);
	}
}
