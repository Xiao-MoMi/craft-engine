package net.momirealms.craftengine.core.item.recipe.input;

import net.momirealms.craftengine.core.item.recipe.UniqueIdItem;

public final class BrewingInput<T> implements RecipeInput {
    private final UniqueIdItem<T> container;
    private final UniqueIdItem<T> ingredient;

    public BrewingInput(UniqueIdItem<T> container, UniqueIdItem<T> ingredient) {
        this.container = container;
        this.ingredient = ingredient;
    }

    public UniqueIdItem<T> container() {
        return container;
    }

    public UniqueIdItem<T> ingredient() {
        return ingredient;
    }
}
