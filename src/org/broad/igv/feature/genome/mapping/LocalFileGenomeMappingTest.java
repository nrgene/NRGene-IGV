package org.broad.igv.feature.genome.mapping;

import java.io.File;
import java.io.IOException;

import junit.framework.Assert;

import org.junit.Test;


public class LocalFileGenomeMappingTest {

	static final String			TEST_RESOURCE = "resources/test/LocalFileGenomeMappingTestData.txt";
	static final String			TEST_FROM_GENOME = "genome1";
	static final String			TEST_TO_GENOME = "genome2";
	
	@Test
	public void testExactEntries() throws IOException
	{
		// create mapper
		IGenomeMapper		mapper = buildMapper();
		
		// check for exact mappings
		Assert.assertTrue(checkMap(mapper, "1:100-1198", "1:100-1837"));
		Assert.assertTrue(checkMap(mapper, "1:1198-1200", "1:1837-1839"));
		Assert.assertTrue(checkMap(mapper, "1:1300-1711", "1:1939-2467"));
	}
	
	@Test
	public void testMissingEntries() throws IOException
	{
		// create mapper
		IGenomeMapper		mapper = buildMapper();
		
		// before
		Assert.assertTrue(checkMap(mapper, "1:1-99", null));
		
		// middle
		Assert.assertTrue(checkMap(mapper, "1:1210-1:1220", null));
		
		// after
		Assert.assertTrue(checkMap(mapper, "2:1-2", null));
	}
	
	
	private IGenomeMapper buildMapper() throws IOException
	{
		String			path = getClass().getClassLoader().getResource(TEST_RESOURCE).getFile();
		return new LocalFileGenomeMapping(new File(path), TEST_FROM_GENOME, TEST_TO_GENOME, null);
	}

	private boolean checkMap(IGenomeMapper mapper, String from, String to)
	{
		GenomeLocus			result = map(mapper, new GenomeLocus(from));
		
		if ( result == null )
			return (to == null);
		else if ( to == null )
			return false;
		else
			return result.equals(new GenomeLocus(to));
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
