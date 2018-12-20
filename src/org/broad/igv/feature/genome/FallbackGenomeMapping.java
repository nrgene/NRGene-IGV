package org.broad.igv.feature.genome;

import java.util.Collections;
import java.util.List;

import org.broad.igv.feature.genome.mapping.GenomeLocus;
import org.broad.igv.feature.genome.mapping.GenomeMapperException;
import org.broad.igv.feature.genome.mapping.IGenomeMapper;

public class FallbackGenomeMapping implements IGenomeMapper {

	private final List<IGenomeMapper>			mappers;
	
	public FallbackGenomeMapping(List<? extends IGenomeMapper> mappers)
	{
		this.mappers = Collections.unmodifiableList(mappers);
	}
	
	@Override
	public GenomeLocus mapLocus(GenomeLocus locus) throws GenomeMapperException 
	{
		GenomeMapperException		exception = null;
		
		// loop through mappers, use the first one that does not fail
		for ( IGenomeMapper mapper : mappers )
		{
			try
			{
				return mapper.mapLocus(locus);
				
			} catch (GenomeMapperException e) 
			{
				exception = e;
			}
		}
		
		// if here, all failed. delivery exception from last one
		throw exception;
	}

	@Override
	public String getMappingInfo() 
	{
		StringBuilder		sb = new StringBuilder();
		
		sb.append(getClass().getSimpleName() + ":");
		for ( IGenomeMapper mapper : mappers )
			sb.append("\n\t" + mapper.getMappingInfo());
		
		return sb.toString();
	}	

}
