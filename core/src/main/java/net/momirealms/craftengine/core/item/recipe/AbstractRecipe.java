package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractRecipe<T> implements Recipe<T> {
    protected String group;
    protected Key id;

    protected AbstractRecipe(Key id, String group) {
        this.group = group;
        this.id = id;
    }

    @Override
    @Nullable
    public String group() {
        return group;
    }

    @Override
    public Key id() {
        return id;
    }
}
