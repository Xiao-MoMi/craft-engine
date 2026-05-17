package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.plugin.context.Context;

public interface ConditionalRecipe extends Recipe {

    boolean canUse(final Context context);

    boolean hasCondition();
}
