package net.momirealms.craftengine.core.item.recipe.vanilla;

import com.google.gson.JsonObject;

public interface VanillaRecipeReader {

    VanillaShapedRecipe readShaped(JsonObject json);

    VanillaShapelessRecipe readShapeless(JsonObject json);

    VanillaBlastingRecipe readBlasting(JsonObject json);

    VanillaSmeltingRecipe readSmelting(JsonObject json);

    VanillaSmokingRecipe readSmoking(JsonObject json);

    VanillaCampfireRecipe readCampfire(JsonObject json);

    VanillaStoneCuttingRecipe readStoneCutting(JsonObject json);

    VanillaSmithingTransformRecipe readSmithingTransform(JsonObject json);

    VanillaSmithingTrimRecipe readSmithingTrim(JsonObject json);
}
