package net.momirealms.craftengine.core.item.recipe;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

public class PlacementInfo<T> {
    private final List<Ingredient<T>> ingredients;
    private final IntList slotsToIngredientIndex;

    private PlacementInfo(List<Ingredient<T>> ingredients, IntList placementSlots) {
        this.ingredients = ingredients;
        this.slotsToIngredientIndex = placementSlots;
    }

    public static <T> PlacementInfo<T> create(List<Ingredient<T>> ingredients) {
        int i = ingredients.size();
        IntList intList = new IntArrayList(i);
        for (int j = 0; j < i; j++) {
            Ingredient<T> ingredient = ingredients.get(j);
            if (ingredient.isEmpty()) {
                return new PlacementInfo<>(List.of(), IntList.of());
            }
            intList.add(j);
        }
        return new PlacementInfo<>(ingredients, intList);
    }

    public List<Ingredient<T>> ingredients() {
        return this.ingredients;
    }

    public boolean isImpossibleToPlace() {
        return this.slotsToIngredientIndex.isEmpty();
    }
}
