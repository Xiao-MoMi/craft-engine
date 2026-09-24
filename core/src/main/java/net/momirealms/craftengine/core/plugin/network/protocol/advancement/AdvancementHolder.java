package net.momirealms.craftengine.core.plugin.network.protocol.advancement;

import net.kyori.adventure.text.Component;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;

import java.util.function.Function;

public record AdvancementHolder(Key id, Advancement advancement, float x, float y) {

    public AdvancementHolder(Key id, Advancement advancement) {
        this(id, advancement, 0, 0);
    }

    public static AdvancementHolder read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        Key key = buf.readKey();
        Advancement ad = Advancement.read(buf, reader);
        float x = VersionHelper.isOrAbove26_3 ? buf.readFloat() : 0;
        float y = VersionHelper.isOrAbove26_3 ? buf.readFloat() : 0;
        return new AdvancementHolder(key, ad, x, y);
    }

    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeKey(this.id);
        this.advancement.write(buf, writer);
        if (VersionHelper.isOrAbove26_3) {
            buf.writeFloat(this.x);
            buf.writeFloat(this.y);
        }
    }

    public void applyClientboundData(Function<Item, Item> function) {
        this.advancement.applyClientboundData(function);
    }

    public void replaceNetworkTags(Function<Component, Component> function) {
        this.advancement.replaceNetworkTags(function);
    }
}
