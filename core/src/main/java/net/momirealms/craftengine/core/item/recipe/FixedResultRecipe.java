package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.ItemBuildContext;

public interface FixedResultRecipe<T> extends Recipe<T> {

    T result(ItemBuildContext context);

    CustomRecipeResult<T> result();
}
