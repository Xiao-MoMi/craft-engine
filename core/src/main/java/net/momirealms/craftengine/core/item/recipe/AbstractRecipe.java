package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractRecipe<T> implements Recipe<T> {
    protected final String group;
    protected final Key id;
    protected final CustomRecipeResult<T> result;

    protected AbstractRecipe(Key id, String group, CustomRecipeResult<T> result) {
        this.group = group;
        this.id = id;
        this.result = result;
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

    @Override
    public T result(ItemBuildContext context) {
        return result.buildItemStack(context);
    }

    @Override
    public CustomRecipeResult<T> result() {
        return this.result;
    }
}
