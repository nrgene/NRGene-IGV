/*
 * Copyright (c) 2007-2011 by The Broad Institute of MIT and Harvard.  All Rights Reserved.
 *
 * This software is licensed under the terms of the GNU Lesser General Public License (LGPL),
 * Version 2.1 which is available at http://www.opensource.org/licenses/lgpl-2.1.php.
 *
 * THE SOFTWARE IS PROVIDED "AS IS." THE BROAD AND MIT MAKE NO REPRESENTATIONS OR
 * WARRANTES OF ANY KIND CONCERNING THE SOFTWARE, EXPRESS OR IMPLIED, INCLUDING,
 * WITHOUT LIMITATION, WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, NONINFRINGEMENT, OR THE ABSENCE OF LATENT OR OTHER DEFECTS, WHETHER
 * OR NOT DISCOVERABLE.  IN NO EVENT SHALL THE BROAD OR MIT, OR THEIR RESPECTIVE
 * TRUSTEES, DIRECTORS, OFFICERS, EMPLOYEES, AND AFFILIATES BE LIABLE FOR ANY DAMAGES
 * OF ANY KIND, INCLUDING, WITHOUT LIMITATION, INCIDENTAL OR CONSEQUENTIAL DAMAGES,
 * ECONOMIC DAMAGES OR INJURY TO PROPERTY AND LOST PROFITS, REGARDLESS OF WHETHER
 * THE BROAD OR MIT SHALL BE ADVISED, SHALL HAVE OTHER REASON TO KNOW, OR IN FACT
 * SHALL KNOW OF THE POSSIBILITY OF THE FOREGOING.
 */

package org.broad.igv.feature.tribble;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.broad.igv.feature.genome.Genome;
import org.broad.igv.variant.vcf.VCFVariant;
import org.broad.tribble.Feature;
import org.broad.tribble.FeatureCodec;
import org.broad.tribble.readers.LineReader;
import org.broadinstitute.sting.utils.variantcontext.Allele;
import org.broadinstitute.sting.utils.variantcontext.GenotypesContext;
import org.broadinstitute.sting.utils.variantcontext.VariantContext;

/**
 * @author Jim Robinson
 * @date Aug 1, 2011
 */
public class VCFWrapperCodec implements FeatureCodec {

	static public class FractionVariantContext extends VariantContext
	{
		private double		startFraction;
		private double		stopFraction;
		private int			overrideEnd = -1;

		protected FractionVariantContext(String source, String ID, String contig,
				long start, long stop, Collection<Allele> alleles,
				GenotypesContext genotypes, double log10pError,
				Set<String> filters, Map<String, Object> attributes,
				Byte referenceBaseForIndel,
				EnumSet<Validation> validationToPerform) {
			super(source, ID, contig, start, stop, alleles, genotypes, log10pError,
					filters, attributes, referenceBaseForIndel, validationToPerform);
		}

	    protected FractionVariantContext(VariantContext other) {
	    	super(other);
	    	if ( other instanceof FractionVariantContext )
	    	{
	    		startFraction = ((FractionVariantContext)other).startFraction;
	    		stopFraction = ((FractionVariantContext)other).stopFraction;
	    	}
	    }

		public double getStartFraction() {
			return startFraction;
		}

		public void setStartFraction(double startFraction) {
			this.startFraction = startFraction;
		}

		public double getStopFraction() {
			return stopFraction;
		}

		public void setStopFraction(double stopFraction) {
			this.stopFraction = stopFraction;
		}

		@Override
		public int getEnd() 
		{
			if ( overrideEnd >= 0 )
				return overrideEnd;
			else
				return super.getEnd();
		}

		public void setOverrideEnd(int overrideEnd) {
			this.overrideEnd = overrideEnd;
		}

	}
	
    FeatureCodec wrappedCodec;
	double	startFraction[] = new double[1];
	double	stopFraction[] = new double[1];


    public VCFWrapperCodec(FeatureCodec wrappedCodec) {
       this.wrappedCodec = wrappedCodec;
    }

    public Feature decodeLoc(String line) {
        return wrappedCodec.decodeLoc(line);
    }

    public Feature decode(String line) {
    	return decode(line, null);
    }

    public Feature decode(String line, Genome genome) {
    	
    	int			overrideEnd = -1;
    	
    	if ( genome.isGeneticMap() && !StringUtils.isEmpty(line) && line.charAt(0) != '#' )
    	{
    		String		parts[] = line.split("\\\t");
    		if ( parts.length > 2 )
    		{
    			int			start = genome.parseIntCoordinate(parts[1], startFraction);
    			
    			overrideEnd = genome.parseIntCoordinate(Integer.toString(Integer.parseInt(parts[1]) + 1), stopFraction);
    			
    			parts[1] = Integer.toString(start);
    			
    			line = StringUtils.join(parts, "\t");
    		}
    		else
    		{
    			stopFraction[0] = startFraction[0] = 0;
    		}
    	}
    	
        VariantContext vc = (VariantContext) wrappedCodec.decode(line);
        
        if ( vc != null && genome.isGeneticMap() )
        {
        	FractionVariantContext		fvc = new FractionVariantContext(vc);
        	
        	fvc.setStartFraction(startFraction[0]);
        	fvc.setStopFraction(stopFraction[0]);
        	fvc.setOverrideEnd(overrideEnd);
        	
        	return new VCFVariant(fvc);
        }
        
        return vc == null ? null : new VCFVariant(vc);
    }

    public Class getFeatureType() {
        return VCFVariant.class;
    }

    public Object readHeader(LineReader reader) {
        return wrappedCodec.readHeader(reader);
    }

    /**
     * This function returns true iff the File potentialInput can be parsed by this
     * codec.
     * <p/>
     * There is an assumption that there's never a situation where two different Codecs
     * return true for the same file.  If this occurs, the recommendation would be to error out.
     * <p/>
     * Note this function must never throw an error.  All errors should be trapped
     * and false returned.
     *
     * @param path the file to test for parsability with this codec
     * @return true if potentialInput can be parsed, false otherwise
     */
    public boolean canDecode(String path) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
