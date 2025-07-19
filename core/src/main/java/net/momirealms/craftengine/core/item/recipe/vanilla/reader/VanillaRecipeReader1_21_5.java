package net.momirealms.craftengine.core.item.recipe.vanilla.reader;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.recipe.vanilla.VanillaSmithingTrimRecipe;

public class VanillaRecipeReader1_21_5 extends VanillaRecipeReader1_21_2 {

    @Override
    public VanillaSmithingTrimRecipe readSmithingTrim(JsonObject json) {
        return new VanillaSmithingTrimRecipe(
                readSingleIngredient(json.get("base")),
                readSingleIngredient(json.get("template")),
                readSingleIngredient(json.get("addition")),
                json.get("pattern").getAsString()
        );
    }
}
