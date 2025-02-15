package net.momirealms.craftengine.core.item.recipe.input;

import net.momirealms.craftengine.core.item.recipe.OptimizedIDItem;

public class CookingInput<T> implements RecipeInput {
    private final OptimizedIDItem<T> input;

    public CookingInput(OptimizedIDItem<T> input) {
        this.input = input;
    }

    public OptimizedIDItem<T> input() {
        return input;
    }
}
