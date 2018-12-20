package org.broad.igv.feature.genome.mapping;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.broad.igv.feature.Chromosome;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.ui.IGV;

public class GenomeMapping192 implements IGenomeMapper {

	private GenomeLocusPair[]		startIndex;
	private GenomeLocusPair[]		defaultMappings;
	
	private MyComparator			startComp = new MyComparator();
	
	private String					fromGenomeId;
	private String					toGenomeId;
	
	private boolean					expandBeyondOnNoExactMatch = false;
	
	private GenomeMappingManager	gmm;
	
	class MyComparator implements Comparator<GenomeLocusPair> {

		@Override
		public int compare(GenomeLocusPair o1, GenomeLocusPair o2) 
		{
			GenomeLocus		l1 = o1.left;
			GenomeLocus		l2 = o2.left;
			
			int		diff = hybridCompare(l1.getChr(),  l2.getChr());
			if ( diff != 0 )
				return diff;

			diff = l1.getStart() - l2.getStart();
			if ( diff != 0 )
				return diff;
			
			return diff;
		}
		
		protected int hybridCompare(String s1, String s2)
		{
			int		length1 = s1.length();
			int		length2 = s2.length();
			int		index1 = 0;
			int		index2 = 0;
			
			for ( ; index1 < length1 && index2 < length2 ; )
			{
				char		ch1 = s1.charAt(index1++);
				char		ch2 = s2.charAt(index2++);
				
				// if not both digits resort to basic compare
				if ( !Character.isDigit(ch1) || !Character.isDigit(ch2) )
				{
					if ( ch1 != ch2 )
						return ch1 - ch2;
				}
				
				// parse numbers (base 10)
				int			num1 = ch1 - '0';
				for ( ; index1 < length1 && Character.isDigit(ch1 = s1.charAt(index1)) ; index1++ )
					num1 = num1 * 10 + ch1 - '0';
				int			num2 = ch2 - '0';
				for ( ; index2 < length2 && Character.isDigit(ch2 = s2.charAt(index2)) ; index2++ )
					num2 = num2 * 10 + ch2 - '0';
				
				// compare nunbers
				if ( num1 != num2 )
					return num1 - num2;
				
				// continue if equal
			}
			
			// both strings ended?
			if ( (index1 == length1) && (index2 == length2) )
				return 0;
			
			// shorter string wins
			if ( index1 == length1 )
				return -1;
			else
				return 1;
		}
	}
	
	public GenomeMapping192(List<GenomeLocusPair> pairs, String fromGenomeId, String toGenomeId, GenomeMappingManager gmm)
	{
		// save genomes
		this.gmm = gmm;
		this.fromGenomeId = fromGenomeId;
		this.toGenomeId = toGenomeId;
		
		// extract list of default mappings
		List<GenomeLocusPair>	defaults = new LinkedList<GenomeLocusPair>();
		for ( GenomeLocusPair pair : pairs )
			if ( pair.left.isChromosomeOnly() && pair.right.isChromosomeOnly() )
				defaults.add(pair);
		pairs.removeAll(defaults);
		
		// copy into a stable and fast arrays
		startIndex = pairs.toArray(new GenomeLocusPair[0]);
		defaultMappings = defaults.toArray(new GenomeLocusPair[0]);

		// sort
		Arrays.sort(startIndex, startComp);
	}

