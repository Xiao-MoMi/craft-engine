package net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display.slot;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public final class EmptySlotDisplay implements SlotDisplay {
    public static final EmptySlotDisplay INSTANCE = new EmptySlotDisplay();

    public static EmptySlotDisplay read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        return INSTANCE;
    }

    @Override
    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeVarInt(BuiltInRegistries.SLOT_DISPLAY_TYPE.getId(SlotDisplayTypes.EMPTY));
    }

    @Override
    public String toString() {
        return "EmptySlotDisplay{}";
    }
}
