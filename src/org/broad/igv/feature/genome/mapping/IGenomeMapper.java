package org.broad.igv.feature.genome.mapping;

public interface IGenomeMapper {

	GenomeLocus		mapLocus(GenomeLocus locus) throws GenomeMapperException;
	String			getMappingInfo();
}
