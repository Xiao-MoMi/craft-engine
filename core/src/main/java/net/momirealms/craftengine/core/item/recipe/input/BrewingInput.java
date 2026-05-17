package net.momirealms.craftengine.core.item.recipe.input;

import net.momirealms.craftengine.core.item.recipe.UniqueIdItem;

public final class BrewingInput<T> implements RecipeInput {
    private final UniqueIdItem container;
    private final UniqueIdItem ingredient;

    public BrewingInput(UniqueIdItem container, UniqueIdItem ingredient) {
        this.container = container;
        this.ingredient = ingredient;
    }

    public UniqueIdItem container() {
        return this.container;
    }

    public UniqueIdItem ingredient() {
        return this.ingredient;
    }
}
