package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.util.Key;

public abstract class AbstractFixedResultRecipe extends AbstractRecipe implements FixedResultRecipe {
    protected CustomRecipeResult result;

    public AbstractFixedResultRecipe(Key id, boolean showNotification, CustomRecipeResult result) {
        super(id, showNotification);
        this.result = result;
    }

    @Override
    public CustomRecipeResult result() {
        return this.result;
    }

    @Override
    public Item assemble(RecipeInput input, ItemBuildContext context) {
        return this.result(context);
    }
}
