/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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

    void delayedInit();

    default int loadingSequence() {
        return LoadingSequence.RECIPE;
    }
}
