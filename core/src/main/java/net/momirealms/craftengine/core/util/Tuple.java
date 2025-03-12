/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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