	@Override
	public GenomeLocus mapLocus(GenomeLocus locus) throws GenomeMapperException 
	{
		if ( isEmptyLocus(locus) )
			return locus;
		
		if ( locus.isChromosomeOnly() )
			return mapLocusUsingDefault(locus);
		
		GenomeLocusPair		key = new GenomeLocusPair(translateFromGenome(locus, fromGenomeId), null);
		
		// find start/end
		int					entry = Arrays.binarySearch(startIndex, key, startComp);
		int					entryEnd = Arrays.binarySearch(startIndex, new GenomeLocusPair(translateFromGenome(locus.flipStartEnd(), fromGenomeId), null), startComp);
		int					minStart = Integer.MAX_VALUE;
		int					maxEnd = Integer.MIN_VALUE;
		
		// special code for avoiding extensions
		if ( !expandBeyondOnNoExactMatch )
		{
			// start and end before first mapping? 
			if ( (entry == -1) && (entryEnd == -1) )
				throw new GenomeMapperException(key.left, "start/end before all mappings");
			
			// start and end after last mapping?
			if ( (-(entry + 1) == startIndex.length) && (-(entryEnd + 1) == startIndex.length) )
				throw new GenomeMapperException(key.left, "start/end past all mappings");
			
			// otherwise we'll give it a chance
		}
		
		if ( entry < 0 )
		{
			// not found, get entry after/before
			entry = -(entry + 1);
			if ( expandBeyondOnNoExactMatch )
				entry--; 				// moving one back because we're going down
			entry = Math.max(0, Math.min(entry, startIndex.length - 1));
			
			// if before first entry, start from start of chromosome
			if ( entry == 0 )
			{
				if ( expandBeyondOnNoExactMatch )
					minStart = translateFromGenome(new GenomeLocus("x", 1, 1), toGenomeId).getStart();
				else if ( startIndex.length == 0 )
					throw new GenomeMapperException(key.left, "could not map start since mapping is empty ...");
				else
					minStart = startIndex[entry].right.getStart();
			}
			
			// insertion point must be on the same chromosome
			while ( !startIndex[entry].left.getChr().equals(key.left.getChr()) )
			{
				if ( entry < startIndex.length - 1 )
					entry++;
				else 
					throw new GenomeMapperException(key.left, "could not align start on same chromosome");
				
				if ( minStart == Integer.MAX_VALUE )
					minStart = translateFromGenome(new GenomeLocus("x", 1, 1), toGenomeId).getStart();
			}
		}
		
		// if we are handling ranges, the insertion point can just beyond the key we are included in
		if ( !startIndex[entry].left.containsStart(key.left) 
				&& entry > 0 
				&& startIndex[entry-1].left.getChr().equals(startIndex[entry].left.getChr()) 
				&& startIndex[entry-1].left.containsStart(key.left) )
			entry--;
		
		
		// found or approx, walk back to first entry with same start address
		String			startChr = startIndex[entry].left.getChr();
		int				start = startIndex[entry].left.getStart();
		while ( entry > 0 && startIndex[entry - 1].left.getChr().equals(startChr) && startIndex[entry - 1].left.getStart() == start )
			entry--;
		
		// take in first entry
		String			targetChr = null;
		if ( startIndex[entry].left.getStart() != startIndex[entry].left.getEnd() )
		{
			minStart = Math.min(minStart, startIndex[entry].right.getStart());
			maxEnd = Math.max(maxEnd, startIndex[entry].right.getEnd());
			targetChr = startIndex[entry].right.getChr();
		}
		
		// walk forward collecting minimal start and max end from address from right side
		boolean			overflow = expandBeyondOnNoExactMatch;
		int				overflowEnd = -1;
		while ( entry < startIndex.length 
				&& startIndex[entry].left.getChr().equals(startChr) 
				&& (startIndex[entry].left.getEnd() <= key.left.getEnd() || overflow) ) 
		{
			if ( overflowEnd >= 0 && startIndex[entry].left.getEnd() > overflowEnd )
				break;
			
			minStart = Math.min(minStart, startIndex[entry].right.getStart());
			maxEnd = Math.max(maxEnd, startIndex[entry].right.getEnd());
			
			if ( targetChr == null )
				targetChr = startIndex[entry].right.getChr();
			else if ( !targetChr.equals(startIndex[entry].right.getChr()) )
				throw new GenomeMapperException(key.left, "mapping spans more than one chromosome (" + targetChr + ", " + startIndex[entry].right.getChr());;
			
			if ( startIndex[entry].left.getEnd() >= key.left.getEnd() )
			{
				if ( overflowEnd < 0 )
					overflowEnd = startIndex[entry].left.getEnd();
			}
			
			entry++;
		}
		
		// extend last
		if ( startIndex[entry].left.getStart() != startIndex[entry].left.getEnd() 
				&& startIndex[entry].left.containsEnd(key.left) )
		{
			minStart = Math.min(minStart, startIndex[entry].right.getStart());
			maxEnd = Math.max(maxEnd, startIndex[entry].right.getEnd());
		}

		
		if ( entry >= startIndex.length )
		{
			// need to extend to end of chromosome?
			if ( key.left.getEnd() > startIndex[entry-1].left.getEnd() && expandBeyondOnNoExactMatch )
			{
				int			chromosomeLength = getChromosomeLength(toGenomeId, targetChr);
				if ( chromosomeLength >= 0 )
				{
					chromosomeLength = translateFromGenome(new GenomeLocus("x", 1, chromosomeLength), toGenomeId).getEnd();
					maxEnd = Math.max(maxEnd, chromosomeLength);
				}
			}
		}
		
		if ( targetChr == null || minStart == Integer.MAX_VALUE || maxEnd == Integer.MIN_VALUE )
			throw new GenomeMapperException(key.left, "no mapping found");
		
		// construct a new locus
		GenomeLocus		result = new GenomeLocus(targetChr, Math.min(minStart, maxEnd), Math.max(minStart, maxEnd));
		
		return translateToGenome(result, toGenomeId);
		
	}
	
