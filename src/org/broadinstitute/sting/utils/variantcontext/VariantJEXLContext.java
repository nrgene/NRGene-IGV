package org.broadinstitute.sting.utils.variantcontext;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.jexl2.JexlContext;

class VariantJEXLContext implements JexlContext {
   private VariantContext vc;
   private static Map x = new HashMap();

   public VariantJEXLContext(VariantContext vc) {
      this.vc = vc;
   }

   public Object get(String name) {
      Object result = null;
      if (x.containsKey(name)) {
         result = ((VariantJEXLContext.AttributeGetter)x.get(name)).get(this.vc);
      } else if (this.vc.hasAttribute(name)) {
         result = this.vc.getAttribute(name);
      } else if (this.vc.getFilters().contains(name)) {
         result = "1";
      }

      return result;
   }

   public boolean has(String name) {
      return this.get(name) != null;
   }

   public void set(String name, Object value) {
      throw new UnsupportedOperationException("remove() not supported on a VariantJEXLContext");
   }

   static {
      x.put("vc", new VariantJEXLContext.AttributeGetter() {
         public Object get(VariantContext vc) {
            return vc;
         }
      });
      x.put("CHROM", new VariantJEXLContext.AttributeGetter() {
         public Object get(VariantContext vc) {
            return vc.getChr();
         }
      });
      x.put("POS", new VariantJEXLContext.AttributeGetter() {
         public Object get(VariantContext vc) {
            return vc.getStart();
         }
      });
      x.put("TYPE", new VariantJEXLContext.AttributeGetter() {
         public Object get(VariantContext vc) {
            return vc.getType().toString();
         }
      });
      x.put("QUAL", new VariantJEXLContext.AttributeGetter() {
         public Object get(VariantContext vc) {
            return -10.0D * vc.getLog10PError();
         }
      });
      x.put("ALLELES", new VariantJEXLContext.AttributeGetter() {
         public Object get(VariantContext vc) {
            return vc.getAlleles();
         }
      });
      x.put("N_ALLELES", new VariantJEXLContext.AttributeGetter() {
         public Object get(VariantContext vc) {
            return vc.getNAlleles();
         }
      });
      x.put("FILTER", new VariantJEXLContext.AttributeGetter() {
         public Object get(VariantContext vc) {
            return vc.isFiltered() ? "1" : "0";
         }
      });
      x.put("homRefCount", new VariantJEXLContext.AttributeGetter() {
         public Object get(VariantContext vc) {
            return vc.getHomRefCount();
         }
      });
      x.put("hetCount", new VariantJEXLContext.AttributeGetter() {
         public Object get(VariantContext vc) {
            return vc.getHetCount();
         }
      });
      x.put("homVarCount", new VariantJEXLContext.AttributeGetter() {
         public Object get(VariantContext vc) {
            return vc.getHomVarCount();
         }
      });
   }

   private interface AttributeGetter {
      Object get(VariantContext var1);
   }
}
