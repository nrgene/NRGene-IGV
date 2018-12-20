package org.broad.igv.feature.genome.mapping;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.broad.igv.feature.genome.FallbackGenomeMapping;

public class GenomeMappingManager {
	
	static class AssertInstruction
	{
		String			fromGenome;
		String			toGenome;
		GenomeLocus		fromLocus;
		GenomeLocus		toLocus;
		
		boolean			passed;
		String			errorMessage;
		
		AssertInstruction(String fromGenome, String toGenome, GenomeLocus fromLocus, GenomeLocus toLocus)
		{
			this.fromGenome = fromGenome;
			this.toGenome = toGenome;
			this.fromLocus = fromLocus;
			this.toLocus = toLocus;
		}
		
		@Override
		public String toString()
		{
			return String.format("%s!%s -> %s!%s", fromGenome, fromLocus, toGenome, toLocus);
		}
	}
	
	static class MappingPair
	{
		String		fromGenome;
		String		toGenome;
		
		public MappingPair(String fromGenome, String toGenome)
		{
			this.fromGenome = fromGenome;
			this.toGenome = toGenome;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((fromGenome == null) ? 0 : fromGenome.hashCode());
			result = prime * result
					+ ((toGenome == null) ? 0 : toGenome.hashCode());
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
			MappingPair other = (MappingPair) obj;
			if (fromGenome == null) {
				if (other.fromGenome != null)
					return false;
			} else if (!fromGenome.equals(other.fromGenome))
				return false;
			if (toGenome == null) {
				if (other.toGenome != null)
					return false;
			} else if (!toGenome.equals(other.toGenome))
				return false;
			return true;
		}
		
		
	}
	
    private static Logger log = Logger.getLogger(GenomeMappingManager.class);
	
	private Map<MappingPair, IGenomeMapper>		mappers = new LinkedHashMap<MappingPair, IGenomeMapper>();
	private Map<String, GenomeLocus>		mappingOriginAdvices = new LinkedHashMap<String, GenomeLocus>();
	
	public IGenomeMapper getMapper(String fromGenome, String toGenome)
	{
		// try a direct mapping
		MappingPair			directKey = new MappingPair(fromGenome, toGenome);
		IGenomeMapper		directMapping = mappers.get(directKey);
		if ( directMapping != null )
			return directMapping;
		
		// no direct mapping, try to find an indirect mapping path
		// for now, we are only looking for a two-step path
		Set<String>		midCandidates = new LinkedHashSet<String>();
		for ( MappingPair key : mappers.keySet() )
			if ( key.fromGenome.equals(fromGenome) )
				midCandidates.add(key.toGenome);
		for ( MappingPair key : mappers.keySet() )
			if ( key.toGenome.equals(toGenome) && midCandidates.contains(key.fromGenome) )
			{
				// found! construct an mapping chain
				String					midGenome = key.fromGenome;
				List<IGenomeMapper>		chain = new LinkedList<IGenomeMapper>();
				chain.add(getMapper(fromGenome, midGenome));
				chain.add(getMapper(midGenome, toGenome));
				
				return new ChainGenomeMapper(chain);
			}
		
		// if here, not found
		return null;
		
	}
	
	public void addMapping(String fromGenome, String toGenome, List<GenomeLocusPair> pairs)
	{
		GenomeMapping		mapping = new GenomeMapping(pairs, fromGenome, toGenome, this);
		
		log.info("loading " + pairs.size() + " mappings from " + fromGenome + " to " + toGenome);
		
		mappersPut(new MappingPair(fromGenome, toGenome), mapping);
		mappersPut(new MappingPair(toGenome, fromGenome), mapping.getReverseGenomeMapper());
	}
	
	public void addMapping(String fromGenome, String toGenome, File localFile) throws IOException
	{
		LocalFileGenomeMapping			mapping = new LocalFileGenomeMapping(localFile, fromGenome, toGenome, this);
		
		mappersPut(new MappingPair(fromGenome, toGenome), mapping);
		mappersPut(new MappingPair(toGenome, fromGenome), mapping.getReverseGenomeMapper());
	}
	
	private void mappersPut(MappingPair mappingPair, IGenomeMapper mapper) 
	{
		IGenomeMapper		existingMapper = mappers.get(mappingPair);
		
		// new?
		if ( existingMapper == null )
			mappers.put(mappingPair, mapper);
		else
		{
			// already has one. push this one in front of it
			List<IGenomeMapper>		list = Arrays.asList(mapper, existingMapper);
			IGenomeMapper			fallbackMapper = new FallbackGenomeMapping(list);
			
			mappers.put(mappingPair, fallbackMapper);
		}
		
	}

