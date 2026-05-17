package net.momirealms.craftengine.core.util;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public final class MarkedArrayList<T> extends ArrayList<T> {
    public MarkedArrayList() {
    }

    public MarkedArrayList(@NotNull Collection<? extends T> c) {
        super(c);
    }
}
