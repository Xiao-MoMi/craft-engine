package net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display.slot.SlotDisplay;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public record FurnaceRecipeDisplay(SlotDisplay ingredient, SlotDisplay fuel, SlotDisplay result, SlotDisplay craftingStation, int duration, float experience)
        implements RecipeDisplay {

    public static FurnaceRecipeDisplay read(FriendlyByteBuf buffer, FriendlyByteBuf.Reader<Item> reader) {
        SlotDisplay ingredient = SlotDisplay.read(buffer, reader);
        SlotDisplay fuel = SlotDisplay.read(buffer, reader);
        SlotDisplay result = SlotDisplay.read(buffer, reader);
        SlotDisplay craftingStation = SlotDisplay.read(buffer, reader);
        int duration = buffer.readVarInt();
        float experience = buffer.readFloat();
        return new FurnaceRecipeDisplay(ingredient, fuel, result, craftingStation, duration, experience);
    }

    @Override
    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeVarInt(2);
        this.ingredient.write(buf, writer);
        this.fuel.write(buf, writer);
        this.result.write(buf, writer);
        this.craftingStation.write(buf, writer);
        buf.writeVarInt(this.duration);
        buf.writeFloat(this.experience);
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        this.ingredient.applyClientboundData(function);
        this.fuel.applyClientboundData(function);
        this.result.applyClientboundData(function);
        this.craftingStation.applyClientboundData(function);
    }

    @Override
    public @NotNull String toString() {
        return "FurnaceRecipeDisplay{" +
                "craftingStation=" + this.craftingStation +
                ", ingredient=" + this.ingredient +
                ", fuel=" + this.fuel +
                ", result=" + this.result +
                ", duration=" + this.duration +
                ", experience=" + this.experience +
                '}';
    }
}
