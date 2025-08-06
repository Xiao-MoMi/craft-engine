package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractGroupedRecipe<T> extends AbstractedFixedResultRecipe<T> {
    protected final String group;

    protected AbstractGroupedRecipe(Key id, boolean showNotification, CustomRecipeResult<T> result, String group) {
        super(id, showNotification, result);
        this.group = group == null ? "" : group;
    }

    @Nullable
    public String group() {
        return group;
    }
}
