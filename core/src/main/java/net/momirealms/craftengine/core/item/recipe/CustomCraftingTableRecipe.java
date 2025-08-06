package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.util.Key;

public abstract class CustomCraftingTableRecipe<T> extends AbstractGroupedRecipe<T> {
    protected final CraftingRecipeCategory category;

    protected CustomCraftingTableRecipe(Key id, boolean showNotification, CustomRecipeResult<T> result, String group, CraftingRecipeCategory category) {
        super(id, showNotification, result, group);
        this.category = category == null ? CraftingRecipeCategory.MISC : category;
    }

    public CraftingRecipeCategory category() {
        return category;
    }

    @Override
    public RecipeType type() {
        return RecipeType.CRAFTING;
    }
}
