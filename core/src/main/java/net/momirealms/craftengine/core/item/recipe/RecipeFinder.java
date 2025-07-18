package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.util.UniqueKey;

import java.util.List;

public class RecipeFinder {
    private final StackedContents<UniqueKey> stackedContents = new StackedContents<>();

    public <T> void addInput(UniqueIdItem<T> item) {
        if (!item.isEmpty()) {
            this.stackedContents.add(item.id(), 1);
        }
    }

    public <T> boolean canCraft(CustomShapelessRecipe<T> recipe) {
        PlacementInfo<T> placementInfo = recipe.placementInfo();
        return !placementInfo.isImpossibleToPlace() && canCraft(placementInfo.ingredients());
    }

    private boolean canCraft(List<? extends StackedContents.IngredientInfo<UniqueKey>> rawIngredients) {
        return this.stackedContents.tryPick(rawIngredients);
    }
}
