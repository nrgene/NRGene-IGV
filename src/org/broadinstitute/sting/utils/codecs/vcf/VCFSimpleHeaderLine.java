package org.broadinstitute.sting.utils.codecs.vcf;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class VCFSimpleHeaderLine extends VCFHeaderLine implements VCFIDHeaderLine {
   private String name;
   private Map genericFields = new LinkedHashMap();

   public VCFSimpleHeaderLine(String key, String name, Map genericFields) {
      super(key, "");
      this.initialize(name, genericFields);
   }

   public VCFSimpleHeaderLine(String key, String name, String description) {
      super(key, "");
      Map map = new LinkedHashMap(1);
      map.put("Description", description);
      this.initialize(name, map);
   }

   protected VCFSimpleHeaderLine(String line, VCFHeaderVersion version, String key, List expectedTagOrdering) {
      super(key, "");
      Map mapping = VCFHeaderLineTranslator.parseLine(version, line, expectedTagOrdering);
      this.name = (String)mapping.get("ID");
      this.initialize(this.name, mapping);
   }

   protected void initialize(String name, Map genericFields) {
      if (name != null && genericFields != null && !genericFields.isEmpty()) {
         this.name = name;
         this.genericFields.putAll(genericFields);
      } else {
         throw new IllegalArgumentException(String.format("Invalid VCFSimpleHeaderLine: key=%s name=%s", super.getKey(), name));
      }
   }

   protected String toStringEncoding() {
      Map map = new LinkedHashMap();
      map.put("ID", this.name);
      map.putAll(this.genericFields);
      return this.getKey() + "=" + VCFHeaderLine.toStringEncoding(map);
   }

   public boolean equals(Object o) {
      if (!(o instanceof VCFSimpleHeaderLine)) {
         return false;
      } else {
         VCFSimpleHeaderLine other = (VCFSimpleHeaderLine)o;
         if (this.name.equals(other.name) && this.genericFields.size() == other.genericFields.size()) {
            Iterator j = this.genericFields.entrySet().iterator();

            Entry entry;
            do {
               if (!j.hasNext()) {
                  return true;
               }

               entry = (Entry)j.next();
            } while(((String)entry.getValue()).equals(other.genericFields.get(entry.getKey())));

            return false;
         } else {
            return false;
         }
      }
   }

   public String getID() {
      return this.name;
   }

   public Map getGenericFields() {
      return this.genericFields;
   }
}
