package net.momirealms.craftengine.core.world.chunk.storage;

import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.BitSet;

/**
 * A class that manages a bitmap for tracking used and free regions of memory or storage.
 * This class provides methods to allocate, free, and query regions in the bitmap.
 */
public final class RegionBitmap {
    private final BitSet used = new BitSet();

    /**
     * Copies the state of another RegionBitmap into this one.
     * The bits from the other RegionBitmap are copied into this one.
     * @param other The other RegionBitmap to copy from
     */
    public void copyFrom(RegionBitmap other) {
        BitSet thisBitset = this.used;
        BitSet otherBitset = other.used;
        for (int i = 0; i < Math.max(thisBitset.size(), otherBitset.size()); ++i) {
            thisBitset.set(i, otherBitset.get(i));
        }
    }

    /**
     * Attempts to allocate a region starting from a given position with a specified length.
     * The allocation succeeds if the region is free; otherwise, it returns false.
     * @param from The starting position of the allocation
     * @param length The length of the region to allocate
     * @return true if the allocation was successful, false otherwise
     */
    public boolean tryAllocate(int from, int length) {
        BitSet bitset = this.used;
        // Find the next set bit from the specified position
        int firstSet = bitset.nextSetBit(from);
        // If there is an overlap with an already allocated region, return false
        if (firstSet > 0 && firstSet < (from + length)) {
            return false;
        }
        // Mark the region as used
        bitset.set(from, from + length);
        return true;
    }

    /**
     * Forces a region to be marked as used starting from a specified position and with a given size.
     * @param start The starting position of the forced allocation
     * @param size The size of the region to force allocate
     */
    public void allocate(int start, int size) {
        this.used.set(start, start + size);
    }

    /**
     * Frees a region by clearing the bits corresponding to the given range.
     * @param start The starting position of the region to free
     * @param size The size of the region to free
     */
    public void free(int start, int size) {
        this.used.clear(start, start + size);
    }

    /**
     * Allocates a region of the specified size by finding the next available free region
     * and marking it as used.
     * @param size The size of the region to allocate
     * @return The starting position of the allocated region
     */
    public int allocate(int size) {
        int i = 0;
        while(true) {
            // Find the next free region after the current position
            int start = this.used.nextClearBit(i);
            // Find the next set bit after the free region
            int end = this.used.nextSetBit(start);
            // If there's enough space, allocate and return the starting position
            if (end == -1 || end - start >= size) {
                this.allocate(start, size);
                return start;
            }
            i = end;
        }
    }

    /**
     * Returns a set of integers representing the used regions in the bitmap.
     * @return An IntSet containing the positions of used bits
     */
    public IntSet getUsed() {
        return this.used.stream().collect(IntArraySet::new, IntCollection::add, IntCollection::addAll);
    }
}
