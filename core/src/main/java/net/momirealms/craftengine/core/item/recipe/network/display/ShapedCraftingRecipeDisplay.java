package net.momirealms.craftengine.core.item.recipe.network.display;

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;

public record ShapedCraftingRecipeDisplay(int width, int height, List<SlotDisplay> ingredients, SlotDisplay result, SlotDisplay craftingStation) implements RecipeDisplay {

    public static ShapedCraftingRecipeDisplay read(FriendlyByteBuf buffer) {
        int width = buffer.readVarInt();
        int height = buffer.readVarInt();

        List<SlotDisplay> ingredients = buffer.readCollection(ArrayList::new, friendlyByteBuf -> friendlyByteBuf.readById(BuiltInRegistries.SLOT_DISPLAY));
        return null;
    }

    @Override
    public void write(FriendlyByteBuf buf) {

    }
}
