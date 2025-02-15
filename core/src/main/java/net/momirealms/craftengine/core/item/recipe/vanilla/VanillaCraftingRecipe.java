package net.momirealms.craftengine.core.item.recipe.vanilla;

import net.momirealms.craftengine.core.item.recipe.CraftingRecipeCategory;

public class VanillaCraftingRecipe extends VanillaRecipe {
    protected final CraftingRecipeCategory category;

    protected VanillaCraftingRecipe(CraftingRecipeCategory category, String group, RecipeResult result) {
        super(group, result);
        this.category = category;
    }

    public CraftingRecipeCategory category() {
        return category;
    }
}
