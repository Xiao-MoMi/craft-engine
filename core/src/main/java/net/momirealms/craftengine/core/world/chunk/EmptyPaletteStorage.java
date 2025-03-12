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

package net.momirealms.craftengine.core.world.chunk;

import java.util.Arrays;
import java.util.function.IntConsumer;

public record EmptyPaletteStorage(int size) implements PaletteStorage {
    public static final long[] EMPTY_DATA = new long[0];

    public int swap(int index, int value) {
        return 0;
    }

    public void set(int index, int value) {
    }

    public int get(int index) {
        return 0;
    }

    public long[] getData() {
        return EMPTY_DATA;
    }

    public int getElementBits() {
        return 0;
    }

    public void forEach(IntConsumer action) {
        for (int i = 0; i < this.size; ++i) {
            action.accept(0);
        }
    }

    public void writePaletteIndices(int[] out) {
        Arrays.fill(out, 0, this.size, 0);
    }

    public PaletteStorage copy() {
        return this;
    }
}
