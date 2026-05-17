package net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display.slot;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.function.Function;

public final class ItemStackSlotDisplay implements SlotDisplay {
    private Item item;

    public ItemStackSlotDisplay(Item item) {
        this.item = item;
    }

    public static ItemStackSlotDisplay read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        Item itemStack = reader.apply(buf);
        return new ItemStackSlotDisplay(itemStack);
    }

    @Override
    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeVarInt(BuiltInRegistries.SLOT_DISPLAY_TYPE.getId(SlotDisplayTypes.ITEM_STACK));
        writer.accept(buf, this.item);
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        this.item = function.apply(this.item);
    }

    public Item item() {
        return this.item;
    }

    @Override
    public String toString() {
        return "ItemStackSlotDisplay{" +
                "item=" + this.item.minecraftItem() +
                '}';
    }
}
