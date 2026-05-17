package net.momirealms.craftengine.core.util;

import java.lang.reflect.Array;
import java.util.*;

public final class ArrayUtils {
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

    @SuppressWarnings("unchecked")
    public static <T> T[] merge(T[] array1, T[] array2) {
        if (array1 == null && array2 == null) {
            return null;
        }
        if (array1 == null) {
            return Arrays.copyOf(array2, array2.length);
        }
        if (array2 == null) {
            return Arrays.copyOf(array1, array1.length);
        }
        T[] mergedArray = (T[]) Array.newInstance(
                array1.getClass().getComponentType(),
                array1.length + array2.length
        );
        System.arraycopy(array1, 0, mergedArray, 0, array1.length);
        System.arraycopy(array2, 0, mergedArray, array1.length, array2.length);
        return mergedArray;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] mergeNoCopy(T[] array1, T[] array2) {
        if (array1 == null && array2 == null) {
            return null;
        }
        if (array1 == null) {
            return array2;
        }
        if (array2 == null) {
            return array1;
        }
        T[] mergedArray = (T[]) Array.newInstance(
                array1.getClass().getComponentType(),
                array1.length + array2.length
        );
        System.arraycopy(array1, 0, mergedArray, 0, array1.length);
        System.arraycopy(array2, 0, mergedArray, array1.length, array2.length);
        return mergedArray;
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

    public static int[] appendIntToArrayHead(int[] array, int element) {
        int[] newArray = new int[array.length + 1];
        System.arraycopy(array, 0, newArray, 1, array.length);
        newArray[0] = element;
        return newArray;
    }

    public static String[] splitValue(String value) {
        return value.substring(value.indexOf('[') + 1, value.lastIndexOf(']'))
                .replaceAll("\\s", "")
                .split(",");
    }

    public static <T> boolean contains(T[] array, T element) {
        if (array == null || element == null) {
            return false;
        }
        for (T item : array) {
            if (Objects.equals(item, element)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEmpty(Object[] array) {
        return array == null || array.length == 0;
    }

    public static <T> T[] collectionToArray(Collection<T> array, Class<T> clazz) {
        @SuppressWarnings("unchecked")
        T[] res = (T[]) Array.newInstance(clazz, array.size());
        int i = 0;
        for (T item : array) {
            res[i++] = item;
        }
        return res;
    }
}
