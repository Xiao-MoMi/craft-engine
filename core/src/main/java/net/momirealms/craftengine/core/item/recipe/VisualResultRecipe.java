package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;

public interface VisualResultRecipe extends FixedResultRecipe {

    CustomRecipeResult visualResult();

    default boolean hasVisualResult() {
        return visualResult() != null;
    }

    default Item assembleVisual(RecipeInput input, ItemBuildContext context) {
        CustomRecipeResult result = visualResult();
        if (result != null) {
            return result.buildItem(context);
        }
        return null;
    }

    default Item buildVisualOrActualResult(ItemBuildContext context) {
        CustomRecipeResult visualResult = visualResult();
        if (visualResult != null) {
            return visualResult.buildItem(context);
        } else {
            return this.result().buildItem(context);
        }
    }
}
