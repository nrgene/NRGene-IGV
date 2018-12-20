package org.broad.igv.feature.tribble;

import java.io.IOException;
import java.util.Iterator;

import org.broad.tribble.FeatureSource;
import org.broad.tribble.bed.BEDFeature;
import org.broad.tribble.source.BasicFeatureSource;
import org.broadinstitute.sting.utils.codecs.vcf.VCFCodec;
import org.broadinstitute.sting.utils.variantcontext.VariantContext;

/**
 * @author Jim Robinson
 * @date 12/7/11
 */
public class VCFExample {



    public void testBED() throws IOException {

        String bedFile = "path to your indexed bed";
        FeatureSource<BEDFeature> source = BasicFeatureSource.getFeatureSource(bedFile, new BEDCodec(), true);

        Iterator<BEDFeature> iter = source.query("chr1", 100000, 200000);

        while (iter.hasNext()) {
            BEDFeature f = iter.next();
            //assertTrue(f.getEnd() >= 0 && f.getStart() <= 200);
        }

        source.close();
    }


    public void testVCF() throws IOException {

        String vcfFile = "path to your indexed VCF";
        boolean requiresIndex = true;
        FeatureSource<VariantContext> source = BasicFeatureSource.getFeatureSource(vcfFile, new VCFCodec(), true);

        Iterator<VariantContext> iter = source.query("chr1", 100000, 200000);

        while (iter.hasNext()) {
            VariantContext variant = iter.next();
            variant.getStart();
            variant.getAlleles();
            // etc etc
        }
        source.close();
    }

}
