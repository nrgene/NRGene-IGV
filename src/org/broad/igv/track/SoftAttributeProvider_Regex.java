package org.broad.igv.track;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class SoftAttributeProvider_Regex implements SoftAttributeProvider {
	
    static private Logger 						log = Logger.getLogger(SoftAttributeProvider_Regex.class);
    
	private class RegexExpr {
		
		Pattern			pattern;
		String			output;
	};
	
	private Map<String, RegexExpr>				regexExprMap = new LinkedHashMap<String, SoftAttributeProvider_Regex.RegexExpr>();
	

	public String getAttribute(AbstractTrack track, String attrName, String providerParam, String locus) throws Exception
	{
		String		trackKey = track.getResourceLocator().getPath();
		
		log.debug("Regex provider: trackKey: " + trackKey + ", attrName: " + attrName + ", providerParam: " + providerParam + ", locus:" + locus);

		// isolate filename
		String				filename = new File(trackKey).getName();
		
		// get regex expression
		RegexExpr			re = getRegexExpr(providerParam);
		if ( re == null )
			return "?Err?";
		
		// match
		Matcher				m = re.pattern.matcher(filename);
		if ( !m.find() )
			return "";
		
		// generate output
		String				value = m.replaceFirst(re.output);
		log.debug("Regex provider: value: " + value);
		
		return value;
	}


	private RegexExpr getRegexExpr(String providerParam) 
	{
		RegexExpr			re = regexExprMap.get(providerParam);
		
		if ( re == null )
		{
			// parse pattern -> output
			String[]		toks = providerParam.split("->");
			if ( toks.length != 2 )
			{
				log.warn("failed to parse: " + providerParam);
				return null;
			}
			
			re = new RegexExpr();
			re.pattern = Pattern.compile(toks[0].trim());
			re.output = toks[1].trim();
			
			regexExprMap.put(providerParam, re);
		}
		
		return re;
	}
}
