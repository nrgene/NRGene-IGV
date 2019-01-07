/*
package org.broadinstitute.sting.utils.codecs.vcf;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.apache.log4j.Logger;
import org.broadinstitute.sting.commandline.RodBinding;
import org.broadinstitute.sting.gatk.GenomeAnalysisEngine;
import org.broadinstitute.sting.gatk.datasources.rmd.ReferenceOrderedDataSource;
import org.broadinstitute.sting.utils.variantcontext.VariantContext;

public class VCFUtils {
   private VCFUtils() {
   }

   public static Map getVCFHeadersFromRods(GenomeAnalysisEngine toolkit, List rodBindings) {
      Set names = new TreeSet();
      Iterator j = rodBindings.iterator();

      while(j.hasNext()) {
         RodBinding evalRod = (RodBinding)j.next();
         names.add(evalRod.getName());
      }

      return getVCFHeadersFromRods(toolkit, (Collection)names);
   }

   public static Map getVCFHeadersFromRods(GenomeAnalysisEngine toolkit) {
      return getVCFHeadersFromRods(toolkit, (Collection)null);
   }

   public static Map getVCFHeadersFromRods(GenomeAnalysisEngine toolkit, Collection rodNames) {
      Map data = new HashMap();
      List dataSources = toolkit.getRodDataSources();
      Iterator j = dataSources.iterator();

      while(true) {
         ReferenceOrderedDataSource source;
         do {
            if (!j.hasNext()) {
               return data;
            }

            source = (ReferenceOrderedDataSource)j.next();
         } while(rodNames != null && !rodNames.contains(source.getName()));

         if (source.getHeader() != null && source.getHeader() instanceof VCFHeader) {
            data.put(source.getName(), (VCFHeader)source.getHeader());
         }
      }
   }

   public static Map getVCFHeadersFromRodPrefix(GenomeAnalysisEngine toolkit, String prefix) {
      Map data = new HashMap();
      List dataSources = toolkit.getRodDataSources();
      Iterator j = dataSources.iterator();

      while(j.hasNext()) {
         ReferenceOrderedDataSource source = (ReferenceOrderedDataSource)j.next();
         if (source.getName().startsWith(prefix) && source.getHeader() != null && source.getHeader() instanceof VCFHeader) {
            data.put(source.getName(), (VCFHeader)source.getHeader());
         }
      }

      return data;
   }

   public static Set getHeaderFields(GenomeAnalysisEngine toolkit) {
      return getHeaderFields(toolkit, (Collection)null);
   }

   public static Set getHeaderFields(GenomeAnalysisEngine toolkit, Collection rodNames) {
      TreeSet fields = new TreeSet();
      List dataSources = toolkit.getRodDataSources();
      Iterator j = dataSources.iterator();

      while(true) {
         ReferenceOrderedDataSource source;
         do {
            if (!j.hasNext()) {
               return fields;
            }

            source = (ReferenceOrderedDataSource)j.next();
         } while(rodNames != null && !rodNames.contains(source.getName()));

         if (source.getRecordType().equals(VariantContext.class)) {
            VCFHeader header = (VCFHeader)source.getHeader();
            if (header != null) {
               fields.addAll(header.getMetaData());
            }
         }
      }
   }

   public static Set smartMergeHeaders(Collection headers, Logger logger) throws IllegalStateException {
      HashMap map = new HashMap();
      VCFUtils.HeaderConflictWarner conflictWarner = new VCFUtils.HeaderConflictWarner(logger);
      Iterator j = headers.iterator();

      label79:
      while(j.hasNext()) {
         VCFHeader source = (VCFHeader)j.next();
         Iterator j = source.getMetaData().iterator();

         VCFHeaderLine line;
         VCFHeaderLine other;
         String lineName;
         String otherName;
         do {
            while(true) {
               String key;
               do {
                  while(true) {
                     if (!j.hasNext()) {
                        continue label79;
                     }

                     line = (VCFHeaderLine)j.next();
                     key = line.getKey();
                     if (line instanceof VCFIDHeaderLine) {
                        key = key + "-" + ((VCFIDHeaderLine)line).getID();
                     }

                     if (map.containsKey(key)) {
                        other = (VCFHeaderLine)map.get(key);
                        break;
                     }

                     map.put(key, line);
                  }
               } while(line.equals(other));

               if (!line.getClass().equals(other.getClass())) {
                  throw new IllegalStateException("Incompatible header types: " + line + " " + other);
               }

               if (line instanceof VCFFilterHeaderLine) {
                  lineName = ((VCFFilterHeaderLine)line).getID();
                  otherName = ((VCFFilterHeaderLine)other).getID();
                  break;
               }

               if (!(line instanceof VCFCompoundHeaderLine)) {
                  conflictWarner.warn(line, "Ignoring header line already in map: this header line = " + line + " already present header = " + other);
               } else {
                  VCFCompoundHeaderLine compLine = (VCFCompoundHeaderLine)line;
                  VCFCompoundHeaderLine compOther = (VCFCompoundHeaderLine)other;
                  if (!compLine.equalsExcludingDescription(compOther)) {
                     if (compLine.getType().equals(compOther.getType())) {
                        conflictWarner.warn(line, "Promoting header field Number to . due to number differences in header lines: " + line + " " + other);
                        compOther.setNumberToUnbounded();
                     } else if (compLine.getType() == VCFHeaderLineType.Integer && compOther.getType() == VCFHeaderLineType.Float) {
                        conflictWarner.warn(line, "Promoting Integer to Float in header: " + compOther);
                        map.put(key, compOther);
                     } else {
                        if (compLine.getType() != VCFHeaderLineType.Float || compOther.getType() != VCFHeaderLineType.Integer) {
                           throw new IllegalStateException("Incompatible header types, collision between these two types: " + line + " " + other);
                        }

                        conflictWarner.warn(line, "Promoting Integer to Float in header: " + compOther);
                     }
                  }

                  if (!compLine.getDescription().equals(compOther)) {
                     conflictWarner.warn(line, "Allowing unequal description fields through: keeping " + compOther + " excluding " + compLine);
                  }
               }
            }
         } while(lineName.equals(otherName));

         throw new IllegalStateException("Incompatible header types: " + line + " " + other);
      }

      return new HashSet(map.values());
   }

   public static String rsIDOfFirstRealVariant(List VCs, VariantContext.Type type) {
      if (VCs == null) {
         return null;
      } else {
         String rsID = null;
         Iterator j = VCs.iterator();

         while(j.hasNext()) {
            VariantContext vc = (VariantContext)j.next();
            if (vc.getType() == type) {
               rsID = vc.getID();
               break;
            }
         }

         return rsID;
      }
   }

   private static final class HeaderConflictWarner {
      Logger logger;
      Set alreadyIssued;

      private HeaderConflictWarner(Logger logger) {
         this.alreadyIssued = new HashSet();
         this.logger = logger;
      }

      public void warn(VCFHeaderLine line, String msg) {
         if (this.logger != null && !this.alreadyIssued.contains(line.getKey())) {
            this.alreadyIssued.add(line.getKey());
            this.logger.warn(msg);
         }

      }

      // $FF: synthetic method
      HeaderConflictWarner(Logger x0, Object x1) {
         this(x0);
      }
   }
}
*/
