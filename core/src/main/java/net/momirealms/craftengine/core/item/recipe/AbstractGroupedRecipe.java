package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractGroupedRecipe extends AbstractFixedResultRecipe {
    protected final String group;

    protected AbstractGroupedRecipe(Key id, boolean showNotification, CustomRecipeResult result, String group) {
        super(id, showNotification, result);
        this.group = group == null ? "" : group;
    }

    @Nullable
    public String group() {
        return this.group;
    }
}
