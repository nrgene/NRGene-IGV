package org.broadinstitute.sting.utils.codecs.vcf;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class VCF4Parser implements VCFLineParser {
   public Map parseLine(String valueLine, List expectedTagOrder) {
      Map ret = new LinkedHashMap();
      StringBuilder builder = new StringBuilder();
      String key = "";
      int index = 0;
      boolean inQuote = false;
      char[] arr = valueLine.toCharArray();
      int len = arr.length;

      for(int j = 0; j < len; ++j) {
         char c = arr[j];
         if (c == '"') {
            inQuote = !inQuote;
         } else if (inQuote) {
            builder.append(c);
         } else {
            switch(c) {
            case ',':
               ret.put(key, builder.toString().trim());
               builder = new StringBuilder();
               break;
            case '<':
               if (index == 0) {
                  break;
               }
            case '>':
               if (index == valueLine.length() - 1) {
                  ret.put(key, builder.toString().trim());
               }
               break;
            case '=':
               key = builder.toString().trim();
               builder = new StringBuilder();
               break;
            default:
               builder.append(c);
            }
         }

         ++index;
      }

      index = 0;
      if (expectedTagOrder != null) {
         if (ret.size() > expectedTagOrder.size()) {
            throw new IllegalArgumentException("Unexpected tag count " + ret.size() + " in string " + expectedTagOrder.size());
         }

         for(Iterator j = ret.keySet().iterator(); j.hasNext(); ++index) {
            String str = (String)j.next();
            if (!((String)expectedTagOrder.get(index)).equals(str)) {
               throw new IllegalArgumentException("Unexpected tag " + str + " in string " + valueLine);
            }
         }
      }

      return ret;
   }
}
