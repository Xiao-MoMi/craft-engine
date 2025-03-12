package net.momirealms.craftengine.core.item.recipe.vanilla.reader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.recipe.vanilla.RecipeResult;
import org.jetbrains.annotations.NotNull;

public class VanillaRecipeReader1_20_5 extends VanillaRecipeReader1_20 {

    @Override
    protected @NotNull RecipeResult readCraftingResult(JsonObject object) {
        String item = object.get("id").getAsString();
        JsonObject components = object.has("components") ? object.getAsJsonObject("components") : null;
        int count = object.has("count") ? object.get("count").getAsInt() : 1;
        return new RecipeResult(item, count, components);
    }

    @NotNull
    @Override
    protected RecipeResult readCookingResult(JsonElement object) {
        return readCraftingResult(object.getAsJsonObject());
    }

    @NotNull
    @Override
    protected RecipeResult readStoneCuttingResult(JsonObject json) {
        return readCraftingResult(json.getAsJsonObject("result"));
    }
}
