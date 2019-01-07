/*jadclipse*/// Decompiled by Jad v1.5.8g. Copyright 2001 Pavel Kouznetsov.
// Jad home page: http://www.kpdus.com/jad.html
// Decompiler options: packimports(3) radix(10) lradix(10) 
// Source File Name:   CommonInfo.java

// This source file is based on a file extracted from vcf.jar size 195323 bytes, with the following manifest:
// META-INF/MANIFEST.MF size 14337 bytes date Oct 25  2012 

package org.broadinstitute.sting.utils.variantcontext;

import java.util.*;

import org.broad.igv.variant.vcf.VCFVariant;

final class CommonInfo
{

    public CommonInfo(String name, double log10PError, Set filters, Map attributes)
    {
        this.log10PError = 1.0D;
        this.name = null;
        this.filters = null;
        this.attributes = NO_ATTRIBUTES;
        this.name = name;
        setLog10PError(log10PError);
        this.filters = filters;
        if(attributes != null && !attributes.isEmpty())
            this.attributes = VCFVariant.fixupMap(attributes);
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        if(name == null)
        {
            throw new IllegalArgumentException((new StringBuilder()).append("Name cannot be null ").append(this).toString());
        } else
        {
            this.name = name;
            return;
        }
    }

    public Set getFiltersMaybeNull()
    {
        return filters;
    }

    public Set getFilters()
    {
        return filters != null ? Collections.unmodifiableSet(filters) : NO_FILTERS;
    }

    public boolean filtersWereApplied()
    {
        return filters != null;
    }

    public boolean isFiltered()
    {
        return filters != null ? filters.size() > 0 : false;
    }

    public boolean isNotFiltered()
    {
        return !isFiltered();
    }

    public void addFilter(String filter)
    {
        if(filters == null)
            filters = new HashSet();
        if(filter == null)
            throw new IllegalArgumentException((new StringBuilder()).append("BUG: Attempting to add null filter ").append(this).toString());
        if(getFilters().contains(filter))
        {
            throw new IllegalArgumentException((new StringBuilder()).append("BUG: Attempting to add duplicate filter ").append(filter).append(" at ").append(this).toString());
        } else
        {
            filters.add(filter);
            return;
        }
    }

    public void addFilters(Collection filters)
    {
        if(filters == null)
            throw new IllegalArgumentException((new StringBuilder()).append("BUG: Attempting to add null filters at").append(this).toString());
        String f;
        for(Iterator j = filters.iterator(); j.hasNext(); addFilter(f))
            f = (String)j.next();

    }

    public boolean hasLog10PError()
    {
        return getLog10PError() != 1.0D;
    }

    public double getLog10PError()
    {
        return log10PError;
    }

    public double getPhredScaledQual()
    {
        return getLog10PError() * -10D;
    }

    public void setLog10PError(double log10PError)
    {
        if(log10PError > 0.0D && log10PError != 1.0D)
            throw new IllegalArgumentException((new StringBuilder()).append("BUG: log10PError cannot be > 0 : ").append(this.log10PError).toString());
        if(Double.isInfinite(this.log10PError))
            throw new IllegalArgumentException("BUG: log10PError should not be Infinity");
        if(Double.isNaN(this.log10PError))
        {
            throw new IllegalArgumentException("BUG: log10PError should not be NaN");
        } else
        {
            this.log10PError = log10PError;
            return;
        }
    }

    public void clearAttributes()
    {
        attributes = new HashMap();
    }

    public Map getAttributes()
    {
        return Collections.unmodifiableMap(attributes);
    }

    public void setAttributes(Map map)
    {
        clearAttributes();
        putAttributes(map);
    }

    public void putAttribute(String key, Object value)
    {
        putAttribute(key, value, false);
    }

    public void putAttribute(String key, Object value, boolean allowOverwrites)
    {
        if(!allowOverwrites && hasAttribute(key))
            throw new IllegalStateException((new StringBuilder()).append("Attempting to overwrite key->value binding: key = ").append(key).append(" this = ").append(this).toString());
        if(attributes == NO_ATTRIBUTES)
            attributes = new HashMap();
        attributes.put(key, value);
    }

    public void removeAttribute(String key)
    {
        if(attributes == NO_ATTRIBUTES)
            attributes = new HashMap();
        attributes.remove(key);
    }

    public void putAttributes(Map map)
    {
        if(map != null)
            if(attributes.size() == 0)
            {
                if(attributes == NO_ATTRIBUTES)
                    attributes = new HashMap();
                attributes.putAll(map);
            } else
            {
                java.util.Map.Entry elt;
                for(Iterator j = map.entrySet().iterator(); j.hasNext(); putAttribute((String)elt.getKey(), elt.getValue(), false))
                    elt = (java.util.Map.Entry)j.next();

            }
    }

    public boolean hasAttribute(String key)
    {
        return attributes.containsKey(key);
    }

    public int getNumAttributes()
    {
        return attributes.size();
    }

    public Object getAttribute(String key)
    {
        return attributes.get(key);
    }

    public Object getAttribute(String key, Object defaultValue)
    {
        if(hasAttribute(key))
            return attributes.get(key);
        else
            return defaultValue;
    }

    public String getAttributeAsString(String key, String defaultValue)
    {
        Object x = getAttribute(key);
        if(x == null)
            return defaultValue;
        if(x instanceof String)
            return (String)x;
        else
            return String.valueOf(x);
    }

    public int getAttributeAsInt(String key, int defaultValue)
    {
        Object x = getAttribute(key);
        if(x == null || x == ".")
            return defaultValue;
        if(x instanceof Integer)
            return ((Integer)x).intValue();
        else
            return Integer.valueOf((String)x).intValue();
    }

    public double getAttributeAsDouble(String key, double defaultValue)
    {
        Object x = getAttribute(key);
        if(x == null)
            return defaultValue;
        if(x instanceof Double)
            return ((Double)x).doubleValue();
        else
            return Double.valueOf((String)x).doubleValue();
    }

    public boolean getAttributeAsBoolean(String key, boolean defaultValue)
    {
        Object x = getAttribute(key);
        if(x == null)
            return defaultValue;
        if(x instanceof Boolean)
            return ((Boolean)x).booleanValue();
        else
            return Boolean.valueOf((String)x).booleanValue();
    }

    public static final double NO_LOG10_PERROR = 1D;
    private static Set NO_FILTERS = Collections.emptySet();
    private static Map NO_ATTRIBUTES = Collections.unmodifiableMap(new HashMap());
    private double log10PError;
    private String name;
    private Set filters;
    private Map attributes;

}


/*
	DECOMPILATION REPORT

	Decompiled from: /Users/drorkessler/Documents/projects/Nrgene/IGV-Revisit/NrgeneVer/IGV/IGVDistribution_2.0.35_Nrgene/lib/vcf.jar
	Total time: 36 ms
	Jad reported messages/errors:
	Exit status: 0
	Caught exceptions:
*/
