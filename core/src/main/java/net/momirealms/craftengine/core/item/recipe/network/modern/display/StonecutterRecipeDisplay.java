package net.momirealms.craftengine.core.item.recipe.network.modern.display;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.recipe.network.modern.display.slot.SlotDisplay;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public record StonecutterRecipeDisplay(SlotDisplay input, SlotDisplay result, SlotDisplay craftingStation) implements RecipeDisplay {

    public static StonecutterRecipeDisplay read(FriendlyByteBuf buffer) {
        SlotDisplay input = SlotDisplay.read(buffer);
        SlotDisplay result = SlotDisplay.read(buffer);
        SlotDisplay craftingStation = SlotDisplay.read(buffer);
        return new StonecutterRecipeDisplay(input, result, craftingStation);
    }

    @Override
    public void applyClientboundData(Player player) {
        this.input.applyClientboundData(player);
        this.result.applyClientboundData(player);
        this.craftingStation.applyClientboundData(player);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(3);
        this.input.write(buf);
        this.result.write(buf);
        this.craftingStation.write(buf);
    }

    @Override
    public @NotNull String toString() {
        return "StonecutterRecipeDisplay{" +
                "craftingStation=" + craftingStation +
                ", input=" + input +
                ", result=" + result +
                '}';
    }
}
