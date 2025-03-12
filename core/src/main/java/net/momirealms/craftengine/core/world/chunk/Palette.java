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

import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.IndexedIterable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public interface Palette<T> {

    int index(T object);

    boolean hasAny(Predicate<T> predicate);

    T get(int id);

    int getSize();

    void readPacket(FriendlyByteBuf buf);

    void writePacket(FriendlyByteBuf buf);

    Palette<T> copy(PaletteResizeListener<T> resizeListener);

    void remap(Function<T, T> function);

    boolean canRemap();

    interface Factory {
        <A> Palette<A> create(int bits, IndexedIterable<A> idList, PaletteResizeListener<A> listener, List<A> list);
    }
}
