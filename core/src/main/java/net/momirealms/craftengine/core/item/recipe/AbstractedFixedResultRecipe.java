package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.util.Key;

public abstract class AbstractedFixedResultRecipe<T> extends AbstractRecipe<T> {
    protected CustomRecipeResult<T> result;

    public AbstractedFixedResultRecipe(Key id, boolean showNotification, CustomRecipeResult<T> result) {
        super(id, showNotification);
        this.result = result;
    }

    public T result(ItemBuildContext context) {
        return this.result.buildItemStack(context);
    }

    public CustomRecipeResult<T> result() {
        return this.result;
    }

    @Override
    public T assemble(RecipeInput input, ItemBuildContext context) {
        return this.result(context);
    }
}
