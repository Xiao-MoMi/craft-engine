package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.plugin.Reloadable;
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface RecipeManager<T> extends Reloadable, ConfigSectionParser {
    String CONFIG_SECTION_NAME = "recipes";

    default String sectionId() {
        return CONFIG_SECTION_NAME;
    }

    boolean isDataPackRecipe(Key key);

    boolean isCustomRecipe(Key key);

    List<Recipe<T>> getRecipes(Key type);

    @Nullable
    Recipe<T> getRecipe(Key type, RecipeInput input);

    void delayedLoad();

    default int loadingSequence() {
        return LoadingSequence.RECIPE;
    }
}