	private int getChromosomeLength(String genomeId, String chromosome) 
	{
		// debugging hack
		if ( genomeId.endsWith("*") )
			return 10000;
		
		// access genome
		Genome			genome = IGV.getInstance().getGenomeManager().getCachedGenomeById(genomeId);
		if ( genome == null )
			return -1;
		
		// access chromosome
		Chromosome		ch = genome.getChromosome(chromosome);
		if ( ch == null )
			return -1;
		
		return ch.getLength();
	}

	private GenomeLocus mapLocusUsingDefault(GenomeLocus locus) throws GenomeMapperException 
	{
		for ( GenomeLocusPair pair : defaultMappings )
			if ( pair.left.getChr().equals(locus.getChr()) )
				return pair.right;
		
		throw new GenomeMapperException(locus, "no default mapping");
	}

	@SuppressWarnings("unused")
	private boolean locusIntersect(GenomeLocus l1, GenomeLocus l2)
	{
		if ( l1.getChr().equals(l2.getChr()) )
		{
			int		start1 = l1.getStart();
			int		end1 = l1.getEnd();
			int		start2 = l2.getStart();
			int		end2 = l2.getEnd();
			
			// 1 includes 2?
			if ( start1 < start2 && end1 > end2 )
				return true;
			
			// 1 start or end falls in 2?
			if ( start1 >= start2 && start1 <= end2 )
				return true;
			if ( end1 >= start2 && end1 <= end2 )
				return true;
			
			// otherwise no intersection
			return false;
		}
		else
			return false;
	}
	
	private GenomeLocus translateFromGenome(GenomeLocus locus, String genomeId) 
	{
		double			start;
		double			end;
		
		if ( genomeId.equals("a*") )
		{
			start = locus.getStart() * 100;
			end = locus.getEnd() * 100;
		}
		else if ( genomeId.equals("b*") )
		{
			start = locus.getStart();
			end = locus.getEnd();
		}
		else if ( genomeId.endsWith("*") )
			return locus;
		else
		{
			Genome			genome = IGV.getInstance().getGenomeManager().getCachedGenomeById(genomeId);
			if ( genome == null )
				return locus;

			start = genome.formatDoubleCoordinate(Integer.toString(locus.getStart()));
			end = genome.formatDoubleCoordinate(Integer.toString(locus.getEnd()));
		}
		
		GenomeLocus		result = gmm.getTranslationOriginAdvice(genomeId, locus);
		if ( result != null )
			return result;
		
		result = new GenomeLocus(locus.getChr(), (int)Math.floor(start), (int)Math.ceil(end));
		
		return result;
	}

	private GenomeLocus translateToGenome(GenomeLocus locus, String genomeId) 
	{
		double		start;
		double		end;
		
		if ( genomeId.equals("a*") )
		{
			start = (double)locus.getStart() / 100;
			end = (double)locus.getEnd() / 100;
		}
		else if ( genomeId.equals("b*") )
		{
			start = locus.getStart();
			end = locus.getEnd();
		}
		else if ( genomeId.endsWith("*") )
			return locus;
		else
		{
			Genome			genome = IGV.getInstance().getGenomeManager().getCachedGenomeById(genomeId);
			if ( genome == null )
				return locus;
			
			start = genome.parseDoubleCoordinate(Integer.toString(locus.getStart()));
			end = genome.parseDoubleCoordinate(Integer.toString(locus.getEnd()));
		}
			
		GenomeLocus		result = new GenomeLocus(locus.getChr(), (int)Math.floor(start), (int)Math.ceil(end));
		
		gmm.addTranslationOriginAdvice(genomeId, locus, result);
		
		return result;
	}

	public IGenomeMapper getReverseGenomeMapper()
	{
		List<GenomeLocusPair>	pairs = new LinkedList<GenomeLocusPair>();
		
		for ( GenomeLocusPair pair : defaultMappings )
			pairs.add(new GenomeLocusPair(pair.right, pair.left));			
		
		for ( GenomeLocusPair pair : startIndex )
			pairs.add(new GenomeLocusPair(pair.right, pair.left));
		
		return new GenomeMapping192(pairs, toGenomeId, fromGenomeId, gmm);
	}
	
	private boolean isEmptyLocus(GenomeLocus locus) 
	{
		return StringUtils.isEmpty(locus.getChr()) || "all".equalsIgnoreCase(locus.getChr());
	}

	@Override
	public String getMappingInfo() 
	{
		return String.format("%s: %d pairs %d defaults", getClass().getSimpleName(), startIndex.length, defaultMappings.length);
	}
}
