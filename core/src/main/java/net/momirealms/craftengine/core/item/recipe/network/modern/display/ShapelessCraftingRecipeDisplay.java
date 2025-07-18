package net.momirealms.craftengine.core.item.recipe.network.modern.display;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.recipe.network.modern.display.slot.SlotDisplay;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record ShapelessCraftingRecipeDisplay(List<SlotDisplay> ingredients, SlotDisplay result, SlotDisplay craftingStation) implements RecipeDisplay {

    public static ShapelessCraftingRecipeDisplay read(FriendlyByteBuf buffer) {
        List<SlotDisplay> ingredients = buffer.readCollection(ArrayList::new, SlotDisplay::read);
        SlotDisplay result = SlotDisplay.read(buffer);
        SlotDisplay craftingStation = SlotDisplay.read(buffer);
        return new ShapelessCraftingRecipeDisplay(ingredients, result, craftingStation);
    }

    @Override
    public void applyClientboundData(Player player) {
        for (SlotDisplay ingredient : ingredients) {
            ingredient.applyClientboundData(player);
        }
        this.result.applyClientboundData(player);
        this.craftingStation.applyClientboundData(player);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(0);
        buf.writeCollection(this.ingredients, (byteBuf, slotDisplay) -> slotDisplay.write(buf));
        this.result.write(buf);
        this.craftingStation.write(buf);
    }

    @Override
    public @NotNull String toString() {
        return "ShapelessCraftingRecipeDisplay{" +
                "craftingStation=" + craftingStation +
                ", ingredients=" + ingredients +
                ", result=" + result +
                '}';
    }
}
