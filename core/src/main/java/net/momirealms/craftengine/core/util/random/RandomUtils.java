package net.momirealms.craftengine.core.util.random;

import java.util.concurrent.ThreadLocalRandom;

public final class RandomUtils {

    private RandomUtils() {}

    public static double generateRandomDouble(double min, double max) {
        return min + (max - min) * ThreadLocalRandom.current().nextDouble();
    }

    public static float generateRandomFloat(float min, float max) {
        return min + (max - min) * ThreadLocalRandom.current().nextFloat();
    }

    public static int generateRandomInt(int minInclusive, int maxExclusive) {
        return minInclusive >= maxExclusive ? minInclusive : ThreadLocalRandom.current().nextInt(maxExclusive - minInclusive) + minInclusive;
    }

    public static boolean generateRandomBoolean() {
        return ThreadLocalRandom.current().nextBoolean();
    }

    public static long generateRandomLong() {
        return ThreadLocalRandom.current().nextLong();
    }

    public static double triangle(double mode, double deviation) {
        return mode + deviation * (generateRandomDouble(0,1) - generateRandomDouble(0,1));
    }

    public static <T> T getRandomElementFromArray(T[] array) {
        int index = ThreadLocalRandom.current().nextInt(array.length);
        return array[index];
    }

    public static <T> T[] getRandomElementsFromArray(T[] array, int count) {
        if (count > array.length) {
            throw new IllegalArgumentException("Count cannot be greater than array length");
        }
        @SuppressWarnings("unchecked")
        T[] result = (T[]) new Object[count];
        for (int i = 0; i < count; i++) {
            int index = ThreadLocalRandom.current().nextInt(array.length - i);
            result[i] = array[index];
            array[index] = array[array.length - i - 1];
        }
        return result;
    }
}