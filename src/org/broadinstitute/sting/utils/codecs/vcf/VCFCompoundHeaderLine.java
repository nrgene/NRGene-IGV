package org.broadinstitute.sting.utils.codecs.vcf;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.broad.tribble.TribbleException;
import org.broadinstitute.sting.utils.exceptions.ReviewedStingException;

public abstract class VCFCompoundHeaderLine extends VCFHeaderLine implements VCFIDHeaderLine {
   private String name;
   private int count = -1;
   private VCFHeaderLineCount countType;
   private String description;
   private VCFHeaderLineType type;
   private final VCFCompoundHeaderLine.SupportedHeaderLineType lineType;

   public String getID() {
      return this.name;
   }

   public String getDescription() {
      return this.description;
   }

   public VCFHeaderLineType getType() {
      return this.type;
   }

   public VCFHeaderLineCount getCountType() {
      return this.countType;
   }

   public int getCount() {
      if (this.countType != VCFHeaderLineCount.INTEGER) {
         throw new ReviewedStingException("Asking for header line count when type is not an integer");
      } else {
         return this.count;
      }
   }

   public int getCount(int numAltAlleles) {
      int myCount;
      switch(this.countType) {
      case INTEGER:
         myCount = this.count;
         break;
      case UNBOUNDED:
         myCount = -1;
         break;
      case A:
         myCount = numAltAlleles-1;
         break;
      case R:
         myCount = numAltAlleles;
         break;
      case G:
         myCount = (numAltAlleles + 1) * (numAltAlleles + 2) / 2;
         break;
      default:
         throw new ReviewedStingException("Unknown count type: " + this.countType);
      }

      return myCount;
   }

   public void setNumberToUnbounded() {
      this.countType = VCFHeaderLineCount.UNBOUNDED;
      this.count = -1;
   }

   protected VCFCompoundHeaderLine(String name, int count, VCFHeaderLineType type, String description, VCFCompoundHeaderLine.SupportedHeaderLineType lineType) {
      super(lineType.toString(), "");
      this.name = name;
      this.countType = VCFHeaderLineCount.INTEGER;
      this.count = count;
      this.type = type;
      this.description = description;
      this.lineType = lineType;
      this.validate();
   }

   protected VCFCompoundHeaderLine(String name, VCFHeaderLineCount count, VCFHeaderLineType type, String description, VCFCompoundHeaderLine.SupportedHeaderLineType lineType) {
      super(lineType.toString(), "");
      this.name = name;
      this.countType = count;
      this.type = type;
      this.description = description;
      this.lineType = lineType;
      this.validate();
   }

   protected VCFCompoundHeaderLine(String line, VCFHeaderVersion version, VCFCompoundHeaderLine.SupportedHeaderLineType lineType) {
      super(lineType.toString(), "");
      Map mapping = VCFHeaderLineTranslator.parseLine(version, line, Arrays.asList("ID", "Number", "Type", "Description"));
      this.name = (String)mapping.get("ID");
      this.count = -1;
      String numberStr = (String)mapping.get("Number");
      if (numberStr.equals(VCFConstants.PER_ALTERNATE_COUNT)) {
         this.countType = VCFHeaderLineCount.A;
      } else if ( numberStr.equals(VCFConstants.PER_ALLELE_COUNT) ) {
         this.countType = VCFHeaderLineCount.R;
      } else if (numberStr.equals(VCFConstants.PER_GENOTYPE_COUNT)) {
         this.countType = VCFHeaderLineCount.G;
      } else if ((version != VCFHeaderVersion.VCF4_0 && version != VCFHeaderVersion.VCF4_1 || !numberStr.equals(".")) && (version != VCFHeaderVersion.VCF3_2 && version != VCFHeaderVersion.VCF3_3 || !numberStr.equals("-1"))) {
         this.countType = VCFHeaderLineCount.INTEGER;
         this.count = Integer.valueOf(numberStr);
      } else {
         this.countType = VCFHeaderLineCount.UNBOUNDED;
      }

      try {
         this.type = VCFHeaderLineType.valueOf((String)mapping.get("Type"));
      } catch (Exception var7) {
         throw new TribbleException((String)mapping.get("Type") + " is not a valid type in the VCF specification (note that types are case-sensitive)");
      }

      if (this.type == VCFHeaderLineType.Flag && !this.allowFlagValues()) {
         throw new IllegalArgumentException("Flag is an unsupported type for this kind of field");
      } else {
         this.description = (String)mapping.get("Description");
         if (this.description == null && ALLOW_UNBOUND_DESCRIPTIONS) {
            this.description = UNBOUND_DESCRIPTION;
         }

         this.lineType = lineType;
         this.validate();
      }
   }

   private void validate() {
      if (this.name == null || this.type == null || this.description == null || this.lineType == null) {
         throw new IllegalArgumentException(String.format("Invalid VCFCompoundHeaderLine: key=%s name=%s type=%s desc=%s lineType=%s", super.getKey(), this.name, this.type, this.description, this.lineType));
      }
   }

   protected String toStringEncoding() {
      Map map = new LinkedHashMap();
      map.put("ID", this.name);
      Object number;
      switch(this.countType) {
      case A:
         number = VCFConstants.PER_ALTERNATE_COUNT;
         break;
      case R:
         number = VCFConstants.PER_ALLELE_COUNT;
         break;
      case G:
         number = VCFConstants.PER_GENOTYPE_COUNT;
         break;
      case UNBOUNDED:
         number = VCFConstants.UNBOUNDED_ENCODING_v4;
         break;
      case INTEGER:
      default:
         number = this.count;
      }

      map.put("Number", number);
      map.put("Type", this.type);
      map.put("Description", this.description);
      return this.lineType.toString() + "=" + VCFHeaderLine.toStringEncoding(map);
   }

   public boolean equals(Object o) {
      if (!(o instanceof VCFCompoundHeaderLine)) {
         return false;
      } else {
         VCFCompoundHeaderLine other = (VCFCompoundHeaderLine)o;
         return this.equalsExcludingDescription(other) && this.description.equals(other.description);
      }
   }

   public boolean equalsExcludingDescription(VCFCompoundHeaderLine other) {
      return this.count == other.count && this.countType == other.countType && this.type == other.type && this.lineType == other.lineType && this.name.equals(other.name);
   }

   public boolean sameLineTypeAndName(VCFCompoundHeaderLine other) {
      return this.lineType == other.lineType && this.name.equals(other.name);
   }

   abstract boolean allowFlagValues();

   public static enum SupportedHeaderLineType {
      INFO(true),
      FORMAT(false);

      public final boolean allowFlagValues;

      private SupportedHeaderLineType(boolean flagValues) {
         this.allowFlagValues = flagValues;
      }
   }
}
