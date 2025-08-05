package net.momirealms.craftengine.bukkit.plugin.injector;

import net.momirealms.craftengine.core.item.recipe.RecipeType;
import net.momirealms.craftengine.core.util.Key;

public interface InjectedCacheCheck {

    Object recipeType();

    void recipeType(Object recipeType);

    RecipeType customRecipeType();

    void customRecipeType(RecipeType customRecipeType);

    Object lastRecipe();

    void lastRecipe(Object lastRecipe);

    Key lastCustomRecipe();

    void lastCustomRecipe(Key lastCustomRecipe);
}
