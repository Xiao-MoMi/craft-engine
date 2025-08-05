package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;

public interface FixedResultRecipe<T> extends Recipe<T> {

    T result(ItemBuildContext context);

    CustomRecipeResult<T> result();



    @Override
    default T assemble(RecipeInput input, ItemBuildContext context) {
        return this.result(context);
    }
}
