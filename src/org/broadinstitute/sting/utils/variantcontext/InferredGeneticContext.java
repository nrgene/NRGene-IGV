package org.broadinstitute.sting.utils.variantcontext;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public final class InferredGeneticContext {
   public static final double NO_NEG_LOG_10PERROR = -1.0D;
   private static Set NO_FILTERS = Collections.unmodifiableSet(new HashSet());
   private static Map NO_ATTRIBUTES = Collections.unmodifiableMap(new HashMap());
   private double negLog10PError = -1.0D;
   private String name = null;
   private Set filters;
   private Map attributes;

   public InferredGeneticContext(String name, double negLog10PError, Set filters, Map attributes) {
      this.filters = NO_FILTERS;
      this.attributes = NO_ATTRIBUTES;
      this.name = name;
      this.setNegLog10PError(negLog10PError);
      if (filters != null) {
         this.setFilters(filters);
      }

      if (attributes != null) {
         this.setAttributes(attributes);
      }

   }

   public String getName() {
      return this.name;
   }

   public void setName(String name) {
      if (name == null) {
         throw new IllegalArgumentException("Name cannot be null " + this);
      } else {
         this.name = name;
      }
   }

   public Set getFilters() {
      return Collections.unmodifiableSet(this.filters);
   }

   public boolean isFiltered() {
      return this.filters.size() > 0;
   }

   public boolean isNotFiltered() {
      return !this.isFiltered();
   }

   public void addFilter(String filter) {
      if (this.filters == NO_FILTERS) {
         this.filters = new HashSet(this.filters);
      }

      if (filter == null) {
         throw new IllegalArgumentException("BUG: Attempting to add null filter " + this);
      } else if (this.getFilters().contains(filter)) {
         throw new IllegalArgumentException("BUG: Attempting to add duplicate filter " + filter + " at " + this);
      } else {
         this.filters.add(filter);
      }
   }

   public void addFilters(Collection filters) {
      if (filters == null) {
         throw new IllegalArgumentException("BUG: Attempting to add null filters at" + this);
      } else {
         Iterator j = filters.iterator();

         while(j.hasNext()) {
            String f = (String)j.next();
            this.addFilter(f);
         }

      }
   }

   public void clearFilters() {
      this.filters = new HashSet();
   }

   public void setFilters(Collection filters) {
      this.clearFilters();
      this.addFilters(filters);
   }

   public boolean hasNegLog10PError() {
      return this.getNegLog10PError() != -1.0D;
   }

   public double getNegLog10PError() {
      return this.negLog10PError;
   }

   public double getPhredScaledQual() {
      return this.getNegLog10PError() * 10.0D;
   }

   public void setNegLog10PError(double negLog10PError) {
      if (negLog10PError < 0.0D && negLog10PError != -1.0D) {
         throw new IllegalArgumentException("BUG: negLog10PError cannot be < than 0 : " + negLog10PError);
      } else if (Double.isInfinite(negLog10PError)) {
         throw new IllegalArgumentException("BUG: negLog10PError should not be Infinity");
      } else if (Double.isNaN(negLog10PError)) {
         throw new IllegalArgumentException("BUG: negLog10PError should not be NaN");
      } else {
         this.negLog10PError = negLog10PError;
      }
   }

   public void clearAttributes() {
      this.attributes = new HashMap();
   }

   public Map getAttributes() {
      return Collections.unmodifiableMap(this.attributes);
   }

   public void setAttributes(Map map) {
      this.clearAttributes();
      this.putAttributes(map);
   }

   public void putAttribute(String key, Object value) {
      this.putAttribute(key, value, false);
   }

   public void putAttribute(String key, Object value, boolean allowOverwrites) {
      if (!allowOverwrites && this.hasAttribute(key)) {
         throw new IllegalStateException("Attempting to overwrite key->value binding: key = " + key + " this = " + this);
      } else {
         if (this.attributes == NO_ATTRIBUTES) {
            this.attributes = new HashMap();
         }

         this.attributes.put(key, value);
      }
   }

   public void removeAttribute(String key) {
      if (this.attributes == NO_ATTRIBUTES) {
         this.attributes = new HashMap();
      }

      this.attributes.remove(key);
   }

   public void putAttributes(Map map) {
      if (map != null) {
         if (this.attributes.size() == 0) {
            if (this.attributes == NO_ATTRIBUTES) {
               this.attributes = new HashMap();
            }

            this.attributes.putAll(map);
         } else {
            Iterator j = map.entrySet().iterator();

            while(j.hasNext()) {
               Entry elt = (Entry)j.next();
               this.putAttribute((String)elt.getKey(), elt.getValue(), false);
            }
         }
      }

   }

   public boolean hasAttribute(String key) {
      return this.attributes.containsKey(key);
   }

   public int getNumAttributes() {
      return this.attributes.size();
   }

   public Object getAttribute(String key) {
      return this.attributes.get(key);
   }

   public Object getAttribute(String key, Object defaultValue) {
      return this.hasAttribute(key) ? this.attributes.get(key) : defaultValue;
   }

   public String getAttributeAsString(String key) {
      return String.valueOf(this.getAttribute(key));
   }

   public int getAttributeAsInt(String key) {
      Object x = this.getAttribute(key);
      return x instanceof Integer ? (Integer)x : Integer.valueOf((String)x);
   }

   public double getAttributeAsDouble(String key) {
      Object x = this.getAttribute(key);
      return x instanceof Double ? (Double)x : Double.valueOf((String)x);
   }

   public boolean getAttributeAsBoolean(String key) {
      Object x = this.getAttribute(key);
      return x instanceof Boolean ? (Boolean)x : Boolean.valueOf((String)x);
   }

   public String getAttributeAsString(String key, String defaultValue) {
      return (String)this.getAttribute(key, defaultValue);
   }

   public int getAttributeAsInt(String key, int defaultValue) {
      return (Integer)this.getAttribute(key, defaultValue);
   }

   public double getAttributeAsDouble(String key, double defaultValue) {
      return (Double)this.getAttribute(key, defaultValue);
   }

   public boolean getAttributeAsBoolean(String key, boolean defaultValue) {
      return (Boolean)this.getAttribute(key, defaultValue);
   }

   public Integer getAttributeAsIntegerNoException(String key) {
      try {
         return this.getAttributeAsInt(key);
      } catch (Exception var3) {
         return null;
      }
   }

   public Double getAttributeAsDoubleNoException(String key) {
      try {
         return this.getAttributeAsDouble(key);
      } catch (Exception var3) {
         return null;
      }
   }

   public String getAttributeAsStringNoException(String key) {
      return this.getAttribute(key) == null ? null : this.getAttributeAsString(key);
   }

   public Boolean getAttributeAsBooleanNoException(String key) {
      try {
         return this.getAttributeAsBoolean(key);
      } catch (Exception var3) {
         return null;
      }
   }
}
