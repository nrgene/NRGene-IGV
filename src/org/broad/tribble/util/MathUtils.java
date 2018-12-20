package org.broad.tribble.util;

/**
 * a collection of functions and classes for various common calculations
 */
public class MathUtils {

    /**
     * a class for calculating moving statistics - this class returns the
     * mean, variance, and std dev after accumulating any number of records.
     * taken from http://www.johndcook.com/standard_deviation.html
     */
    public static class RunningStat {
        private double oldMean, newMean, oldStdDev, newStdDev;
        private long recordCount = 0;

        /**
         * @param x the value to add to the running mean and variance
         */
        public void push(double x) {
            recordCount++;
            // See Knuth TAOCP vol 2, 3rd edition, page 232
            if (recordCount == 1) {
                oldMean = newMean = x;
                oldStdDev = 0.0;
            } else {
                newMean = oldMean + (x - oldMean) / recordCount;
                newStdDev = oldStdDev + (x - oldMean) * (x - newMean);

                // set up for next iteration
                oldMean = newMean;
                oldStdDev = newStdDev;
            }
        }

        public void clear() { recordCount = 0; }
        public final long numDataValues() { return recordCount; }
        public final double mean() { return (recordCount > 0) ? newMean : 0.0; }
        public double variance() { return ((recordCount > 1) ? newStdDev / (recordCount - 1) : 0.0); }
        public double standardDeviation() { return Math.sqrt(variance()); }
    }

}
