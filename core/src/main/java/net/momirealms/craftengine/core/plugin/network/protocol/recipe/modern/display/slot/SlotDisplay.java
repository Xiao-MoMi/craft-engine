package net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display.slot;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface SlotDisplay {

    void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer);

    default void applyClientboundData(Function<Item, Item> function) {
    }

    static SlotDisplay read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        return buf.readById(BuiltInRegistries.SLOT_DISPLAY_TYPE).read(buf, reader);
    }

    record Type<T extends SlotDisplay>(Key id, BiFunction<FriendlyByteBuf, FriendlyByteBuf.Reader<Item>, T> reader) {

        public SlotDisplay read(final FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
            return this.reader.apply(buf, reader);
        }
    }
}
