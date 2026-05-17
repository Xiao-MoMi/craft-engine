package net.momirealms.craftengine.core.plugin.network.protocol.recipe.legacy;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

@ApiStatus.Obsolete
public final class LegacyIngredient {
    private final Item[] items;

    public LegacyIngredient(Item[] items) {
        this.items = items;
    }

    public Item[] items() {
        return this.items;
    }

    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeArray(this.items, writer);
    }

    public static LegacyIngredient read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        Item[] items = buf.readArray(reader, Item.class);
        return new LegacyIngredient(items);
    }

    public void applyClientboundData(Function<Item, Item> function) {
        for (int i = 0; i < this.items.length; i++) {
            Item item = this.items[i];
            this.items[i] = function.apply(item);
        }
    }
}
