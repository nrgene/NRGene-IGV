package org.broad.igv.hic.data;


/**
 * @author Jim Robinson
 * @date 9/6/11
 */
public class Chromosome {
    
    private int index;
    private String name;
    private int size;


    public Chromosome(int index, String name, int size) {
        this.index = index;
        this.name = name;
        this.size = size;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

    public int getSize() {
        return size;
    }

    public String toString() {
        return name;
    }

}
