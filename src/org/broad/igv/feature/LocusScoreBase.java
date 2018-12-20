package org.broad.igv.feature;

import org.broad.igv.feature.genome.Genome;
import org.broad.igv.track.WindowFunction;

public abstract class LocusScoreBase implements LocusScore {

	@Override
	public String getValueString(double position, WindowFunction windowFunction, Genome genome) 
	{
		return getValueString(position, windowFunction);
	}

}
