package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.util.Key;

public abstract class CraftingTableRecipe<T> extends AbstractRecipe<T> {
    protected final CraftingRecipeCategory category;

    protected CraftingTableRecipe(Key id, CraftingRecipeCategory category, String group) {
        super(id, group);
        this.category = category;
    }

    public CraftingRecipeCategory category() {
        return category;
    }
}
