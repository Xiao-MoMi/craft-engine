package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class IntIdentityList implements IndexedIterable<Integer> {
    private final int size;

    public IntIdentityList(int size) {
        this.size = size;
    }

    @Override
    public int getRawId(Integer value) {
        return value;
    }

    @Override
    public @NotNull Integer get(int index) {
        return index;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public @NotNull Iterator<Integer> iterator() {
        return new IntIterator(size);
    }

    private static class IntIterator implements Iterator<Integer> {
        private final int size;
        private int current;

        public IntIterator(int size) {
            this.size = size;
            this.current = 0;
        }

        @Override
        public boolean hasNext() {
            return current < size;
        }

        @Override
        public Integer next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return current++;
        }
    }
}