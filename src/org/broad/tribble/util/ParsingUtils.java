package org.broad.tribble.util;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.log4j.Logger;
import org.broad.tribble.readers.AsciiLineReader;

/**
 * @author jrobinso
 */
public class ParsingUtils {

    private static Logger log = Logger.getLogger(ParsingUtils.class);

    public static Map<Object, Color> colorCache = new WeakHashMap<Object, Color>(100);

    // HTML 4.1 color table,  + orange and magenta
    static Map<String, String> colorSymbols = new HashMap();

    static {
        colorSymbols.put("white", "FFFFFF");
        colorSymbols.put("silver", "C0C0C0");
        colorSymbols.put("gray", "808080");
        colorSymbols.put("black", "000000");
        colorSymbols.put("red", "FF0000");
        colorSymbols.put("maroon", "800000");
        colorSymbols.put("yellow", "FFFF00");
        colorSymbols.put("olive", "808000");
        colorSymbols.put("lime", "00FF00");
        colorSymbols.put("green", "008000");
        colorSymbols.put("aqua", "00FFFF");
        colorSymbols.put("teal", "008080");
        colorSymbols.put("blue", "0000FF");
        colorSymbols.put("navy", "000080");
        colorSymbols.put("fuchsia", "FF00FF");
        colorSymbols.put("purple", "800080");
        colorSymbols.put("orange", "FFA500");
        colorSymbols.put("magenta", "FF00FF");
    }


    public static BufferedReader openBufferedReader(String path)
            throws IOException {
        InputStream stream = openInputStream(path);
        return new BufferedReader(new InputStreamReader(stream));
    }


    public static AsciiLineReader openAsciiReader(String path)
            throws IOException {
        InputStream stream = openInputStream(path);
        return new AsciiLineReader(stream);

    }


    public static InputStream openInputStream(String path)
            throws IOException {

        InputStream inputStream;
        if (path.startsWith("ftp:")) {
            // TODO -- throw an appropriate exception
            throw new RuntimeException("FTP streams not supported.");
        }
        if (path.startsWith("http:") || path.startsWith("https:")) {
            inputStream = SeekableStreamFactory.getStreamFor(path);
        } else {
            File file = new File(path);
            inputStream = new FileInputStream(file);
        }

        if (path.endsWith("gz")) {
            return new BlockCompressedInputStream(inputStream);
        } else {
            return inputStream;
        }

    }

    //public static String join(String separator, Collection<String> strings) {
    //    return join( separator, strings.toArray(new String[0]) );
    //}

    public static <T> String join(String separator, Collection<T> objects) {
        if (objects.isEmpty()) {
            return "";
        }
        Iterator<T> iter = objects.iterator();
        final StringBuilder ret = new StringBuilder(iter.next().toString());
        while (iter.hasNext()) {
            ret.append(separator);
            ret.append(iter.next().toString());
        }

        return ret.toString();
    }

    /**
     * a small utility function for sorting a list
     *
     * @param list
     * @param <T>
     * @return
     */
    public static <T extends Comparable> List<T> sortList(Collection<T> list) {
        ArrayList<T> ret = new ArrayList<T>();
        ret.addAll(list);
        Collections.sort(ret);
        return ret;
    }

    public static <T extends Comparable<T>, V> String sortedString(Map<T, V> c) {
        List<T> t = new ArrayList<T>(c.keySet());
        Collections.sort(t);

        List<String> pairs = new ArrayList<String>();
        for (T k : t) {
            pairs.add(k + "=" + c.get(k));
        }

        return "{" + ParsingUtils.join(", ", pairs.toArray(new String[pairs.size()])) + "}";
    }

    /**
     * join an array of strings given a seperator
     *
     * @param separator the string to insert between each array element
     * @param strings   the array of strings
     * @return a string, which is the joining of all array values with the separator
     */
    public static String join(String separator, String[] strings) {
        return join(separator, strings, 0, strings.length);
    }

    /**
     * join a set of strings, using the separator provided, from index start to index stop
     *
     * @param separator the separator to use
     * @param strings   the list of strings
     * @param start     the start position (index in the list)0
     * @param end       the end position (index in the list)
     * @return a joined string, or "" if end - start == 0
     */
    public static String join(String separator, String[] strings, int start, int end) {
        if ((end - start) == 0) {
            return "";
        }
        StringBuilder ret = new StringBuilder(strings[start]);
        for (int i = start + 1; i < end; ++i) {
            ret.append(separator);
            ret.append(strings[i]);
        }
        return ret.toString();
    }


    /**
     * Split the string into tokesn separated by the given delimiter.  Profiling has
     * revealed that the standard string.split() method typically takes > 1/2
     * the total time when used for parsing ascii files.
     *
     * @param aString the string to split
     * @param tokens  an array to hold the parsed tokens
     * @param delim   character that delimits tokens
     * @return the number of tokens parsed
     */
    public static int split(String aString, String[] tokens, char delim) {
        return split(aString, tokens, delim, false);
    }

