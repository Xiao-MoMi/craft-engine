package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.input.SingleItemInput;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.UniqueKey;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class CustomStoneCuttingRecipe<T> extends AbstractGroupedRecipe<T> {
    public static final Factory<?> FACTORY = new Factory<>();
    protected final Ingredient<T> ingredient;

    public CustomStoneCuttingRecipe(Key id, String group, Ingredient<T> ingredient, CustomRecipeResult<T> result) {
        super(id, group, result);
        this.ingredient = ingredient;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(RecipeInput input) {
        return this.ingredient.test(((SingleItemInput<T>) input).input());
    }

    @Override
    public List<Ingredient<T>> ingredientsInUse() {
        return List.of(ingredient);
    }

    @Override
    public @NotNull Key type() {
        return RecipeTypes.STONECUTTING;
    }

    public Ingredient<T> ingredient() {
        return ingredient;
    }

    public static class Factory<A> extends AbstractRecipeFactory<A> {

        @SuppressWarnings({"DuplicatedCode"})
        @Override
        public Recipe<A> create(Key id, Map<String, Object> arguments) {
            String group = arguments.containsKey("group") ? arguments.get("group").toString() : null;
            Set<UniqueKey> holders = ingredientHolders(arguments);
            return new CustomStoneCuttingRecipe<>(id, group, Ingredient.of(holders), parseResult(arguments));
        }
    }
}
