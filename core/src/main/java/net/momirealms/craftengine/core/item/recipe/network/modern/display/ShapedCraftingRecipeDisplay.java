package net.momirealms.craftengine.core.item.recipe.network.modern.display;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.recipe.network.modern.display.slot.SlotDisplay;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record ShapedCraftingRecipeDisplay(int width, int height, List<SlotDisplay> ingredients, SlotDisplay result, SlotDisplay craftingStation) implements RecipeDisplay {

    public static ShapedCraftingRecipeDisplay read(FriendlyByteBuf buffer) {
        int width = buffer.readVarInt();
        int height = buffer.readVarInt();
        List<SlotDisplay> ingredients = buffer.readCollection(ArrayList::new, SlotDisplay::read);
        SlotDisplay result = SlotDisplay.read(buffer);
        SlotDisplay craftingStation = SlotDisplay.read(buffer);
        return new ShapedCraftingRecipeDisplay(width, height, ingredients, result, craftingStation);
    }

    @Override
    public void applyClientboundData(Player player) {
        for (SlotDisplay ingredient : this.ingredients) {
            ingredient.applyClientboundData(player);
        }
        this.result.applyClientboundData(player);
        this.craftingStation.applyClientboundData(player);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeVarInt(1);
        buf.writeVarInt(this.width);
        buf.writeVarInt(this.height);
        buf.writeCollection(this.ingredients, (byteBuf, slotDisplay) -> slotDisplay.write(buf));
        this.result.write(buf);
        this.craftingStation.write(buf);
    }

    @Override
    public @NotNull String toString() {
        return "ShapedCraftingRecipeDisplay{" +
                "craftingStation=" + craftingStation +
                ", width=" + width +
                ", height=" + height +
                ", ingredients=" + ingredients +
                ", result=" + result +
                '}';
    }
}
