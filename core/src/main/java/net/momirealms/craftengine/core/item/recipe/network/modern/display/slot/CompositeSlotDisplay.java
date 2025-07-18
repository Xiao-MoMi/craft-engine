package net.momirealms.craftengine.core.item.recipe.network.modern.display.slot;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public class CompositeSlotDisplay implements SlotDisplay {
    private final List<SlotDisplay> slots;

    public CompositeSlotDisplay(List<SlotDisplay> slots) {
        this.slots = slots;
    }

    public static CompositeSlotDisplay read(FriendlyByteBuf buf) {
        List<SlotDisplay> slots = buf.readCollection(ArrayList::new, SlotDisplay::read);
        return new CompositeSlotDisplay(slots);
    }

    @Override
    public void applyClientboundData(Player player) {
        for (SlotDisplay slotDisplay : this.slots) {
            slotDisplay.applyClientboundData(player);
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(7);
        buf.writeCollection(this.slots, (byteBuf, slotDisplay) -> slotDisplay.write(buf));
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
