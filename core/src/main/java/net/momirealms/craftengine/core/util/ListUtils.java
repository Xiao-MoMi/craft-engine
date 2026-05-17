package net.momirealms.craftengine.core.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ListUtils {
    private ListUtils() {}

    public static <T> List<T> compact(final List<T> list) {
        if (list.isEmpty()) return Collections.emptyList();
        if (list.size() == 1) return List.of(list.get(0));
        if (list.size() == 2) return List.of(list.get(0), list.get(1));
        return list;
    }

    @SafeVarargs
    public static <T> List<T> newNonNullList(T... elements) {
        List<T> list = new ArrayList<>(elements.length);
        for (T element : elements) {
            if (element != null) list.add(element);
        }
        return list;
    }
}
