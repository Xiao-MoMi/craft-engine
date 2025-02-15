package net.momirealms.craftengine.core.item.recipe.vanilla;

import net.momirealms.craftengine.core.item.recipe.CookingRecipeCategory;

import java.util.List;

public class VanillaCampfireRecipe extends VanillaCookingRecipe {

    public VanillaCampfireRecipe(CookingRecipeCategory category, String group, RecipeResult result, List<String> ingredient, float experience, int cookingTime) {
        super(category, group, result, ingredient, experience, cookingTime);
    }
}
