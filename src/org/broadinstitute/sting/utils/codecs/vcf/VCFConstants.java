package org.broadinstitute.sting.utils.codecs.vcf;

import java.util.Locale;

public final class VCFConstants {
   public static final Locale VCF_LOCALE;
   public static final String ANCESTRAL_ALLELE_KEY = "AA";
   public static final String ALLELE_COUNT_KEY = "AC";
   public static final String ALLELE_FREQUENCY_KEY = "AF";
   public static final String ALLELE_NUMBER_KEY = "AN";
   public static final String RMS_BASE_QUALITY_KEY = "BQ";
   public static final String CIGAR_KEY = "CIGAR";
   public static final String DBSNP_KEY = "DB";
   public static final String DEPTH_KEY = "DP";
   public static final String DOWNSAMPLED_KEY = "DS";
   public static final String EXPECTED_ALLELE_COUNT_KEY = "EC";
   public static final String END_KEY = "END";
   public static final String GENOTYPE_FILTER_KEY = "FT";
   public static final String GENOTYPE_KEY = "GT";
   /** @deprecated */
   @Deprecated
   public static final String GENOTYPE_LIKELIHOODS_KEY = "GL";
   public static final String GENOTYPE_POSTERIORS_KEY = "GP";
   public static final String GENOTYPE_QUALITY_KEY = "GQ";
   public static final String HAPMAP2_KEY = "H2";
   public static final String HAPMAP3_KEY = "H3";
   public static final String HAPLOTYPE_QUALITY_KEY = "HQ";
   public static final String RMS_MAPPING_QUALITY_KEY = "MQ";
   public static final String MAPPING_QUALITY_ZERO_KEY = "MQ0";
   public static final String SAMPLE_NUMBER_KEY = "NS";
   public static final String PHRED_GENOTYPE_LIKELIHOODS_KEY = "PL";
   public static final String PHASE_QUALITY_KEY = "PQ";
   public static final String PHASE_SET_KEY = "PS";
   public static final String OLD_DEPTH_KEY = "RD";
   public static final String STRAND_BIAS_KEY = "SB";
   public static final String SOMATIC_KEY = "SOMATIC";
   public static final String VALIDATED_KEY = "VALIDATED";
   public static final String THOUSAND_GENOMES_KEY = "1000G";
   public static final String FORMAT_FIELD_SEPARATOR = ":";
   public static final String GENOTYPE_FIELD_SEPARATOR = ":";
   public static final char GENOTYPE_FIELD_SEPARATOR_CHAR = ':';
   public static final String FIELD_SEPARATOR = "\t";
   public static final char FIELD_SEPARATOR_CHAR = '\t';
   public static final String FILTER_CODE_SEPARATOR = ";";
   public static final String INFO_FIELD_ARRAY_SEPARATOR = ",";
   public static final char INFO_FIELD_ARRAY_SEPARATOR_CHAR = ',';
   public static final String ID_FIELD_SEPARATOR = ";";
   public static final String INFO_FIELD_SEPARATOR = ";";
   public static final char INFO_FIELD_SEPARATOR_CHAR = ';';
   public static final String UNPHASED = "/";
   public static final String PHASED = "|";
   public static final String PHASED_SWITCH_PROB_v3 = "\\";
   public static final String PHASING_TOKENS = "/|\\";
   public static final String FILTER_HEADER_START = "##FILTER";
   public static final String FORMAT_HEADER_START = "##FORMAT";
   public static final String INFO_HEADER_START = "##INFO";
   public static final String ALT_HEADER_START = "##ALT";
   public static final String CONTIG_HEADER_START = "##contig";
   public static final char DELETION_ALLELE_v3 = 'D';
   public static final char INSERTION_ALLELE_v3 = 'I';
   public static final String UNFILTERED = ".";
   public static final String PASSES_FILTERS_v3 = "0";
   public static final String PASSES_FILTERS_v4 = "PASS";
   public static final String EMPTY_ID_FIELD = ".";
   public static final String EMPTY_INFO_FIELD = ".";
   public static final String EMPTY_ALTERNATE_ALLELE_FIELD = ".";
   public static final String MISSING_VALUE_v4 = ".";
   public static final String MISSING_QUALITY_v3 = "-1";
   public static final Double MISSING_QUALITY_v3_DOUBLE;
   public static final String MISSING_GENOTYPE_QUALITY_v3 = "-1";
   public static final String MISSING_HAPLOTYPE_QUALITY_v3 = "-1";
   public static final String MISSING_DEPTH_v3 = "-1";
   public static final String UNBOUNDED_ENCODING_v4 = ".";
   public static final String UNBOUNDED_ENCODING_v3 = "-1";
   public static final String PER_ALTERNATE_COUNT = "A";
   public static final String PER_ALLELE_COUNT = "R";
   public static final String PER_GENOTYPE_COUNT = "G";
   public static final String EMPTY_ALLELE = ".";
   public static final String EMPTY_GENOTYPE = "./.";
   public static final double MAX_GENOTYPE_QUAL = 99.0D;
   public static final String DOUBLE_PRECISION_FORMAT_STRING = "%.2f";
   public static final String DOUBLE_PRECISION_INT_SUFFIX = ".00";
   public static final Double VCF_ENCODING_EPSILON;

   // special alleles
   public static final char SPANNING_DELETION_ALLELE = '*';
   public static final char NO_CALL_ALLELE = '.';
   public static final char NULL_ALLELE = '-';

   static {
      VCF_LOCALE = Locale.US;
      MISSING_QUALITY_v3_DOUBLE = Double.valueOf("-1");
      VCF_ENCODING_EPSILON = 5.0E-5D;
   }
}
