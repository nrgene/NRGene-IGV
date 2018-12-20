package org.broad.igv.track;

public interface SoftAttributeProvider {

	String		getAttribute(AbstractTrack track, String attrName, String providerParam, String locus) throws Exception;
}
