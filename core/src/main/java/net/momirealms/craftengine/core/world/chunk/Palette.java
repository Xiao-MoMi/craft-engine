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
