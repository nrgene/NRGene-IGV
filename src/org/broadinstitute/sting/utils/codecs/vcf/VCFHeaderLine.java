package org.broadinstitute.sting.utils.codecs.vcf;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import org.broad.tribble.TribbleException.InternalCodecException;

public class VCFHeaderLine implements Comparable {
   protected static boolean ALLOW_UNBOUND_DESCRIPTIONS = true;
   protected static String UNBOUND_DESCRIPTION = "Not provided in original VCF header";
   private String mKey = null;
   private String mValue = null;

   public VCFHeaderLine(String key, String value) {
      if (key == null) {
         throw new IllegalArgumentException("VCFHeaderLine: key cannot be null: key = " + key);
      } else {
         this.mKey = key;
         this.mValue = value;
      }
   }

   public String getKey() {
      return this.mKey;
   }

   public String getValue() {
      return this.mValue;
   }

   public String toString() {
      return this.toStringEncoding();
   }

   protected String toStringEncoding() {
      return this.mKey + "=" + this.mValue;
   }

   public boolean equals(Object o) {
      if (!(o instanceof VCFHeaderLine)) {
         return false;
      } else {
         return this.mKey.equals(((VCFHeaderLine)o).getKey()) && this.mValue.equals(((VCFHeaderLine)o).getValue());
      }
   }

   public int compareTo(Object other) {
      return this.toString().compareTo(other.toString());
   }

   public static boolean isHeaderLine(String line) {
      return line != null && line.length() > 0 && "#".equals(line.substring(0, 1));
   }

   public static String toStringEncoding(Map keyValues) {
      StringBuilder builder = new StringBuilder();
      builder.append("<");
      boolean start = true;
      Iterator j = keyValues.entrySet().iterator();

      while(j.hasNext()) {
         Entry entry = (Entry)j.next();
         if (start) {
            start = false;
         } else {
            builder.append(",");
         }

         if (entry.getValue() == null) {
            throw new InternalCodecException("Header problem: unbound value at " + entry + " from " + keyValues);
         }

         builder.append((String)entry.getKey());
         builder.append("=");
         builder.append(!entry.getValue().toString().contains(",") && !entry.getValue().toString().contains(" ") && !((String)entry.getKey()).equals("Description") ? entry.getValue() : "\"" + entry.getValue() + "\"");
      }

      builder.append(">");
      return builder.toString();
   }
}
