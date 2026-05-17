package net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display.slot.SlotDisplay;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public record ShapelessCraftingRecipeDisplay(List<SlotDisplay> ingredients, SlotDisplay result, SlotDisplay craftingStation) implements RecipeDisplay {

    public static ShapelessCraftingRecipeDisplay read(FriendlyByteBuf buffer, FriendlyByteBuf.Reader<Item> reader) {
        List<SlotDisplay> ingredients = buffer.readCollection(ArrayList::new, buf -> SlotDisplay.read(buf, reader));
        SlotDisplay result = SlotDisplay.read(buffer, reader);
        SlotDisplay craftingStation = SlotDisplay.read(buffer, reader);
        return new ShapelessCraftingRecipeDisplay(ingredients, result, craftingStation);
    }

    @Override
    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeVarInt(0);
        buf.writeCollection(this.ingredients, (byteBuf, slotDisplay) -> slotDisplay.write(buf, writer));
        this.result.write(buf, writer);
        this.craftingStation.write(buf, writer);
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        for (SlotDisplay ingredient : ingredients) {
            ingredient.applyClientboundData(function);
        }
        this.result.applyClientboundData(function);
        this.craftingStation.applyClientboundData(function);
    }

    @Override
    public @NotNull String toString() {
        return "ShapelessCraftingRecipeDisplay{" +
                "craftingStation=" + this.craftingStation +
                ", ingredients=" + this.ingredients +
                ", result=" + this.result +
                '}';
    }
}
