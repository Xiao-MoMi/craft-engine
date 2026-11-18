package net.momirealms.craftengine.core.util;

import java.util.ArrayList;
import java.util.List;

public final class ListUtils {
    private ListUtils() {
    }

    @SafeVarargs
    public static <T> List<T> newNonNullList(T... elements) {
        List<T> list = new ArrayList<>(elements.length);
        for (T element : elements) {
            if (element != null) list.add(element);
        }
        return list;
    }

    public static Object getIndexed(Object value, String path, int start) {
        while (start < path.length()) {
            if (!(value instanceof List<?> list) || path.charAt(start) != '[') {
                return null;
            }
            int end = path.indexOf(']', start + 1);
            if (end < 0) {
                return null;
            }
            int index;
            try {
                index = Integer.parseInt(path, start + 1, end, 10);
            } catch (NumberFormatException exception) {
                return null;
            }
            if (index < 0 || index >= list.size()) {
                return null;
            }
            value = list.get(index);
            start = end + 1;
        }
        return value;
    }
}
