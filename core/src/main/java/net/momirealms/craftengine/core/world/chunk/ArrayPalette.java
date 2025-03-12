package net.momirealms.craftengine.core.world.chunk;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.IndexedIterable;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class ArrayPalette<T> implements Palette<T> {
    private final IndexedIterable<T> idList;
    private final T[] array;
    private final PaletteResizeListener<T> listener;
    private final int indexBits;
    private int size;

    @SuppressWarnings("unchecked")
    private ArrayPalette(IndexedIterable<T> idList, int bits, PaletteResizeListener<T> listener, List<T> list) {
        this.idList = idList;
        this.array = (T[]) new Object[1 << bits];
        this.indexBits = bits;
        this.listener = listener;

        for (int i = 0; i < list.size(); ++i) {
            this.array[i] = list.get(i);
        }

        this.size = list.size();
    }

    private ArrayPalette(IndexedIterable<T> idList, T[] array, PaletteResizeListener<T> listener, int indexBits, int size) {
        this.idList = idList;
        this.array = array;
        this.listener = listener;
        this.indexBits = indexBits;
        this.size = size;
    }

    public static <A> Palette<A> create(int bits, IndexedIterable<A> idList, PaletteResizeListener<A> listener, List<A> list) {
        return new ArrayPalette<>(idList, bits, listener, list);
    }

    @Override
    public void readPacket(FriendlyByteBuf buf) {
        this.size = buf.readVarInt();
        for(int i = 0; i < this.size; ++i) {
            this.array[i] = this.idList.getOrThrow(buf.readVarInt());
        }
    }

    @Override
    public void writePacket(FriendlyByteBuf buf) {
        buf.writeVarInt(this.size);
        for(int i = 0; i < this.size; ++i) {
            buf.writeVarInt(this.idList.getRawId(this.array[i]));
        }
    }

    @Override
    public int index(T object) {
        int i;
        for(i = 0; i < this.size; ++i) {
            if (this.array[i] == object) {
                return i;
            }
        }

        i = this.size;
        if (i < this.array.length) {
            this.array[i] = object;
            ++this.size;
            return i;
        } else {
            return this.listener.onResize(this.indexBits + 1, object);
        }
    }

    @Override
    public boolean hasAny(Predicate<T> predicate) {
        for(int i = 0; i < this.size; ++i) {
            if (predicate.test(this.array[i])) {
                return true;
            }
        }

        return false;
    }

    @Override
    public T get(int id) {
        if (id >= 0 && id < this.size) {
            return this.array[id];
        } else {
            throw new RuntimeException("Missing Palette entry for index " + id + ".");
        }
    }

    @Override
    public int getSize() {
        return this.size;
    }

    @Override
    public Palette<T> copy(PaletteResizeListener<T> resizeListener) {
        return new ArrayPalette<>(this.idList, this.array.clone(), resizeListener, this.indexBits, this.size);
    }

    @Override
    public void remap(Function<T, T> function) {
        for (int i = 0; i < this.array.length; i++) {
            if (this.array[i] == null) return;
            this.array[i] = function.apply(this.array[i]);
        }
    }

    @Override
    public boolean canRemap() {
        return true;
    }
}
