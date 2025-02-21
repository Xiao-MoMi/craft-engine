package net.momirealms.craftengine.core.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArrayUtils {

    private ArrayUtils() {}

    public static <T> T[] subArray(T[] array, int index) {
        if (index < 0) {
            throw new IllegalArgumentException("Index should be a value no lower than 0");
        }
        if (array.length <= index) {
            @SuppressWarnings("unchecked")
            T[] emptyArray = (T[]) Array.newInstance(array.getClass().getComponentType(), 0);
            return emptyArray;
        }
        @SuppressWarnings("unchecked")
        T[] subArray = (T[]) Array.newInstance(array.getClass().getComponentType(), array.length - index);
        System.arraycopy(array, index, subArray, 0, array.length - index);
        return subArray;
    }

    public static <T> List<T[]> splitArray(T[] array, int chunkSize) {
        List<T[]> result = new ArrayList<>();
        for (int i = 0; i < array.length; i += chunkSize) {
            int end = Math.min(array.length, i + chunkSize);
            @SuppressWarnings("unchecked")
            T[] chunk = (T[]) Array.newInstance(array.getClass().getComponentType(), end - i);
            System.arraycopy(array, i, chunk, 0, end - i);
            result.add(chunk);
        }
        return result;
    }

    public static <T> T[] appendElementToArrayTail(T[] array, T element) {
        T[] newArray = Arrays.copyOf(array, array.length + 1);
        newArray[array.length] = element;
        return newArray;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] appendElementToArrayHead(T[] array, T element) {
        T[] newArray = (T[]) new Object[array.length + 1];
        System.arraycopy(array, 0, newArray, 1, array.length);
        newArray[0] = element;
        return newArray;
    }

    public static String[] splitValue(String value) {
        return value.substring(value.indexOf('[') + 1, value.lastIndexOf(']'))
                .replaceAll("\\s", "")
                .split(",");
    }

    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }
}
