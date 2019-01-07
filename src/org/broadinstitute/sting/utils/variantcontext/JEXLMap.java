/*
package org.broadinstitute.sting.utils.variantcontext;

import org.apache.commons.jexl2.JexlContext;
import org.apache.commons.jexl2.MapContext;
import org.broadinstitute.sting.utils.Utils;
import org.broadinstitute.sting.utils.exceptions.UserException;

import java.util.*;

class JEXLMap implements Map<VariantContextUtils.JexlVCMatchExp, Boolean> {
   private final VariantContext vc;
   private final Genotype g;
   private JexlContext jContext;
   private Map<VariantContextUtils.JexlVCMatchExp, Boolean> jexl;

   public JEXLMap(Collection jexlCollection, VariantContext vc, Genotype g) {
      this.jContext = null;
      this.vc = vc;
      this.g = g;
      this.initialize(jexlCollection);
   }

   public JEXLMap(Collection jexlCollection, VariantContext vc) {
      this(jexlCollection, vc, (Genotype)null);
   }

   private void initialize(Collection jexlCollection) {
      this.jexl = new HashMap();
      Iterator j = jexlCollection.iterator();

      while(j.hasNext()) {
         VariantContextUtils.JexlVCMatchExp exp = (VariantContextUtils.JexlVCMatchExp)j.next();
         this.jexl.put(exp, null);
      }

   }

   private void createContext() {
      if (this.g == null) {
         this.jContext = new VariantJEXLContext(this.vc);
      } else {
         Map infoMap = new HashMap();
         if (this.vc != null) {
            infoMap.put("CHROM", this.vc.getChr());
            infoMap.put("POS", this.vc.getStart());
            infoMap.put("TYPE", this.vc.getType().toString());
            infoMap.put("QUAL", String.valueOf(this.vc.getPhredScaledQual()));
            infoMap.put("ALLELES", Utils.join(";", this.vc.getAlleles()));
            infoMap.put("N_ALLELES", String.valueOf(this.vc.getNAlleles()));
            addAttributesToMap(infoMap, this.vc.getAttributes());
            infoMap.put("FILTER", this.vc.isFiltered() ? "1" : "0");
            Iterator j = this.vc.getFilters().iterator();

            while(j.hasNext()) {
               Object filterCode = (String)j.next();
               infoMap.put(String.valueOf(filterCode), "1");
            }

            infoMap.put("GT", this.g.getGenotypeString());
            infoMap.put("isHomRef", this.g.isHomRef() ? "1" : "0");
            infoMap.put("isHet", this.g.isHet() ? "1" : "0");
            infoMap.put("isHomVar", this.g.isHomVar() ? "1" : "0");
            infoMap.put("GQ", new Double(this.g.getPhredScaledQual()));
            j = this.g.getAttributes().entrySet().iterator();

            while(j.hasNext()) {
               Entry e = (Entry)j.next();
               if (e.getValue() != null && !e.getValue().equals(".")) {
                  infoMap.put(e.getKey(), e.getValue());
               }
            }
         }

         this.jContext = new MapContext(infoMap);
      }

   }

   public int size() {
      return this.jexl.size();
   }

   public boolean isEmpty() {
      return this.jexl.isEmpty();
   }

   public boolean containsKey(Object o) {
      return this.jexl.containsKey(o);
   }

   public Boolean get(Object o) {
      if (this.jexl.containsKey(o) && this.jexl.get(o) != null) {
         return (Boolean)this.jexl.get(o);
      } else {
         VariantContextUtils.JexlVCMatchExp e = (VariantContextUtils.JexlVCMatchExp)o;
         this.evaluateExpression(e);
         return (Boolean)this.jexl.get(e);
      }
   }

   public Set keySet() {
      return this.jexl.keySet();
   }

   public Collection values() {
      Iterator j = this.jexl.keySet().iterator();

      while(j.hasNext()) {
         VariantContextUtils.JexlVCMatchExp exp = (VariantContextUtils.JexlVCMatchExp)j.next();
         if (this.jexl.get(exp) == null) {
            this.evaluateExpression(exp);
         }
      }

      return this.jexl.values();
   }

   private void evaluateExpression(VariantContextUtils.JexlVCMatchExp exp) {
      if (this.jContext == null) {
         this.createContext();
      }

      try {
         this.jexl.put(exp, (Boolean)exp.exp.evaluate(this.jContext));
      } catch (Exception var3) {
         throw new UserException.CommandLineException(String.format("Invalid JEXL expression detected for %s with message %s", exp.name, var3.getMessage()));
      }
   }

   private static void addAttributesToMap(Map infoMap, Map attributes) {
      Iterator j = attributes.entrySet().iterator();

      while(j.hasNext()) {
         Entry e = (Entry)j.next();
         infoMap.put(e.getKey(), String.valueOf(e.getValue()));
      }

   }

   public Boolean put(VariantContextUtils.JexlVCMatchExp jexlVCMatchExp, Boolean aBoolean) {
      return (Boolean)this.jexl.put(jexlVCMatchExp, aBoolean);
   }

   public void putAll(Map map) {
      this.jexl.putAll(map);
   }

   public boolean containsValue(Object o) {
      throw new UnsupportedOperationException("containsValue() not supported on a JEXLMap");
   }

   public Boolean remove(Object o) {
      throw new UnsupportedOperationException("remove() not supported on a JEXLMap");
   }

   public Set entrySet() {
      throw new UnsupportedOperationException("clear() not supported on a JEXLMap");
   }

   public void clear() {
      throw new UnsupportedOperationException("clear() not supported on a JEXLMap");
   }
}
*/
