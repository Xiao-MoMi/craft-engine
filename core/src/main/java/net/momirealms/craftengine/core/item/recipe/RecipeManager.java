package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.plugin.Reloadable;
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface RecipeManager<T> extends Reloadable, ConfigSectionParser {
    String CONFIG_SECTION_NAME = "recipes";

    default String sectionId() {
        return CONFIG_SECTION_NAME;
    }

    boolean isDataPackRecipe(Key key);

    boolean isCustomRecipe(Key key);

    Optional<Recipe<T>> getRecipeById(Key id);

    List<Recipe<T>> getRecipes(Key type);

    List<Recipe<T>> getRecipeByResult(Key result);

    @Nullable
    Recipe<T> getRecipe(Key type, RecipeInput input);

    @Nullable Recipe<T> getRecipe(Key type, RecipeInput input, @Nullable Key lastRecipe);

    CompletableFuture<Void> delayedLoad();

    default int loadingSequence() {
        return LoadingSequence.RECIPE;
    }
}
