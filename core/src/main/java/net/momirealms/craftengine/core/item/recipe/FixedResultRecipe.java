package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;

public interface FixedResultRecipe extends Recipe {

    CustomRecipeResult result();

    default Item result(ItemBuildContext context) {
        return this.result().buildItem(context);
    }
}
