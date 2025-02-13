package net.momirealms.craftengine.core.util;

import com.google.common.collect.Iterators;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MCUtils {

    private MCUtils() {}

    private static final int[] MULTIPLY_DE_BRUIJN_BIT_POSITION = new int[]{0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9};

    public static int fastFloor(double value) {
        int truncated = (int) value;
        return value < (double) truncated ? truncated - 1 : truncated;
    }

    public static int fastFloor(float value) {
        int truncated = (int) value;
        return value < (double) truncated ? truncated - 1 : truncated;
    }

    public static int murmurHash3Mixer(int value) {
        value ^= value >>> 16;
        value *= -2048144789;
        value ^= value >>> 13;
        value *= -1028477387;
        return value ^ value >>> 16;
    }

    public static int ceil(double value) {
        int i = (int)value;
        return value > (double)i ? i + 1 : i;
    }

    public static boolean isPowerOfTwo(int value) {
        return value != 0 && (value & value - 1) == 0;
    }

    public static int smallestEncompassingPowerOfTwo(int value) {
        int i = value - 1;
        i |= i >> 1;
        i |= i >> 2;
        i |= i >> 4;
        i |= i >> 8;
        i |= i >> 16;
        return i + 1;
    }

    public static int ceilLog2(int value) {
        value = isPowerOfTwo(value) ? value : smallestEncompassingPowerOfTwo(value);
        return MULTIPLY_DE_BRUIJN_BIT_POSITION[(int)((long)value * 125613361L >> 27) & 31];
    }

    public static int positiveCeilDiv(int a, int b) {
        return -Math.floorDiv(-a, b);
    }

    public static int idealHash(int value) {
        value ^= value >>> 16;
        value *= -2048144789;
        value ^= value >>> 13;
        value *= -1028477387;
        value ^= value >>> 16;
        return value;
    }

    public static long getUnsignedDivisorMagic(final long divisor, final int bits) {
        return ((1L << bits) - 1L) / divisor + 1L;
    }

    public static <T> T make(T object, Consumer<? super T> initializer) {
        initializer.accept(object);
        return object;
    }

    public static <T> Predicate<T> allOf() {
        return o -> true;
    }

    @SuppressWarnings("unchecked")
    public static <T> Predicate<T> allOf(Predicate<? super T> a) {
        return (Predicate<T>) a;
    }

    public static <T> Predicate<T> allOf(Predicate<? super T> a, Predicate<? super T> b) {
        return o -> a.test(o) && b.test(o);
    }

    public static <T> Predicate<T> allOf(Predicate<? super T> a, Predicate<? super T> b, Predicate<? super T> c) {
        return o -> a.test(o) && b.test(o) && c.test(o);
    }

    public static <T> Predicate<T> allOf(Predicate<? super T> a, Predicate<? super T> b, Predicate<? super T> c, Predicate<? super T> d) {
        return o -> a.test(o) && b.test(o) && c.test(o) && d.test(o);
    }

    public static <T> Predicate<T> allOf(Predicate<? super T> a, Predicate<? super T> b, Predicate<? super T> c, Predicate<? super T> d, Predicate<? super T> e) {
        return o -> a.test(o) && b.test(o) && c.test(o) && d.test(o) && e.test(o);
    }

    @SafeVarargs
    public static <T> Predicate<T> allOf(Predicate<? super T>... predicates) {
        return o -> {
            for (Predicate<? super T> predicate : predicates) {
                if (!predicate.test(o)) {
                    return false;
                }
            }

            return true;
        };
    }

    public static <T> Predicate<T> allOf(List<? extends Predicate<? super T>> predicates) {
        return switch (predicates.size()) {
            case 0 -> allOf();
            case 1 -> allOf((Predicate<? super T>) predicates.get(0));
            case 2 -> allOf((Predicate<? super T>) predicates.get(0), (Predicate<? super T>) predicates.get(1));
            case 3 -> allOf((Predicate<? super T>) predicates.get(0), (Predicate<? super T>) predicates.get(1), (Predicate<? super T>) predicates.get(2));
            case 4 -> allOf(
                    (Predicate<? super T>) predicates.get(0),
                    (Predicate<? super T>) predicates.get(1),
                    (Predicate<? super T>) predicates.get(2),
                    (Predicate<? super T>) predicates.get(3)
            );
            case 5 -> allOf(
                    (Predicate<? super T>) predicates.get(0),
                    (Predicate<? super T>) predicates.get(1),
                    (Predicate<? super T>) predicates.get(2),
                    (Predicate<? super T>) predicates.get(3),
                    (Predicate<? super T>) predicates.get(4)
            );
            default -> {
                Predicate<? super T>[] predicates2 = predicates.toArray(Predicate[]::new);
                yield allOf(predicates2);
            }
        };
    }

    public static <T> T findPreviousInIterable(Iterable<T> iterable, @Nullable T object) {
        Iterator<T> iterator = iterable.iterator();
        T previous = null;
        while (iterator.hasNext()) {
            T current = iterator.next();
            if (current == object) {
                if (previous == null) {
                    previous = iterator.hasNext() ? Iterators.getLast(iterator) : object;
                }
                break;
            }
            previous = current;
        }
        return previous;
    }

    public static <T> T findNextInIterable(Iterable<T> iterable, @Nullable T object) {
        Iterator<T> iterator = iterable.iterator();
        T next = iterator.next();
        if (object != null) {
            T current = next;
            while (current != object) {
                if (iterator.hasNext()) {
                    current = iterator.next();
                }
            }
            if (iterator.hasNext()) {
                return iterator.next();
            }
        }
        return next;
    }
}
