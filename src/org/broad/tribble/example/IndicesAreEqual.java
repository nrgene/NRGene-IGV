package org.broad.tribble.example;

import java.io.File;

import org.broad.tribble.index.Index;
import org.broad.tribble.index.IndexFactory;

/**
 * a quick example of how to index a feature file, and then count all the records in the file.  This is also useful
 * for testing the feature reader
 */
public class IndicesAreEqual {
    // setup the logging system, used by some codecs
    private static org.apache.log4j.Logger logger = org.apache.log4j.Logger.getRootLogger();

    /**
     * this class:
     *  1) checks to see that the feature file exists
     *  2) loads an index from disk, if one doesn't exist, it creates it and writes it to disk
     *  3) creates a FeatureSource
     *  4) iterates over the records, emitting a final tally for the number of features seen
     *  
     * @param args a single parameter, the file name to load
     */
    public static void main(String[] args) {
        if ( args.length != 2 )
            printUsage();
        else {
            Index index1 = loadIndex(args[0]);
            Index index2 = loadIndex(args[1]);
            System.out.printf("%n");
            System.out.printf("index1: %s%n", args[0]);
            System.out.printf("index2: %s%n", args[1]);
            boolean eq = index1.equals(index2);
            System.out.printf("  equals() = %b%n", eq);
        }
    }

    /**
     * print usage information
     */
    public static void printUsage() {
        System.err.println("Usage: java -jar IndicesAreEqual.jar index1 index2");
        System.err.println("    Prints out true / false if index1 and index2 are equal");
        System.exit(1);
    }

    /**
     * @return an index instance
     */
    public static Index loadIndex(String filename) {
        //System.err.println("Loading index from disk for index file -> " + filename);
        File file = new File(filename);
        if (file.canRead()) {
            return IndexFactory.loadIndex(file.getAbsolutePath());
        } else {
            printUsage();
            return null;
        }
    }
}
