package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Recipe<T> {

    boolean matches(RecipeInput input);

    T assemble(RecipeInput input, ItemBuildContext context);

    List<Ingredient<T>> ingredientsInUse();

    @NotNull
    Key serializerType();

    RecipeType type();

    Key id();

    default boolean showNotification() {
        return true;
    }
}
