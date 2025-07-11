package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.UniqueKey;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class CustomSmeltingRecipe<T> extends CustomCookingRecipe<T> {
    public static final Factory<?> FACTORY = new Factory<>();

    public CustomSmeltingRecipe(Key id, CookingRecipeCategory category, String group, Ingredient<T> ingredient, int cookingTime, float experience, CustomRecipeResult<T> result) {
        super(id, category, group, ingredient, cookingTime, experience, result);
    }

    @Override
    public @NotNull Key type() {
        return RecipeTypes.SMELTING;
    }

    public static class Factory<A> extends AbstractRecipeFactory<A> {

        @SuppressWarnings({"unchecked", "rawtypes", "DuplicatedCode"})
        @Override
        public Recipe<A> create(Key id, Map<String, Object> arguments) {
            String group = arguments.containsKey("group") ? arguments.get("group").toString() : null;
            int cookingTime = ResourceConfigUtils.getAsInt(arguments.getOrDefault("time", 80), "time");
            float experience = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("experience", 0.0f), "experience");
            Set<UniqueKey> holders = ingredientHolders(arguments);
            return new CustomSmeltingRecipe(id, cookingRecipeCategory(arguments), group, Ingredient.of(holders), cookingTime, experience, parseResult(arguments));
        }
    }
}
