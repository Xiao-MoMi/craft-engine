package net.momirealms.craftengine.core.util;

import java.util.Objects;

/**
 * A generic class representing a tuple with three values.
 * This class provides methods for creating and accessing tuples with three values.
 *
 * @param <L> the type of the left value
 * @param <M> the type of the middle value
 * @param <R> the type of the right value
 */
public record Tuple<L, M, R>(L left, M mid, R right) {

    /**
     * Creates a new {@link Tuple} with the specified left, middle, and right values.
     *
     * @param left  the left value
     * @param mid   the middle value
     * @param right the right value
     * @param <L>   the type of the left value
     * @param <M>   the type of the middle value
     * @param <R>   the type of the right value
     * @return a new {@link Tuple} with the specified values
     */
    public static <L, M, R> Tuple<L, M, R> of(final L left, final M mid, final R right) {
        return new Tuple<>(left, mid, right);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Tuple<?, ?, ?> tuple = (Tuple<?, ?, ?>) object;
        return Objects.equals(mid, tuple.mid) && Objects.equals(left, tuple.left) && Objects.equals(right, tuple.right);
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, mid, right);
    }
}