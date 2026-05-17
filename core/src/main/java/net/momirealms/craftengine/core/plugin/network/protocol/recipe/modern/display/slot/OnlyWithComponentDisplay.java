package net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display.slot;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.function.Function;

public final class OnlyWithComponentDisplay implements SlotDisplay {
    private final SlotDisplay display;
    private final int component;

    public OnlyWithComponentDisplay(SlotDisplay display, int component) {
        this.display = display;
        this.component = component;
    }

    public static OnlyWithComponentDisplay read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        return new OnlyWithComponentDisplay(SlotDisplay.read(buf, reader), buf.readVarInt());
    }

    @Override
    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeVarInt(BuiltInRegistries.SLOT_DISPLAY_TYPE.getId(SlotDisplayTypes.ONLY_WITH_COMPONENT));
        this.display.write(buf, writer);
        buf.writeVarInt(this.component);
    }

    @Override
    public String toString() {
        return "OnlyWithComponentDisplay{" +
                "display=" + display +
                ", component=" + component +
                '}';
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        this.display.applyClientboundData(function);
    }
}
