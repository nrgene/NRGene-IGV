package org.broad.igv.feature.genome.mapping;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.broad.igv.PreferenceManager;
import org.broad.igv.feature.genome.GenomeDescriptor;
import org.broad.igv.feature.genome.GenomeZipDescriptor;

public class LocalFileGenomeMapping implements IGenomeMapper {

    private static Logger 			log = Logger.getLogger(LocalFileGenomeMapping.class);
    
    private static final int		MAX_MAPPING_LINE_SIZE = 128;
    private static final int		MIN_MAPPING_LINE_SIZE = 10;

	private File					localFile;
	private String					fromGenome;
	private String					toGenome;
	private GenomeMappingManager	genomeMappingManager;
	private boolean					forwardMapping;
	
	private	long					mappingSize;
	private MappedByteBuffer		mapping;
		
	public LocalFileGenomeMapping(File localFile, String fromGenome, String toGenome, GenomeMappingManager genomeMappingManager) throws IOException
	{
		this(localFile, fromGenome, toGenome, genomeMappingManager, true);
	}
	
	public LocalFileGenomeMapping(File localFile, String fromGenome, String toGenome, GenomeMappingManager genomeMappingManager, boolean forwardMapping) throws IOException 
	{
		log.info("localFile: " + localFile);
		
		// store
		this.localFile = localFile;
		this.fromGenome = fromGenome;
		this.toGenome = toGenome;
		this.genomeMappingManager = genomeMappingManager;
		this.forwardMapping = forwardMapping;
		
		// create a memory mapping
		FileInputStream			is = new FileInputStream(localFile);
		long					offset = getFirstDataOffset(is);
		FileChannel				chan = is.getChannel();
		
		mappingSize = localFile.length() - offset;
		mapping = chan.map(FileChannel.MapMode.READ_ONLY, offset, mappingSize);
		if ( mappingSize > Integer.MAX_VALUE)
			throw new RuntimeException("mapping file too large. using ints for array access");
		
		log.info("mappingSize: " + mappingSize);
	}

	private long getFirstDataOffset(FileInputStream is) throws IOException
	{
		long				offset = 0;
		BufferedReader		reader = new BufferedReader(new InputStreamReader(is));
		String				line;
		
		// read until first non comment line
		while ( (line = reader.readLine()) != null )
		{
			if ( line.charAt(0) != '#' )
				break;
			else
				offset += line.length();
		}
		
		return offset;
		
	}

	@Override
	public GenomeLocus mapLocus(GenomeLocus locus) throws GenomeMapperException 
	{
		// map start and end. optimize for same
		GenomeLocus			startMapping = map(locus.getChr(), locus.getStart());
		GenomeLocus			endMapping;
		if ( locus.getStart() == locus.getEnd() )
			endMapping = startMapping;
		else
			endMapping = map(locus.getChr(), locus.getEnd());
		
		// must be on the same chromosome
		if ( !startMapping.getChr().equals(endMapping.getChr()) )
			throw new GenomeMapperException(locus, "start and end must map to same chromosome");
		
		// integrate and return
		return new GenomeLocus(startMapping.getChr(), startMapping.getStart(), endMapping.getStart());
	}

	public IGenomeMapper getReverseGenomeMapper() throws IOException
	{
		return new LocalFileGenomeMapping(localFile, fromGenome, toGenome, genomeMappingManager, !forwardMapping);
	}
	
	private GenomeLocus map(String chr, int loc) throws GenomeMapperException 
	{
		return map(chr, loc, 0, (int)mappingSize);
	}
	
	private GenomeLocus map(String chr, int loc, int rangeStart, int rangeEnd) throws GenomeMapperException
	{
		// if range is too small, its not going to get found
		int			rangeSize = rangeEnd - rangeStart;
		if ( rangeSize < MIN_MAPPING_LINE_SIZE )
			throw new GenomeMapperException(new GenomeLocus(chr, loc, 0), "no mapping found");
		
		// get entry
		int			midOffset = rangeStart + rangeSize / 2;
		String		toks[] = getMappingLineContainingOffset(midOffset);
		GenomeLocus	from = new GenomeLocus(toks[forwardMapping ? 0 : 1]);
		
		// compare choromosome part and recurse
		int			chrCompare = GenomeMapping.hybridCompare(chr, from.getChr());
		if ( chrCompare < 0 )
			return map(chr, loc, rangeStart, midOffset);
		else if ( chrCompare > 0 )
			return map(chr, loc, midOffset, rangeEnd);
		
		// on same chromosome. location below starting location? 
		if ( loc < from.getStart() )
			return map(chr, loc, rangeStart, midOffset);
		
		// location above ending location?
		if ( loc > from.getEnd() )
			return map(chr, loc, midOffset, rangeEnd);
		
		// found!, calculate relative position with the from range
		double		pos = (double)(loc - from.getStart()) / (from.getEnd() - from.getStart() );
		
		// generate 'to' location
		GenomeLocus	to = new GenomeLocus(toks[forwardMapping ? 1 : 0]);
		int			toLoc = to.getStart() + (int)Math.round(pos * (to.getEnd() - to.getStart()));
		
		// return (using only start field)
		return new GenomeLocus(to.getChr(), toLoc, 0);
	}

	private String[] getMappingLineContainingOffset(int offset)
	{
		// walk back to start of the line
		while ( offset > 0 )
		{
			// if previous char is a newline, then stop
			if ( mapping.get(offset - 1) == '\n' )
				break;
			
			// advance backwards
			offset--;
		}
		
		// grab mapping line and some more
		int			size = Math.min(MAX_MAPPING_LINE_SIZE, (int)mappingSize - offset);
		byte[]		bytes = new byte[size];
		mapping.position(offset);
		mapping.get(bytes);
		String		line = new String(bytes);
		String[]	toks = StringUtils.split(line, " \t\n\r");
		
		return toks;
	}
	
	public static File getLocalFileFor(GenomeDescriptor genomeDescriptor, String fileName) 
	{
		if ( !PreferenceManager.getInstance().getAsBoolean(PreferenceManager.GENOME_USE_LOCAL_FILE, true) ) 
			return null;
		
		if ( genomeDescriptor instanceof GenomeZipDescriptor )
		{
			// establish filenames
			File			genomeFile = new File(((GenomeZipDescriptor)genomeDescriptor).getGenomeZipFile().getName());
			File			localFile = new File(genomeFile.getAbsolutePath() + "_" + fileName);
			
			// check if not already there
			if ( localFile.exists() && (localFile.lastModified() > genomeFile.lastModified()) )
					return localFile;
			
			// copy into it
			log.info("creating local copy ... " + localFile);
			try
			{
				OutputStream	os = new FileOutputStream(localFile);
				InputStream		is = genomeDescriptor.getFileStream(fileName);
				IOUtils.copy(is, os);
				os.close();
				is.close();
				
			} catch (IOException e) {
				log.warn("", e);
				return null;
			}
			
			return localFile;
		}
		else
			return null;
	}
	
	@Override
	public String getMappingInfo() 
	{
		return String.format("%s: %d bytes", getClass().getSimpleName(), mappingSize);
	}

}