    /**
     * Split the string into tokesn separated by the given delimiter.  Profiling has
     * revealed that the standard string.split() method typically takes > 1/2
     * the total time when used for parsing ascii files.
     *
     * @param aString                the string to split
     * @param tokens                 an array to hold the parsed tokens
     * @param delim                  character that delimits tokens
     * @param condenseTrailingTokens if true and there are more tokens than will fit in the tokens array, condense all trailing tokens into the last token
     * @return the number of tokens parsed
     */
    public static int split(String aString, String[] tokens, char delim, boolean condenseTrailingTokens) {

        int maxTokens = tokens.length;
        int nTokens = 0;
        int start = 0;
        int end = aString.indexOf(delim);
        if (end < 0) {
            tokens[nTokens++] = aString;
            return nTokens;
        }

        while ((end > 0) && (nTokens < maxTokens)) {
            tokens[nTokens++] = aString.substring(start, end);
            start = end + 1;
            end = aString.indexOf(delim, start);
        }

        // condense if appropriate
        if (condenseTrailingTokens && nTokens == maxTokens) {
            tokens[nTokens - 1] = tokens[nTokens - 1] + delim + aString.substring(start);
        }
        // Add the trailing string
        else if (nTokens < maxTokens) {
            String trailingString = aString.substring(start);
            tokens[nTokens++] = trailingString;
        }

        return nTokens;
    }


    // trim a string for the given character (i.e. not just whitespace)

    public static String trim(String str, char ch) {
        char[] array = str.toCharArray();
        int start = 0;
        while (start < array.length && array[start] == ch)
            start++;

        int end = array.length - 1;
        while (end > start && array[end] == ch)
            end--;

        return str.substring(start, end + 1);
    }


    /**
     * Split the string into tokesn separated by tab or space.  This method
     * was added so support wig and bed files, which apparently accept
     * either.
     *
     * @param aString the string to split
     * @param tokens  an array to hold the parsed tokens
     * @return the number of tokens parsed
     */
    public static int splitWhitespace(String aString, String[] tokens) {

        int maxTokens = tokens.length;
        int nTokens = 0;
        int start = 0;
        int tabEnd = aString.indexOf('\t');
        int spaceEnd = aString.indexOf(' ');
        int end = tabEnd < 0 ? spaceEnd : spaceEnd < 0 ? tabEnd : Math.min(spaceEnd, tabEnd);
        while ((end > 0) && (nTokens < maxTokens)) {
            //tokens[nTokens++] = new String(aString.toCharArray(), start, end-start); //  aString.substring(start, end);
            tokens[nTokens++] = aString.substring(start, end);

            start = end + 1;
            // Gobble up any whitespace before next token -- don't gobble tabs, consecutive tabs => empty cell
            while (start < aString.length() && aString.charAt(start) == ' ') {
                start++;
            }

            tabEnd = aString.indexOf('\t', start);
            spaceEnd = aString.indexOf(' ', start);
            end = tabEnd < 0 ? spaceEnd : spaceEnd < 0 ? tabEnd : Math.min(spaceEnd, tabEnd);

        }

        // Add the trailing string
        if (nTokens < maxTokens) {
            String trailingString = aString.substring(start);
            tokens[nTokens++] = trailingString;
        }
        return nTokens;
    }

    public static <T extends Comparable<? super T>> boolean isSorted(Iterable<T> iterable) {
        Iterator<T> iter = iterable.iterator();
        if (!iter.hasNext())
            return true;

        T t = iter.next();
        while (iter.hasNext()) {
            T t2 = iter.next();
            if (t.compareTo(t2) > 0)
                return false;

            t = t2;
        }

        return true;
    }

    /**
     * Convert an rgb string, hex, or symbol to a color.
     *
     * @param string
     * @return
     */
    public static Color parseColor(String string) {
        try {
            Color c = colorCache.get(string);
            if (c == null) {
                if (string.contains(",")) {
                    String[] rgb = string.split(",");
                    int red = Integer.parseInt(rgb[0]);
                    int green = Integer.parseInt(rgb[1]);
                    int blue = Integer.parseInt(rgb[2]);
                    c = new Color(red, green, blue);
                } else if (string.startsWith("#")) {
                    c = hexToColor(string.substring(1));
                } else {
                    String hexString = colorSymbols.get(string.toLowerCase());
                    if (hexString != null) {
                        c = hexToColor(hexString);
                    }
                }

                if (c == null) {
                    c = Color.black;
                }
                colorCache.put(string, c);
            }
            return c;

        } catch (NumberFormatException numberFormatException) {
            log.error("Error in color string. ", numberFormatException);
            return Color.black;
        }
    }


    private static Color hexToColor(String string) {
        if (string.length() == 6) {
            int red = Integer.parseInt(string.substring(0, 2), 16);
            int green = Integer.parseInt(string.substring(2, 4), 16);
            int blue = Integer.parseInt(string.substring(4, 6), 16);
            return new Color(red, green, blue);
        } else {
            return null;
        }

    }
}
