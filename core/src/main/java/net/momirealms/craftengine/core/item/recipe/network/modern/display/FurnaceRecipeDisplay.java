package net.momirealms.craftengine.core.item.recipe.network.modern.display;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.recipe.network.modern.display.slot.SlotDisplay;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public record FurnaceRecipeDisplay(SlotDisplay ingredient, SlotDisplay fuel, SlotDisplay result, SlotDisplay craftingStation, int duration, float experience) implements RecipeDisplay {

    public static FurnaceRecipeDisplay read(FriendlyByteBuf buffer) {
        SlotDisplay ingredient = SlotDisplay.read(buffer);
        SlotDisplay fuel = SlotDisplay.read(buffer);
        SlotDisplay result = SlotDisplay.read(buffer);
        SlotDisplay craftingStation = SlotDisplay.read(buffer);
        int duration = buffer.readVarInt();
        float experience = buffer.readFloat();
        return new FurnaceRecipeDisplay(ingredient, fuel, result, craftingStation, duration, experience);
    }

    @Override
    public void applyClientboundData(Player player) {
        this.ingredient.applyClientboundData(player);
        this.fuel.applyClientboundData(player);
        this.result.applyClientboundData(player);
        this.craftingStation.applyClientboundData(player);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(2);
        this.ingredient.write(buf);
        this.fuel.write(buf);
        this.result.write(buf);
        this.craftingStation.write(buf);
        buf.writeVarInt(this.duration);
        buf.writeFloat(this.experience);
    }

    @Override
    public @NotNull String toString() {
        return "FurnaceRecipeDisplay{" +
                "craftingStation=" + craftingStation +
                ", ingredient=" + ingredient +
                ", fuel=" + fuel +
                ", result=" + result +
                ", duration=" + duration +
                ", experience=" + experience +
                '}';
    }
}
