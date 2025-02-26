package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface Recipe<T> {

    boolean matches(RecipeInput input);

    T getResult(ItemBuildContext context);

    @NotNull
    Key type();

    Key id();

    @Nullable
    String group();
}
