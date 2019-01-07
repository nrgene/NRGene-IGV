/*
package org.broadinstitute.sting.utils.codecs.vcf;

import com.google.java.contract.Ensures;
import com.google.java.contract.Requires;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import net.sf.samtools.SAMSequenceDictionary;
import org.broad.tribble.Tribble;
import org.broad.tribble.index.DynamicIndexCreator;
import org.broad.tribble.index.Index;
import org.broad.tribble.index.IndexFactory.IndexBalanceApproach;
import org.broad.tribble.util.LittleEndianOutputStream;
import org.broad.tribble.util.PositionalStream;
import org.broadinstitute.sting.gatk.refdata.tracks.IndexDictionaryUtils;
import org.broadinstitute.sting.utils.exceptions.ReviewedStingException;
import org.broadinstitute.sting.utils.exceptions.UserException;
import org.broadinstitute.sting.utils.variantcontext.VariantContext;

public abstract class IndexingVCFWriter implements VCFWriter {
   private final String name;
   private final SAMSequenceDictionary refDict;
   private OutputStream outputStream;
   private PositionalStream positionalStream = null;
   private DynamicIndexCreator indexer = null;
   private LittleEndianOutputStream idxStream = null;

   @Requires({"name != null", "! ( location == null && output == null )", "! ( enableOnTheFlyIndexing && location == null )"})
   protected IndexingVCFWriter(String name, File location, OutputStream output, SAMSequenceDictionary refDict, boolean enableOnTheFlyIndexing) {
      this.outputStream = output;
      this.name = name;
      this.refDict = refDict;
      if (enableOnTheFlyIndexing) {
         try {
            this.idxStream = new LittleEndianOutputStream(new FileOutputStream(Tribble.indexFile(location)));
            this.indexer = new DynamicIndexCreator(IndexBalanceApproach.FOR_SEEK_TIME);
            this.indexer.initialize(location, this.indexer.defaultBinSize());
            this.positionalStream = new PositionalStream(output);
            this.outputStream = this.positionalStream;
         } catch (IOException var7) {
            this.idxStream = null;
            this.indexer = null;
            this.positionalStream = null;
         }
      }

   }

   @Ensures({"result != null"})
   public OutputStream getOutputStream() {
      return this.outputStream;
   }

   @Ensures({"result != null"})
   public String getStreamName() {
      return this.name;
   }

   public abstract void writeHeader(VCFHeader var1);

   public void close() {
      if (this.indexer != null) {
         try {
            Index index = this.indexer.finalizeIndex(this.positionalStream.getPosition());
            IndexDictionaryUtils.setIndexSequenceDictionary(index, this.refDict);
            index.write(this.idxStream);
            this.idxStream.close();
         } catch (IOException var2) {
            throw new ReviewedStingException("Unable to close index for " + this.getStreamName(), var2);
         }
      }

   }

   public void add(VariantContext vc) {
      if (this.indexer != null) {
         this.indexer.addFeature(vc, this.positionalStream.getPosition());
      }

   }

   protected static final String writerName(File location, OutputStream stream) {
      return location == null ? stream.toString() : location.getAbsolutePath();
   }

   protected static OutputStream openOutputStream(File location) {
      try {
         return new FileOutputStream(location);
      } catch (FileNotFoundException var2) {
         throw new UserException.CouldNotCreateOutputFile(location, "Unable to create VCF writer", var2);
      }
   }
}
*/
