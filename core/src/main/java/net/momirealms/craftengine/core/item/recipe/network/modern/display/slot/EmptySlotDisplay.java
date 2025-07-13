package net.momirealms.craftengine.core.item.recipe.network.modern.display.slot;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public class EmptySlotDisplay implements SlotDisplay {
    public static final EmptySlotDisplay INSTANCE = new EmptySlotDisplay();

    public static EmptySlotDisplay read(FriendlyByteBuf buf) {
        return INSTANCE;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(0);
    }

    @Override
    public String toString() {
        return "EmptySlotDisplay{}";
    }
}
