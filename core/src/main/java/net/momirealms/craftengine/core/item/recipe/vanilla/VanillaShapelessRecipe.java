package net.momirealms.craftengine.core.item.recipe.vanilla;

import net.momirealms.craftengine.core.item.recipe.CraftingRecipeCategory;

import java.util.List;

public class VanillaShapelessRecipe extends VanillaCraftingRecipe {
    private final List<List<String>> ingredients;

    public VanillaShapelessRecipe(CraftingRecipeCategory category, String group, List<List<String>> ingredients, RecipeResult result) {
        super(category, group, result);
        this.ingredients = ingredients;
    }

    public List<List<String>> ingredients() {
        return ingredients;
    }
}
