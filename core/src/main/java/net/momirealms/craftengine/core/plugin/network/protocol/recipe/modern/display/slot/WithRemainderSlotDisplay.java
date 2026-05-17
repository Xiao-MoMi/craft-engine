package net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display.slot;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public final class WithRemainderSlotDisplay implements SlotDisplay {
    private final SlotDisplay input;
    private final SlotDisplay remainder;

    public WithRemainderSlotDisplay(SlotDisplay input, SlotDisplay remainder) {
        this.input = input;
        this.remainder = remainder;
    }

    public static WithRemainderSlotDisplay read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        SlotDisplay input = SlotDisplay.read(buf, reader);
        SlotDisplay remainder = SlotDisplay.read(buf, reader);
        return new WithRemainderSlotDisplay(input, remainder);
    }

    @Override
    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeVarInt(BuiltInRegistries.SLOT_DISPLAY_TYPE.getId(SlotDisplayTypes.WITH_REMAINDER));
        this.input.write(buf, writer);
        this.remainder.write(buf, writer);
    }

    @Override
    public String toString() {
        return "WithRemainderSlotDisplay{" +
                "input=" + this.input +
                ", remainder=" + this.remainder +
                '}';
    }
}