	public void addMapping(String fromGenome, String toGenome, InputStream is, boolean readPairs) throws IOException
	{
		BufferedReader			reader = new BufferedReader(new InputStreamReader(is));
		String					line;
		List<GenomeLocusPair> 	pairs = new LinkedList<GenomeLocusPair>();
		List<AssertInstruction>	assertInstructions = new LinkedList<GenomeMappingManager.AssertInstruction>();
		
		while ( (line = reader.readLine()) != null )
		{
			// trip and skip empty lines
			line = line.trim();
			if ( line.length() == 0 )
				continue;
			
			// handle special and regular comments
			if ( line.charAt(0) == '#' )
			{
				// remove leading hash
				line = line.substring(1);
				
				String[]		toks = StringUtils.split(line, "\t ");
				
				// handle know keywords
				if ( toks[0].equalsIgnoreCase("MAP") )
				{
					if ( toks.length != 3 )
						log.error("invalid MAP line (too many tokens): " + line);
					else if ( line.indexOf(":") >= 0 || line.indexOf("-") >= 0 )
						log.error("invalid MAP line (must contain only chromosome names): " + line);
					else
						pairs.add(new GenomeLocusPair(new GenomeLocus(toks[1]), new GenomeLocus(toks[2])));
				}
				else if ( toks[0].equalsIgnoreCase("ASSERT") )
				{
					if ( toks.length != 4 )
						log.error("invalid ASSERT line (too many tokens): " + line);
					else if ( !toks[2].equals("->") && !toks[2].equals("<-") )
						log.error("invalid ASSERT line (mid operator must be -> or <-): " + line);
					else
					{
						if ( toks[2].equals("->") )
							assertInstructions.add(new AssertInstruction(fromGenome, toGenome, new GenomeLocus(toks[1]), new GenomeLocus(toks[3])));
						else
							assertInstructions.add(new AssertInstruction(toGenome, fromGenome, new GenomeLocus(toks[3]), new GenomeLocus(toks[1])));
					}
				}
				
				// comments are skipped
				continue;
			}
			
			if ( readPairs )
			{
				String[]		toks = StringUtils.split(line, "\t ");
				if ( toks.length >= 2 )
					pairs.add(new GenomeLocusPair(new GenomeLocus(toks[0]), new GenomeLocus(toks[1])));
			}
			else
			{
				// assuming that all comments come before pairs, its time to quit reading
				break;
			}
		}
		
		addMapping(fromGenome, toGenome, pairs);
		
		executeAssertInsructions(assertInstructions);
	}
	
	private void executeAssertInsructions(List<AssertInstruction> assertInstructions) 
	{
		for ( AssertInstruction ai : assertInstructions )
		{
			executeAssertInsruction(ai);
			
			if ( !ai.passed )
			{
				log.error("mapping assertion failed: " + ai + " ==>> " + ai.errorMessage);
				
				// execute failed instruction again to allow debug
				executeAssertInsruction(ai);
			}
			else
				log.info("mapping assertion passed: " + ai);
		}
	}

	private void executeAssertInsruction(AssertInstruction ai) 
	{
		// get mapping
		IGenomeMapper		mapper = getMapper(ai.fromGenome, ai.toGenome);
		if ( mapper == null )
		{
			ai.errorMessage = "no such mapper";
			return;
		}
		
		// map
		GenomeLocus			locus;
		try
		{
			 locus = mapper.mapLocus(ai.fromLocus);
		}
		catch (GenomeMapperException e)
		{
			ai.errorMessage = "mapping exception: " + e.getMessage();
			return;
		}
		
		// compare
		if ( locus == null )
		{
			ai.errorMessage = "mapped into null locus";
			return;
		}
		if ( !locus.equals(ai.toLocus) )
		{
			ai.errorMessage = "unexpected mapping result: " + locus;
			return;
		}
		
		// if here, passed
		ai.passed = true;
	}

	public static void main(String[] args) throws IOException, GenomeMapperException
	{
		GenomeMappingManager		gmm = new GenomeMappingManager();
		
		// load file
		String			from = "a*";
		String			to = "b*";
		String			mid = null;
		if ( args.length > 0 && args[0].charAt(0) == '-' )
		{
			from = args[0].substring(1);
			args = Arrays.copyOfRange(args, 1, args.length);
		}
		if ( args.length > 0 && args[0].charAt(0) == '-' )
		{
			to = args[0].substring(1);
			args = Arrays.copyOfRange(args, 1, args.length);
		}
		if ( args.length > 0 && args[0].charAt(0) == '-' )
		{
			mid = args[0].substring(1);
			args = Arrays.copyOfRange(args, 1, args.length);
		}
		if ( args.length > 0 )
		{
			String			path = args[0];
			System.out.println("loading " + path);
			if ( mid == null )
			{
				System.out.println("reading mapping from " + from + " to " + to);
				gmm.addMapping(from, to, new FileInputStream(new File(path)), true);
			}
			else
			{
				System.out.println("reading mapping from " + from + " to " + mid);
				gmm.addMapping(from, mid, new FileInputStream(new File(path)), true);
				
				System.out.println("reading mapping from " + to + " to " + mid);
				gmm.addMapping(to, mid, new FileInputStream(new File(path)), true);
			}
		}
		
		// walk rest of args as address
		System.out.println("reading mapping from " + from + " to " + to);
		for ( int i = 1 ; i < args.length ; i++ )
		{
			String			locus = args[i];
			GenomeLocus		toLocus = gmm.getMapper(from, to).mapLocus(new GenomeLocus(locus));
			GenomeLocus		fromLocus = gmm.getMapper(to, from).mapLocus(toLocus);
			GenomeLocus		toLocus2 = gmm.getMapper(from, to).mapLocus(fromLocus);
			GenomeLocus		fromLocus2 = gmm.getMapper(to, from).mapLocus(toLocus2);
			System.out.println(locus + " -> " + toLocus + " -> " + fromLocus + " -> " + toLocus2 + " -> " + fromLocus2);
		}
	}

	public void addTranslationOriginAdvice(String genomeId, GenomeLocus fromLocus, GenomeLocus toLocus) 
	{
		mappingOriginAdvices.put(genomeId + "/" + toLocus.toString(), fromLocus);
	}

	public GenomeLocus getTranslationOriginAdvice(String genomeId, GenomeLocus locus) {
		return mappingOriginAdvices.get(genomeId + "/" + locus.toString());
	}
	
	public String getMappingInfo()
	{
		StringBuilder		info = new StringBuilder();
		
		for ( Map.Entry<MappingPair, IGenomeMapper> entry : mappers.entrySet() )
		{
			info.append(String.format("%s -> %s : %s\n", 
											entry.getKey().fromGenome,
											entry.getKey().toGenome,
											entry.getValue().getMappingInfo()));
		}
		
		return info.toString();
	}
}
