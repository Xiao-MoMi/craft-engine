package net.momirealms.craftengine.core.util;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public final class NonNullList<E> extends AbstractList<E> {
    private final List<E> list;
    @Nullable
    private final E defaultValue;

    public static <E> NonNullList<E> create() {
        return new NonNullList<>(new ArrayList<>(), null);
    }

    public static <E> NonNullList<E> createWithCapacity(int initialCapacity) {
        return new NonNullList<>(new ArrayList<>(initialCapacity), null);
    }

    public static <E> NonNullList<E> withSize(int size, @Nonnull E defaultValue) {
        Objects.requireNonNull(defaultValue, "Default value cannot be null");
        @SuppressWarnings("unchecked")
        E[] array = (E[]) new Object[size];
        Arrays.fill(array, defaultValue);
        return new NonNullList<>(Arrays.asList(array), defaultValue);
    }

    @SafeVarargs
    public static <E> NonNullList<E> of(@Nonnull E defaultValue, E... elements) {
        Objects.requireNonNull(defaultValue, "Default value cannot be null");
        for (E element : elements) {
            Objects.requireNonNull(element, "Elements cannot contain null");
        }
        return new NonNullList<>(Arrays.asList(elements), defaultValue);
    }

    private NonNullList(List<E> list, @Nullable E defaultValue) {
        this.list = list;
        this.defaultValue = defaultValue;
    }

    @Override
    @Nonnull
    public E get(int index) {
        return this.list.get(index);
    }

    @Override
    public E set(int index, @Nonnull E value) {
        Objects.requireNonNull(value, "NonNullList cannot contain null values");
        return this.list.set(index, value);
    }

    @Override
    public void add(int index, @Nonnull E value) {
        Objects.requireNonNull(value, "NonNullList cannot contain null values");
        this.list.add(index, value);
    }

    @Override
    public E remove(int index) {
        return this.list.remove(index);
    }

    @Override
    public int size() {
        return this.list.size();
    }

    @Override
    public void clear() {
        if (this.defaultValue == null) {
            this.list.clear();
        } else {
            for (int i = 0; i < this.size(); ++i) {
                this.set(i, this.defaultValue);
            }
        }
    }
}