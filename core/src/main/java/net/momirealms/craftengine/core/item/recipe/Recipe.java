package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Recipe {

    boolean matches(RecipeInput input);

    void takeInput(@NotNull final RecipeInput input, int ignore);

    default void takeInput(@NotNull final RecipeInput input) {
        this.takeInput(input, 0);
    }

    Item assemble(RecipeInput input, ItemBuildContext context);

    List<Ingredient> ingredientsInUse();

    @NotNull
    Key serializerType();

    RecipeType type();

    Key id();

    default boolean showNotification() {
        return true;
    }

    default boolean canBeSearched() {
        return true;
    }
}
