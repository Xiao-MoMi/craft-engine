package net.momirealms.craftengine.core.plugin.network.protocol.advancement;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;

import java.util.function.Function;

public record AdvancementHolder(Key id, Advancement advancement) {

    public static AdvancementHolder read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        Key key = buf.readKey();
        Advancement ad = Advancement.read(buf, reader);
        return new AdvancementHolder(key, ad);
    }

    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeKey(this.id);
        this.advancement.write(buf, writer);
    }

    public void applyClientboundData(Function<Item, Item> function) {
        this.advancement.applyClientboundData(function);
    }

    public void replaceNetworkTags(Function<Component, Component> function) {
        this.advancement.replaceNetworkTags(function);
    }
}
