package net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display.slot.SlotDisplay;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record StonecutterRecipeDisplay(SlotDisplay input, SlotDisplay result, SlotDisplay craftingStation) implements RecipeDisplay {

    public static StonecutterRecipeDisplay read(FriendlyByteBuf buffer, FriendlyByteBuf.Reader<Item> reader) {
        SlotDisplay input = SlotDisplay.read(buffer, reader);
        SlotDisplay result = SlotDisplay.read(buffer, reader);
        SlotDisplay craftingStation = SlotDisplay.read(buffer, reader);
        return new StonecutterRecipeDisplay(input, result, craftingStation);
    }

    @Override
    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeVarInt(3);
        this.input.write(buf, writer);
        this.result.write(buf, writer);
        this.craftingStation.write(buf, writer);
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        this.input.applyClientboundData(function);
        this.result.applyClientboundData(function);
        this.craftingStation.applyClientboundData(function);
    }

    @Override
    public @NotNull String toString() {
        return "StonecutterRecipeDisplay{" +
                "craftingStation=" + this.craftingStation +
                ", input=" + this.input +
                ", result=" + this.result +
                '}';
    }
}
