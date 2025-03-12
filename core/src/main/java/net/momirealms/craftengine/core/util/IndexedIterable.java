package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.Nullable;

public interface IndexedIterable<T> extends Iterable<T> {

    int ABSENT_RAW_ID = -1;

    int getRawId(T value);

    @Nullable
    T get(int index);

    default T getOrThrow(int index) {
        T object = this.get(index);
        if (object == null) {
            throw new IllegalArgumentException("No value with id " + index);
        } else {
            return object;
        }
    }

    default int getRawIdOrThrow(T value) {
        int i = this.getRawId(value);
        if (i == ABSENT_RAW_ID) {
            throw new IllegalArgumentException("Can't find id for '" + value + "' in map " + this);
        } else {
            return i;
        }
    }

    int size();
}
