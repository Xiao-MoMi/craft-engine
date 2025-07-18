package net.momirealms.craftengine.core.item.recipe.network.modern.display.slot;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public class ItemSlotDisplay implements SlotDisplay {
    private final int item;

    public ItemSlotDisplay(int item) {
        this.item = item;
    }

    public static ItemSlotDisplay read(FriendlyByteBuf buf) {
        int item = buf.readVarInt();
        return new ItemSlotDisplay(item);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(2);
        buf.writeVarInt(this.item);
    }

    public int item() {
        return item;
    }

    @Override
    public String toString() {
        return "ItemSlotDisplay{" +
                "item=" + item +
                '}';
    }
}
