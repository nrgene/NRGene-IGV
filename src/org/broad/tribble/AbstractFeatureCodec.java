package org.broad.tribble;

import org.broad.igv.feature.genome.Genome;


/**
 * Created by IntelliJ IDEA.
 * User: jrobinso
 * Date: Dec 22, 2009
 * Time: 9:12:43 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class AbstractFeatureCodec<T extends Feature> implements FeatureCodec {

    public boolean canDecode(final String path) { return false; }

	@Override
	public Feature decode(String line, Genome genome) {
		return decode(line);
	}
    

}
