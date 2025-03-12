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

import org.jetbrains.annotations.Nullable;

public interface IndexedIterable<T> extends Iterable<T> {

    int ABSENT_RAW_ID = -1;

    int getRawId(T value);

    @Nullable
    T get(int index);

    default T getOrThrow(int index) {
        T object = this.get(index);
        if (object == null) {
            throw new IllegalArgumentException("No value with id " + index);
        } else {
            return object;
        }
    }

    default int getRawIdOrThrow(T value) {
        int i = this.getRawId(value);
        if (i == ABSENT_RAW_ID) {
            throw new IllegalArgumentException("Can't find id for '" + value + "' in map " + this);
        } else {
            return i;
        }
    }

    int size();
}
