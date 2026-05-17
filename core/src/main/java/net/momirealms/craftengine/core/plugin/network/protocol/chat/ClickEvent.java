package net.momirealms.craftengine.core.plugin.network.protocol.chat;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.sparrow.nbt.CompoundTag;

import java.util.function.Function;

public interface ClickEvent {

    ClickEvent.Type<?> type();

    CompoundTag save();

    default void applyClientboundData(Function<Item, Item> function) {
    }

    default void replaceNetworkTags(Function<Component, Component> function) {
    }

    record Type<T extends ClickEvent>(Key id, Function<CompoundTag, T> reader) {

        public T read(final CompoundTag tag) {
            return this.reader.apply(tag);
        }
    }
}
