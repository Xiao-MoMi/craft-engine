package net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display.slot;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.function.Function;

public final class DyedSlotDisplay implements SlotDisplay {
    private final SlotDisplay dye;
    private final SlotDisplay target;

    public DyedSlotDisplay(SlotDisplay dye, SlotDisplay target) {
        this.dye = dye;
        this.target = target;
    }

    public static DyedSlotDisplay read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        return new DyedSlotDisplay(SlotDisplay.read(buf, reader), SlotDisplay.read(buf, reader));
    }

    @Override
    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeVarInt(BuiltInRegistries.SLOT_DISPLAY_TYPE.getId(SlotDisplayTypes.DYED));
        this.dye.write(buf, writer);
        this.target.write(buf, writer);
    }

    @Override
    public String toString() {
        return "DyedSlotDisplay{" +
                "dye=" + dye +
                ", target=" + target +
                '}';
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        this.dye.applyClientboundData(function);
        this.target.applyClientboundData(function);
    }
}
