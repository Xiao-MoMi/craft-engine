package net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display.slot;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class CompositeSlotDisplay implements SlotDisplay {
    private final List<SlotDisplay> slots;

    public CompositeSlotDisplay(List<SlotDisplay> slots) {
        this.slots = slots;
    }

    public static CompositeSlotDisplay read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        List<SlotDisplay> slots = buf.readCollection(ArrayList::new, buffer -> SlotDisplay.read(buf, reader));
        return new CompositeSlotDisplay(slots);
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        for (SlotDisplay slotDisplay : this.slots) {
            slotDisplay.applyClientboundData(function);
        }
    }

    @Override
    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeVarInt(BuiltInRegistries.SLOT_DISPLAY_TYPE.getId(SlotDisplayTypes.COMPOSITE));
        buf.writeCollection(this.slots, (byteBuf, slotDisplay) -> slotDisplay.write(buf, writer));
    }

    public List<SlotDisplay> slots() {
        return this.slots;
    }

    @Override
    public String toString() {
        return "CompositeSlotDisplay{" +
                "slots=" + slots +
                '}';
    }
}
