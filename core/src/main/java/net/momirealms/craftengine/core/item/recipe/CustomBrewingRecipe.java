package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.recipe.input.BrewingInput;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomBrewingRecipe<T> implements FixedResultRecipe<T> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final Key id;
    private final Ingredient<T> container;
    private final Ingredient<T> ingredient;
    private final CustomRecipeResult<T> result;

    public CustomBrewingRecipe(@NotNull Key id,
                               @NotNull Ingredient<T> container,
                               @NotNull Ingredient<T> ingredient,
                               @NotNull CustomRecipeResult<T> result) {
        this.id = id;
        this.container = container;
        this.ingredient = ingredient;
        this.result = result;
    }

    @Override
    public CustomRecipeResult<T> result() {
        return this.result;
    }

    @Override
    public T result(ItemBuildContext context) {
        return this.result.buildItemStack(context);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(RecipeInput input) {
        BrewingInput<T> brewingInput = (BrewingInput<T>) input;
        return this.container.test(brewingInput.container()) && this.ingredient.test(brewingInput.ingredient());
    }

    @Override
    public T assemble(RecipeInput input, ItemBuildContext context) {
        return this.result(context);
    }

    @Override
    public List<Ingredient<T>> ingredientsInUse() {
        List<Ingredient<T>> ingredients = new ArrayList<>();
        ingredients.add(this.container);
        ingredients.add(this.ingredient);
        return ingredients;
    }

    @Override
    public @NotNull Key type() {
        return RecipeTypes.BREWING;
    }

    @NotNull
    public Ingredient<T> container() {
        return this.container;
    }

    @NotNull
    public Ingredient<T> ingredient() {
        return this.ingredient;
    }

    @Override
    public Key id() {
        return this.id;
    }

    @SuppressWarnings({"DuplicatedCode"})
    public static class Factory<A> implements RecipeFactory<A> {

        @Override
        public Recipe<A> create(Key id, Map<String, Object> arguments) {
            List<String> container = MiscUtils.getAsStringList(arguments.get("container"));
            if (container.isEmpty()) {
                throw new LocalizedResourceConfigException("warning.config.recipe.brewing.missing_container");
            }
            List<String> ingredient = MiscUtils.getAsStringList(arguments.get("ingredient"));
            if (ingredient.isEmpty()) {
                throw new LocalizedResourceConfigException("warning.config.recipe.brewing.missing_ingredient");
            }
            return new CustomBrewingRecipe<>(id,
                    ResourceConfigUtils.requireNonNullOrThrow(toIngredient(container), "warning.config.recipe.brewing.missing_container"),
                    ResourceConfigUtils.requireNonNullOrThrow(toIngredient(ingredient), "warning.config.recipe.brewing.missing_ingredient"),
                    parseResult(arguments));
        }
    }
}
