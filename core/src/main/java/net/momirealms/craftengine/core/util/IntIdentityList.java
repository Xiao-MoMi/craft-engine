package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class IntIdentityList implements IndexedIterable<Integer> {
    private final int size;
    private final List<Integer> list;

    public IntIdentityList(int size) {
        this.size = size;
        list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(i);
        }
    }

    @Override
    public int getRawId(Integer value) {
        return value;
    }

    @Override
    public @Nullable Integer get(int index) {
        return index;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public @NotNull Iterator<Integer> iterator() {
        return list.iterator();
    }
}
