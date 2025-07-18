package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;

public interface FixedResultRecipe<T> extends Recipe<T> {

    T result(ItemBuildContext context);

    CustomRecipeResult<T> result();

    @Override
    default T assemble(RecipeInput input, ItemBuildContext context) {
        return this.result(context);
    }
}
