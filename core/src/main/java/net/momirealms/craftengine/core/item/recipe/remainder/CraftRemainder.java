package net.momirealms.craftengine.core.item.recipe.remainder;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;

public interface CraftRemainder {

    Item remainder(Key recipeId, Item item);
}
