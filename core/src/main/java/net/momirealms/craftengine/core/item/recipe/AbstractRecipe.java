package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.util.Key;

public abstract class AbstractRecipe<T> implements Recipe<T> {
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
}
