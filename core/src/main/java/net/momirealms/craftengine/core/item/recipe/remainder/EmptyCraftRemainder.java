package net.momirealms.craftengine.core.item.recipe.remainder;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;

public class EmptyCraftRemainder implements CraftRemainder {
    public static final EmptyCraftRemainder INSTANCE = new EmptyCraftRemainder();

    @Override
    public Item remainder(Key recipeId, Item item) {
        return null;
    }
}
