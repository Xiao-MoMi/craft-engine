package net.momirealms.craftengine.core.item.recipe.vanilla;

import net.momirealms.craftengine.core.item.recipe.CookingRecipeCategory;

import java.util.List;

public abstract class VanillaCookingRecipe extends VanillaRecipe {
    protected final List<String> ingredient;
    protected final CookingRecipeCategory category;
    protected final float experience;
    protected final int cookingTime;

    protected VanillaCookingRecipe(CookingRecipeCategory category, String group, RecipeResult result, List<String> ingredient, float experience, int cookingTime) {
        super(group, result);
        this.ingredient = ingredient;
        this.experience = experience;
        this.cookingTime = cookingTime;
        this.category = category;
    }

    public CookingRecipeCategory category() {
        return category;
    }

    public List<String> ingredient() {
        return ingredient;
    }

    public float experience() {
        return experience;
    }

    public int cookingTime() {
        return cookingTime;
    }
}
