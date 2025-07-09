package net.momirealms.craftengine.core.item.recipe.network;

import net.momirealms.craftengine.core.item.recipe.network.display.RecipeDisplay;
import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.util.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public record RecipeBookDisplayEntry(RecipeDisplayId displayId, RecipeDisplay display, OptionalInt group, int category, Optional<List<Integer>> ingredients) {

    public static RecipeBookDisplayEntry read(FriendlyByteBuf buffer) {
        RecipeDisplayId displayId = RecipeDisplayId.read(buffer);
        RecipeDisplay display = buffer.readById(BuiltInRegistries.RECIPE_DISPLAY_TYPE).read(buffer);
        OptionalInt group = buffer.readOptionalVarInt();
        int category = buffer.readVarInt(); // simplify the registry lookup since we don't care about the category
        Optional<List<Integer>> requirements = buffer.readOptional(buf -> buf.readCollection(ArrayList::new, FriendlyByteBuf::readVarInt)); // simplify the registry lookup since we don't care about the ingredient ids
        return new RecipeBookDisplayEntry(displayId, display, group, category, requirements);
    }

    public void write(FriendlyByteBuf buffer) {
        this.displayId.write(buffer);
        this.display.write(buffer);
        buffer.writeOptionalVarInt(this.group);
        buffer.writeVarInt(this.category);
        buffer.writeOptional(this.ingredients, (buf, recipeIngredients) -> buf.writeCollection(recipeIngredients, FriendlyByteBuf::writeVarInt));
    }
}
