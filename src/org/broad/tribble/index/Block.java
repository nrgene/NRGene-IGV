package org.broad.tribble.index;

/**
 * Represents a contiguous block of bytes in a file, defined by a start position and size (in bytes)
*/
public class Block {

    private long startPosition;
    private int size;

    /**
     * Constructs ...
     *
     * @param startPosition
     * @param size
     */
    public Block(long startPosition, int size) {
        this.startPosition = startPosition;
        this.size = size;
    }

    /**
     * @return the startPosition
     */
    public long getStartPosition() {
        return startPosition;
    }

    public long getEndPosition() {
        return startPosition + size;
    }

    /**
     * This method is used to aid in consolidating blocks
     */
    public void setEndPosition(long endPosition) {
        if(endPosition < startPosition)
            throw new IllegalArgumentException("Attempting to set block end position to " +
                                                                           endPosition + " which is before the start of " + startPosition);
        size = (int) (endPosition - startPosition);

    }

    /**
     * @return the # of bytes in this block
     */
    public int getSize() {
        return size;
    }

    public boolean equals(Object obj) {
        if ( this == obj ) return true;
        if ( ! (obj instanceof Block) ) return false;
        Block otherBlock = (Block)obj;
        return this.startPosition == otherBlock.startPosition && this.size == otherBlock.size;
    }
}
