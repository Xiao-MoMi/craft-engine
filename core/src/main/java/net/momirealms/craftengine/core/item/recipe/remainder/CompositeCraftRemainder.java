package net.momirealms.craftengine.core.item.recipe.remainder;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;

public final class CompositeCraftRemainder implements CraftRemainder {
    private final CraftRemainder[] remainders;

    public CompositeCraftRemainder(CraftRemainder[] remainders) {
        this.remainders = remainders;
    }

    @Override
    public Item remainder(Key recipeId, Item item) {
        for (CraftRemainder remainder : this.remainders) {
            item = remainder.remainder(recipeId, item);
        }
        return item;
    }
}
