package org.broad.igv.feature.genome.mapping;

import java.util.List;

public class ChainGenomeMapper implements IGenomeMapper {

	private List<IGenomeMapper>		chain;
	
	public ChainGenomeMapper(List<IGenomeMapper> chain)
	{
		this.chain = chain;
	}
	
	@Override
	public GenomeLocus mapLocus(GenomeLocus locus) throws GenomeMapperException 
	{
		int			chainIndex = 0;
		for ( IGenomeMapper mapper : chain )
		{
			locus = mapper.mapLocus(locus);
			if ( locus == null )
				throw new GenomeMapperException(locus, "not found, on chain element " + chainIndex);
			chainIndex++;
		}
		
		return locus;
	}

	@Override
	public String getMappingInfo() 
	{
		StringBuilder		sb = new StringBuilder();
		
		sb.append(getClass().getSimpleName() + ":");
		for ( IGenomeMapper mapper : chain )
			sb.append(" " + mapper.getMappingInfo());
		
		return sb.toString();
	}	
}
