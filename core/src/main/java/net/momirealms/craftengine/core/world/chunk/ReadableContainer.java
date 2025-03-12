package net.momirealms.craftengine.core.world.chunk;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.IndexedIterable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.LongStream;

public interface ReadableContainer<T> {

    T get(int x, int y, int z);

    void forEachValue(Consumer<T> action);

    boolean hasAny(Predicate<T> predicate);

    void count(PalettedContainer.Counter<T> counter);

    void writePacket(FriendlyByteBuf buf);

    PalettedContainer<T> copy();

    PalettedContainer<T> slice();

    Serialized<T> serialize(IndexedIterable<T> idList, PalettedContainer.PaletteProvider paletteProvider);

    record Serialized<T>(List<T> paletteEntries, Optional<LongStream> storage) {}
}
