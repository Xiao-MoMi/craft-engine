package net.momirealms.craftengine.core.item.recipe.network.modern.display.slot;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public class AnyFuelDisplay implements SlotDisplay {
    public static final AnyFuelDisplay INSTANCE = new AnyFuelDisplay();

    public static AnyFuelDisplay read(FriendlyByteBuf buf) {
        return INSTANCE;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(1);
    }

    @Override
    public String toString() {
        return "AnyFuelDisplay{}";
    }
}
