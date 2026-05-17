package net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.network.protocol.recipe.modern.display.slot.SlotDisplay;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public record ShapedCraftingRecipeDisplay(int width, int height, List<SlotDisplay> ingredients, SlotDisplay result, SlotDisplay craftingStation) implements RecipeDisplay {

    public static ShapedCraftingRecipeDisplay read(FriendlyByteBuf buffer, FriendlyByteBuf.Reader<Item> reader) {
        int width = buffer.readVarInt();
        int height = buffer.readVarInt();
        List<SlotDisplay> ingredients = buffer.readCollection(ArrayList::new, buf -> SlotDisplay.read(buf, reader));
        SlotDisplay result = SlotDisplay.read(buffer, reader);
        SlotDisplay craftingStation = SlotDisplay.read(buffer, reader);
        return new ShapedCraftingRecipeDisplay(width, height, ingredients, result, craftingStation);
    }

    @Override
    public void write(FriendlyByteBuf buf, FriendlyByteBuf.Writer<Item> writer) {
        buf.writeVarInt(1);
        buf.writeVarInt(this.width);
        buf.writeVarInt(this.height);
        buf.writeCollection(this.ingredients, (byteBuf, slotDisplay) -> slotDisplay.write(buf, writer));
        this.result.write(buf, writer);
        this.craftingStation.write(buf, writer);
    }

    @Override
    public void applyClientboundData(Function<Item, Item> function) {
        for (SlotDisplay ingredient : this.ingredients) {
            ingredient.applyClientboundData(function);
        }
        this.result.applyClientboundData(function);
        this.craftingStation.applyClientboundData(function);
    }

    @Override
    public @NotNull String toString() {
        return "ShapedCraftingRecipeDisplay{" +
                "craftingStation=" + this.craftingStation +
                ", width=" + this.width +
                ", height=" + this.height +
                ", ingredients=" + this.ingredients +
                ", result=" + this.result +
                '}';
    }
}
