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

public class IdListPalette<T> implements Palette<T> {
    private final IndexedIterable<T> idList;

    public IdListPalette(IndexedIterable<T> idList) {
        this.idList = idList;
    }

    public static <A> Palette<A> create(int bits, IndexedIterable<A> idList, PaletteResizeListener<A> listener, List<A> list) {
        return new IdListPalette<>(idList);
    }

    @Override
    public int index(T object) {
        int i = this.idList.getRawId(object);
        return i == -1 ? 0 : i;
    }

    @Override
    public boolean hasAny(Predicate<T> predicate) {
        return true;
    }

    @Override
    public T get(int id) {
        T object = this.idList.get(id);
        if (object == null) {
            throw new RuntimeException("Missing Palette entry for index " + id + ".");
        } else {
            return object;
        }
    }

    @Override
    public int getSize() {
        return this.idList.size();
    }

    @Override
    public Palette<T> copy(PaletteResizeListener<T> resizeListener) {
        return this;
    }

    @Override
    public void remap(Function<T, T> function) {
        throw new UnsupportedOperationException("This palette does not support remap.");
    }

    @Override
    public boolean canRemap() {
        return false;
    }

    @Override
    public void readPacket(FriendlyByteBuf buf) {
    }

    @Override
    public void writePacket(FriendlyByteBuf buf) {
    }
}
