package org.broadinstitute.sting.utils.codecs.vcf;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class VCF3Parser implements VCFLineParser {
   public Map parseLine(String valueLine, List expectedTagOrder) {
      Map ret = new LinkedHashMap();
      StringBuilder builder = new StringBuilder();
      int index = 0;
      int tagIndex = 0;
      boolean inQuote = false;
      char[] arr$ = valueLine.toCharArray();
      int len$ = arr$.length;

      for(int j = 0; j < len$; ++j) {
         char c = arr$[j];
         switch(c) {
         case '"':
            inQuote = !inQuote;
            break;
         case ',':
            if (!inQuote) {
               ret.put(expectedTagOrder.get(tagIndex++), builder.toString());
               builder = new StringBuilder();
               break;
            }
         default:
            builder.append(c);
         }

         ++index;
      }

      ret.put(expectedTagOrder.get(tagIndex++), builder.toString());
      index = 0;
      if (tagIndex != expectedTagOrder.size()) {
         throw new IllegalArgumentException("Unexpected tag count " + tagIndex + ", we expected " + expectedTagOrder.size());
      } else {
         for(Iterator j = ret.keySet().iterator(); j.hasNext(); ++index) {
            String str = (String)j.next();
            if (!((String)expectedTagOrder.get(index)).equals(str)) {
               throw new IllegalArgumentException("Unexpected tag " + str + " in string " + valueLine);
            }
         }

         return ret;
      }
   }
}
