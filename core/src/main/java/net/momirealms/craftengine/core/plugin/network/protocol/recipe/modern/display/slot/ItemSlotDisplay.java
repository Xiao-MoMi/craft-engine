package net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display.slot;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public final class ItemSlotDisplay implements SlotDisplay {
    private final int item;

    public ItemSlotDisplay(int item) {
        this.item = item;
    }

    public static ItemSlotDisplay read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        int item = buf.readVarInt();
        return new ItemSlotDisplay(item);
    }

    @Override
    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeVarInt(BuiltInRegistries.SLOT_DISPLAY_TYPE.getId(SlotDisplayTypes.ITEM));
        buf.writeVarInt(this.item);
    }

    public int item() {
        return this.item;
    }

    @Override
    public String toString() {
        return "ItemSlotDisplay{" +
                "item=" + this.item +
                '}';
    }
}
