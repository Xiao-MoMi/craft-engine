package net.momirealms.craftengine.core.world.chunk;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.IndexedIterable;
import net.momirealms.craftengine.core.util.Int2ObjectBiMap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class BiMapPalette<T> implements Palette<T> {
    private final IndexedIterable<T> idList;
    private final Int2ObjectBiMap<T> map;
    private final PaletteResizeListener<T> listener;
    private final int indexBits;

    public BiMapPalette(IndexedIterable<T> idList, int bits, PaletteResizeListener<T> listener, List<T> entries) {
        this(idList, bits, listener);
        entries.forEach(this.map::add);
    }

    public BiMapPalette(IndexedIterable<T> idList, int indexBits, PaletteResizeListener<T> listener) {
        this(idList, indexBits, listener, Int2ObjectBiMap.create(1 << indexBits));
    }

    private BiMapPalette(IndexedIterable<T> idList, int indexBits, PaletteResizeListener<T> listener, Int2ObjectBiMap<T> map) {
        this.idList = idList;
        this.indexBits = indexBits;
        this.listener = listener;
        this.map = map;
    }

    public static <A> Palette<A> create(int bits, IndexedIterable<A> idList, PaletteResizeListener<A> listener, List<A> entries) {
        return new BiMapPalette<>(idList, bits, listener, entries);
    }

    @Override
    public void readPacket(FriendlyByteBuf buf) {
        this.map.clear();
        int i = buf.readVarInt();
        for (int j = 0; j < i; ++j) {
            this.map.add(this.idList.getOrThrow(buf.readVarInt()));
        }
    }

    @Override
    public void writePacket(FriendlyByteBuf buf) {
        int i = this.getSize();
        buf.writeVarInt(i);
        for (int j = 0; j < i; ++j) {
            buf.writeVarInt(this.idList.getRawId(this.map.get(j)));
        }
    }

    @Override
    public int index(T object) {
        int i = this.map.getRawId(object);
        if (i == -1) {
            i = this.map.add(object);
            if (i >= 1 << this.indexBits) {
                i = this.listener.onResize(this.indexBits + 1, object);
            }
        }
        return i;
    }

    @Override
    public boolean hasAny(Predicate<T> predicate) {
        for(int i = 0; i < this.getSize(); ++i) {
            if (predicate.test(this.map.get(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public T get(int id) {
        T object = this.map.get(id);
        if (object == null) {
            throw new RuntimeException("Missing Palette entry for index " + id + ".");
        } else {
            return object;
        }
    }

    public List<T> getElements() {
        ArrayList<T> arrayList = new ArrayList<>();
        this.map.iterator().forEachRemaining(arrayList::add);
        return arrayList;
    }

    @Override
    public int getSize() {
        return this.map.size();
    }

    @Override
    public Palette<T> copy(PaletteResizeListener<T> resizeListener) {
        return new BiMapPalette<>(this.idList, this.indexBits, resizeListener, this.map.copy());
    }

    @Override
    public void remap(Function<T, T> function) {
        this.map.remapValues(function);
    }

    @Override
    public boolean canRemap() {
        return true;
    }
}
