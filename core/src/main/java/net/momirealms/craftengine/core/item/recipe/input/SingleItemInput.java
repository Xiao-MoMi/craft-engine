package net.momirealms.craftengine.core.item.recipe.input;

import net.momirealms.craftengine.core.item.recipe.OptimizedIDItem;

public record SingleItemInput<T>(OptimizedIDItem<T> input) implements RecipeInput {
}
