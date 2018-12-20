package org.broad.igv.feature.genome;

import java.util.Arrays;
import java.util.List;

import org.broad.igv.feature.genome.mapping.GenomeLocus;
import org.broad.igv.feature.genome.mapping.GenomeMapperException;
import org.broad.igv.feature.genome.mapping.IGenomeMapper;
import org.junit.Assert;
import org.junit.Test;


public class FallbackGenomeMappingTest {
	
	static final GenomeLocus			LOC1_FROM = new GenomeLocus("1:12-13");
	static final GenomeLocus			LOC1_TO = new GenomeLocus("1:14-15");
	static final GenomeLocus			LOC2_FROM = new GenomeLocus("2:22-23");
	static final GenomeLocus			LOC2_TO = new GenomeLocus("2:24-25");
	static final GenomeLocus			LOC3_FROM = new GenomeLocus("3:32-33");
	
	static class OneEntryMapper implements IGenomeMapper
	{
		private final GenomeLocus		from;
		private final GenomeLocus		to;
		
		OneEntryMapper(GenomeLocus from, GenomeLocus to)
		{
			this.from = from;
			this.to = to;
		}

		@Override
		public GenomeLocus mapLocus(GenomeLocus locus) throws GenomeMapperException 
		{
			 if ( locus.equals(from) )
				 return to;
			 else
				 throw new GenomeMapperException(locus);
		}

		@Override
		public String getMappingInfo() {
			return getClass().getName();
		}
	}
	
	@Test
	public void testNormalFallback()
	{
		// build out of two mappers
		List<? extends IGenomeMapper>		list = Arrays.asList(
															new OneEntryMapper(LOC1_FROM, LOC1_TO),
															new OneEntryMapper(LOC2_FROM, LOC2_TO));
		IGenomeMapper						mapper = new FallbackGenomeMapping(list);
		
		// loc1
		Assert.assertEquals(map(mapper, LOC1_FROM), LOC1_TO);
		Assert.assertEquals(map(mapper, LOC2_FROM), LOC2_TO);
		Assert.assertNull(map(mapper, LOC3_FROM));
	}
	
	@Test
	public void testSameKeyFallback()
	{
		// build out of two mappers
		List<? extends IGenomeMapper>		list = Arrays.asList(
															new OneEntryMapper(LOC1_FROM, LOC1_TO),
															new OneEntryMapper(LOC1_FROM, LOC2_TO));
		IGenomeMapper						mapper = new FallbackGenomeMapping(list);
		
		// loc1
		Assert.assertEquals(map(mapper, LOC1_FROM), LOC1_TO);
	}
	
	private GenomeLocus map(IGenomeMapper mapper, GenomeLocus from)
	{
		try
		{
			return mapper.mapLocus(from);
			
		} catch (GenomeMapperException e) {
			
			return null;
		}
	}
}
