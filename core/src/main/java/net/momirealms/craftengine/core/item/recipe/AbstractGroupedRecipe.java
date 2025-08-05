package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractGroupedRecipe<T> implements FixedResultRecipe<T> {
    protected final String group;
    protected final Key id;
    protected final CustomRecipeResult<T> result;

    protected AbstractGroupedRecipe(Key id, String group, CustomRecipeResult<T> result) {
        this.group = group == null ? "" : group;
        this.id = id;
        this.result = result;
    }

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
