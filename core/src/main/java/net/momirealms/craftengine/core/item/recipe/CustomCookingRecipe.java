package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.input.SingleItemInput;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

public abstract class CustomCookingRecipe extends AbstractGroupedRecipe implements ConditionalRecipe {
    protected final CookingRecipeCategory category;
    protected final Ingredient ingredient;
    protected final float experience;
    protected final int cookingTime;
    protected final Predicate<Context> craftingCondition;

    protected CustomCookingRecipe(Key id,
                                  boolean showNotification,
                                  CustomRecipeResult result,
                                  String group,
                                  CookingRecipeCategory category,
                                  Ingredient ingredient,
                                  int cookingTime,
                                  float experience,
                                  Predicate<Context> craftingCondition) {
        super(id, showNotification, result, group);
        this.category = category == null ? CookingRecipeCategory.MISC : category;
        this.ingredient = ingredient;
        this.experience = experience;
        this.cookingTime = cookingTime;
        this.craftingCondition = craftingCondition;
    }

    @Override
    public boolean canUse(Context context) {
        if (this.craftingCondition == null) return true;
        return this.craftingCondition.test(context);
    }

    @Override
    public boolean hasCondition() {
        return this.craftingCondition != null;
    }

    @Override
    public boolean matches(RecipeInput input) {
        return this.ingredient.test(((SingleItemInput) input).input());
    }

    @Override
    public void takeInput(@NotNull RecipeInput in, int ignore) {
        SingleItemInput input = (SingleItemInput) in;
        takeIngredient(this.ingredient, input.input().item(), ignore);
    }

    public CookingRecipeCategory category() {
        return this.category;
    }

    public Ingredient ingredient() {
        return this.ingredient;
    }

    public float experience() {
        return this.experience;
    }

    public int cookingTime() {
        return this.cookingTime;
    }

    @Override
    public List<Ingredient> ingredientsInUse() {
        return List.of(this.ingredient);
    }
}
