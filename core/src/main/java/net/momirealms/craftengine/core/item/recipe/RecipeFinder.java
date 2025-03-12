package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;

public class RecipeFinder {
    private final StackedContents<Holder<Key>> stackedContents = new StackedContents<>();

    public <T> void addInput(OptimizedIDItem<T> item) {
        if (!item.isEmpty()) {
            this.stackedContents.add(item.id(), 1);
        }
    }

    public <T> boolean canCraft(CustomShapelessRecipe<T> recipe) {
        PlacementInfo<T> placementInfo = recipe.placementInfo();
        return !placementInfo.isImpossibleToPlace() && canCraft(placementInfo.ingredients());
    }

    private boolean canCraft(List<? extends StackedContents.IngredientInfo<Holder<Key>>> rawIngredients) {
        return this.stackedContents.tryPick(rawIngredients);
    }
}
