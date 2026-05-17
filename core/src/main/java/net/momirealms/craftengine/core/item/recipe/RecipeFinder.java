package net.momirealms.craftengine.core.item.recipe;

import java.util.List;

public final class RecipeFinder {
    private final StackedContents<UniqueIdItem> stackedContents = new StackedContents<>();

    public void addInput(UniqueIdItem item) {
        if (!item.isEmpty()) {
            this.stackedContents.add(item, 1);
        }
    }

    public boolean canCraft(CustomShapelessRecipe recipe) {
        PlacementInfo placementInfo = recipe.placementInfo();
        return !placementInfo.isImpossibleToPlace() && canCraft(placementInfo.ingredients());
    }

    private boolean canCraft(List<? extends StackedContents.IngredientInfo<UniqueIdItem>> rawIngredients) {
        return this.stackedContents.tryPick(rawIngredients);
    }
}
