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

package net.momirealms.craftengine.core.util.context;

import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

public class ContextKey<T> {
    private final Key id;

    public ContextKey(@NotNull Key id) {
        this.id = id;
    }

    @NotNull
    public Key id() {
        return id;
    }

    @NotNull
    public static <T> ContextKey<T> of(@NotNull Key id) {
        return new ContextKey<>(id);
    }

    @NotNull
    public static <T> ContextKey<T> of(@NotNull String id) {
        return new ContextKey<>(Key.of(id));
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContextKey<?> that)) return false;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
