package net.momirealms.craftengine.core.item.recipe.network.modern.display.slot;

import net.momirealms.craftengine.core.util.FriendlyByteBuf;

public class WithRemainderSlotDisplay implements SlotDisplay {
    private final SlotDisplay input;
    private final SlotDisplay remainder;

    public WithRemainderSlotDisplay(SlotDisplay input, SlotDisplay remainder) {
        this.input = input;
        this.remainder = remainder;
    }

    public static WithRemainderSlotDisplay read(FriendlyByteBuf buf) {
        SlotDisplay input = SlotDisplay.read(buf);
        SlotDisplay remainder = SlotDisplay.read(buf);
        return new WithRemainderSlotDisplay(input, remainder);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(6);
        this.input.write(buf);
        this.remainder.write(buf);
    }

    @Override
    public String toString() {
        return "WithRemainderSlotDisplay{" +
                "input=" + input +
                ", remainder=" + remainder +
                '}';
    }
}
