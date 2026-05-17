package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractRecipe implements Recipe {
    protected final Key id;
    protected final boolean showNotification;

    public AbstractRecipe(Key id, boolean showNotification) {
        this.id = id;
        this.showNotification = showNotification;
    }

    @Override
    public boolean showNotification() {
        return this.showNotification;
    }

    @Override
    public Key id() {
        return this.id;
    }

    protected void takeIngredient(@NotNull Ingredient ingredient, Item item, int ignore) {
        int i = ingredient.count() - ignore;
        if (i > 0) {
            item.shrink(i);
        }
    }
}
