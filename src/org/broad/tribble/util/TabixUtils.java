package org.broad.tribble.util;

import java.util.HashMap;

/**
 * classes that have anything to do with tabix
 */
public class TabixUtils {

    public static class TPair64 implements Comparable<TPair64> {
        public long u, v;

        public TPair64(final long _u, final long _v) {
            u = _u;
            v = _v;
        }

        public TPair64(final TPair64 p) {
            u = p.u;
            v = p.v;
        }

        public int compareTo(final TPair64 p) {
            return u == p.u ? 0 : ((u < p.u) ^ (u < 0) ^ (p.u < 0)) ? -1 : 1; // unsigned 64-bit comparison
        }
    }

    public static class TIndex {
        public HashMap<Integer, TPair64[]> b; // binning index
        public long[] l; // linear index
    }


    public static class TIntv {
        public int tid, beg, end;
    }


    public static boolean less64(final long u, final long v) { // unsigned 64-bit comparison
        return (u < v) ^ (u < 0) ^ (v < 0);
    }
}
