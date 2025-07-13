package net.momirealms.craftengine.core.item.recipe.input;

import net.momirealms.craftengine.core.item.recipe.UniqueIdItem;

public record SingleItemInput<T>(UniqueIdItem<T> input) implements RecipeInput {
}
