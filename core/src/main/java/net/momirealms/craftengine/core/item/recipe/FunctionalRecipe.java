package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.function.Function;

public interface FunctionalRecipe extends Recipe {

    default boolean hasFunctions() {
        Function<Context>[] functions = functions();
        return functions != null && functions.length > 0;
    }

    Function<Context>[] functions();
}
