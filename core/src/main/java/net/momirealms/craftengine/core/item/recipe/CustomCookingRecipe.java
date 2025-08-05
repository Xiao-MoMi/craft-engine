package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.input.SingleItemInput;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;

public abstract class CustomCookingRecipe<T> extends AbstractGroupedRecipe<T> {
    protected final CookingRecipeCategory category;
    protected final Ingredient<T> ingredient;
    protected final float experience;
    protected final int cookingTime;

    protected CustomCookingRecipe(Key id,
                                  CookingRecipeCategory category,
                                  String group,
                                  Ingredient<T> ingredient,
                                  int cookingTime,
                                  float experience,
                                  CustomRecipeResult<T> result) {
        super(id, group, result);
        this.category = category == null ? CookingRecipeCategory.MISC : category;
        this.ingredient = ingredient;
        this.experience = experience;
        this.cookingTime = cookingTime;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(RecipeInput input) {
        return this.ingredient.test(((SingleItemInput<T>) input).input());
    }

    public CookingRecipeCategory category() {
        return category;
    }

    public Ingredient<T> ingredient() {
        return ingredient;
    }

    public CustomRecipeResult<T> result() {
        return result;
    }

    public float experience() {
        return experience;
    }

    public int cookingTime() {
        return cookingTime;
    }

    @Override
    public List<Ingredient<T>> ingredientsInUse() {
        return List.of(ingredient);
    }
}
