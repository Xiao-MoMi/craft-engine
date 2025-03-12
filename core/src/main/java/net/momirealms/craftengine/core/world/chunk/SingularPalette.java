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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class SingularPalette<T> implements Palette<T> {
    private final IndexedIterable<T> idList;
    @Nullable
    private T entry;
    private final PaletteResizeListener<T> listener;

    public SingularPalette(IndexedIterable<T> idList, PaletteResizeListener<T> listener, List<T> entries) {
        this.idList = idList;
        this.listener = listener;
        if (!entries.isEmpty()) {
            this.entry = entries.get(0);
        }
    }

    public static <A> Palette<A> create(int bitSize, IndexedIterable<A> idList, PaletteResizeListener<A> listener, List<A> entries) {
        return new SingularPalette<>(idList, listener, entries);
    }

    @Override
    public int index(T object) {
        if (this.entry != null && this.entry != object) {
            return this.listener.onResize(1, object);
        } else {
            this.entry = object;
            return 0;
        }
    }

    @Override
    public boolean hasAny(Predicate<T> predicate) {
        if (this.entry == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            return predicate.test(this.entry);
        }
    }

    @Override
    public T get(int id) {
        if (this.entry != null && id == 0) {
            return this.entry;
        } else {
            throw new IllegalStateException("Missing Palette entry for id " + id + ".");
        }
    }

    @Override
    public int getSize() {
        return 1;
    }

    @Override
    public Palette<T> copy(PaletteResizeListener<T> resizeListener) {
        if (this.entry == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            return this;
        }
    }

    @Override
    public void remap(Function<T, T> function) {
        this.entry = function.apply(this.entry);
    }

    @Override
    public boolean canRemap() {
        return true;
    }

    @Override
    public void readPacket(FriendlyByteBuf buf) {
        this.entry = this.idList.getOrThrow(buf.readVarInt());
    }

    @Override
    public void writePacket(FriendlyByteBuf buf) {
        if (this.entry == null) {
            throw new IllegalStateException("Use of an uninitialized palette");
        } else {
            buf.writeVarInt(this.idList.getRawId(this.entry));
        }
    }
}
