package net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display.slot;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public final class AnyFuelDisplay implements SlotDisplay {
    public static final AnyFuelDisplay INSTANCE = new AnyFuelDisplay();

    public static AnyFuelDisplay read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        return INSTANCE;
    }

    @Override
    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeVarInt(BuiltInRegistries.SLOT_DISPLAY_TYPE.getId(SlotDisplayTypes.ANY_FUEL));
    }

    @Override
    public String toString() {
        return "AnyFuelDisplay{}";
    }
}
