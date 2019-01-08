package org.broadinstitute.sting.utils.codecs.vcf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.broad.tribble.util.ParsingUtils;

public class VCFHeader {
   private final Set<VCFHeaderLine> mMetaData;
   private final Map<String, VCFInfoHeaderLine> mInfoMetaData = new HashMap<>();
   private final Map<String, VCFFormatHeaderLine> mFormatMetaData = new HashMap<>();
   private final Map<String, VCFHeaderLine> mOtherMetaData = new HashMap<>();
   private final Set<String> mGenotypeSampleNames = new LinkedHashSet<>();
   public static final String METADATA_INDICATOR = "##";
   public static final String HEADER_INDICATOR = "#";
   public static final String SOURCE_KEY = "source";
   public static final String REFERENCE_KEY = "reference";
   public static final String CONTIG_KEY = "contig";
   public static final String INTERVALS_KEY = "intervals";
   private boolean samplesWereAlreadySorted = true;
   protected ArrayList<String> sampleNamesInOrder = null;
   protected HashMap<String, Integer> sampleNameToOffset = null;
   private boolean writeEngineHeaders = true;
   private boolean writeCommandLine = true;

   public VCFHeader(Set<VCFHeaderLine>  metaData) {
      this.mMetaData = new TreeSet<>(metaData);
      this.loadVCFVersion();
      this.loadMetaDataMaps();
   }

   public VCFHeader(Set<VCFHeaderLine>  metaData, Set<String> genotypeSampleNames) {
      this.mMetaData = new TreeSet<>();
      if (metaData != null) {
         this.mMetaData.addAll(metaData);
      }

      this.mGenotypeSampleNames.addAll(genotypeSampleNames);
      this.loadVCFVersion();
      this.loadMetaDataMaps();
      this.samplesWereAlreadySorted = ParsingUtils.isSorted(genotypeSampleNames);
   }

   protected void buildVCFReaderMaps(List genotypeSampleNamesInAppearenceOrder) {
      this.sampleNamesInOrder = new ArrayList<>(genotypeSampleNamesInAppearenceOrder.size());
      this.sampleNameToOffset = new HashMap<>(genotypeSampleNamesInAppearenceOrder.size());
      int i = 0;

      for (Object aGenotypeSampleNamesInAppearenceOrder : genotypeSampleNamesInAppearenceOrder) {
         String name = (String) aGenotypeSampleNamesInAppearenceOrder;
         this.sampleNamesInOrder.add(name);
         this.sampleNameToOffset.put(name, i++);
      }

      Collections.sort(this.sampleNamesInOrder);
   }

   public void addMetaDataLine(VCFHeaderLine headerLine) {
      this.mMetaData.add(headerLine);
   }

   public void loadVCFVersion() {
      List<VCFHeaderLine> toRemove = new ArrayList<>();
      Iterator j = this.mMetaData.iterator();

      while(j.hasNext()) {
         VCFHeaderLine line = (VCFHeaderLine)j.next();
         if (VCFHeaderVersion.isFormatString(line.getKey())) {
            toRemove.add(line);
         }
      }

      this.mMetaData.removeAll(toRemove);
   }

   private void loadMetaDataMaps() {
      Iterator j = this.mMetaData.iterator();

      while(j.hasNext()) {
         VCFHeaderLine line = (VCFHeaderLine)j.next();
         if (line instanceof VCFInfoHeaderLine) {
            VCFInfoHeaderLine infoLine = (VCFInfoHeaderLine)line;
            this.mInfoMetaData.put(infoLine.getID(), infoLine);
         } else if (line instanceof VCFFormatHeaderLine) {
            VCFFormatHeaderLine formatLine = (VCFFormatHeaderLine)line;
            this.mFormatMetaData.put(formatLine.getID(), formatLine);
         } else {
            this.mOtherMetaData.put(line.getKey(), line);
         }
      }

   }

   public Set<VCFHeader.HEADER_FIELDS> getHeaderFields() {
      return new LinkedHashSet(Arrays.asList(VCFHeader.HEADER_FIELDS.values()));
   }

   public Set<VCFHeaderLine> getMetaData() {
      Set<VCFHeaderLine> lines = new LinkedHashSet<>();
      lines.add(new VCFHeaderLine(VCFHeaderVersion.VCF4_0.getFormatString(), VCFHeaderVersion.VCF4_0.getVersionString()));
      lines.addAll(this.mMetaData);
      return Collections.unmodifiableSet(lines);
   }

   public Set getGenotypeSamples() {
      return this.mGenotypeSampleNames;
   }

   public boolean hasGenotypingData() {
      return this.mGenotypeSampleNames.size() > 0;
   }

   public boolean samplesWereAlreadySorted() {
      return this.samplesWereAlreadySorted;
   }

   public int getColumnCount() {
      return VCFHeader.HEADER_FIELDS.values().length + (this.hasGenotypingData() ? this.mGenotypeSampleNames.size() + 1 : 0);
   }

   public VCFInfoHeaderLine getInfoHeaderLine(String key) {
      return this.mInfoMetaData.get(key);
   }

   public VCFFormatHeaderLine getFormatHeaderLine(String key) {
      return this.mFormatMetaData.get(key);
   }

   public VCFHeaderLine getOtherHeaderLine(String key) {
      return (VCFHeaderLine)this.mOtherMetaData.get(key);
   }

   public boolean isWriteEngineHeaders() {
      return this.writeEngineHeaders;
   }

   public void setWriteEngineHeaders(boolean writeEngineHeaders) {
      this.writeEngineHeaders = writeEngineHeaders;
   }

   public boolean isWriteCommandLine() {
      return this.writeCommandLine;
   }

   public void setWriteCommandLine(boolean writeCommandLine) {
      this.writeCommandLine = writeCommandLine;
   }

   public static enum HEADER_FIELDS {
      CHROM,
      POS,
      ID,
      REF,
      ALT,
      QUAL,
      FILTER,
      INFO
   }
}
