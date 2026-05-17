package net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display.slot;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.function.Function;

public final class WithAnyPotionDisplay implements SlotDisplay {
    private final SlotDisplay display;

    public WithAnyPotionDisplay(SlotDisplay display) {
        this.display = display;
    }

    public static WithAnyPotionDisplay read(FriendlyByteBuf buf, FriendlyByteBuf.Reader<Item> reader) {
        return new WithAnyPotionDisplay(SlotDisplay.read(buf, reader));
    }

    @Override
    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeVarInt(BuiltInRegistries.SLOT_DISPLAY_TYPE.getId(SlotDisplayTypes.WITH_ANY_POTION));
        this.display.write(buf, writer);
    }

    @Override
    public String toString() {
        return "WithAnyPotionDisplay{" +
                "display=" + display +
                '}';
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        this.display.applyClientboundData(function);
    }
}
