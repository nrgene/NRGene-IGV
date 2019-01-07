package org.broadinstitute.sting.utils;

import com.google.java.contract.Ensures;

public interface HasGenomeLocation {
   @Ensures({"result != null"})
   GenomeLoc getLocation();
}
