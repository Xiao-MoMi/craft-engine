package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.plugin.Manageable;
import net.momirealms.craftengine.core.plugin.config.ConfigParser;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public interface RecipeManager extends Manageable {

    ConfigParser parser();

    boolean isDataPackRecipe(Key key);

    boolean isCustomRecipe(Key key);

    Optional<Recipe> recipeById(Key id);

    List<Recipe> recipesByType(RecipeType type);

    List<Recipe> recipeByResult(Key result);

    List<Recipe> recipeByIngredient(Key ingredient);

    @Nullable
    Recipe recipeByInput(RecipeType type, RecipeInput input);

    @Nullable
    Recipe recipeByInput(RecipeType type, RecipeInput input, @Nullable Key lastRecipe);
}
