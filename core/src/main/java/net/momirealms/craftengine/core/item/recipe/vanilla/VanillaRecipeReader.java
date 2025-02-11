package net.momirealms.craftengine.core.item.recipe.vanilla;

import com.google.gson.JsonObject;

public interface VanillaRecipeReader {

    VanillaShapedRecipe readShaped(JsonObject json);

    VanillaShapelessRecipe readShapeless(JsonObject json);
}
