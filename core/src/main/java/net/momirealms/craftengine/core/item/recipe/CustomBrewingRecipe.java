package net.momirealms.craftengine.core.item.recipe;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.recipe.input.BrewingInput;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomBrewingRecipe<T> extends AbstractedFixedResultRecipe<T> {
    public static final Serializer<?> SERIALIZER = new Serializer<>();
    private final Ingredient<T> container;
    private final Ingredient<T> ingredient;

    public CustomBrewingRecipe(@NotNull Key id,
                               @NotNull Ingredient<T> container,
                               @NotNull Ingredient<T> ingredient,
                               @NotNull CustomRecipeResult<T> result,
                               boolean showNotification) {
        super(id, showNotification, result);
        this.container = container;
        this.ingredient = ingredient;
        this.result = result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(RecipeInput input) {
        BrewingInput<T> brewingInput = (BrewingInput<T>) input;
        return this.container.test(brewingInput.container()) && this.ingredient.test(brewingInput.ingredient());
    }

    @Override
    public List<Ingredient<T>> ingredientsInUse() {
        List<Ingredient<T>> ingredients = new ArrayList<>();
        ingredients.add(this.container);
        ingredients.add(this.ingredient);
        return ingredients;
    }

    @Override
    public @NotNull Key serializerType() {
        return RecipeSerializers.BREWING;
    }

    @Override
    public RecipeType type() {
        return RecipeType.BREWING;
    }

    @NotNull
    public Ingredient<T> container() {
        return this.container;
    }

    @NotNull
    public Ingredient<T> ingredient() {
        return this.ingredient;
    }

    @SuppressWarnings({"DuplicatedCode"})
    public static class Serializer<A> extends AbstractRecipeSerializer<A, CustomBrewingRecipe<A>> {

        @Override
        public CustomBrewingRecipe<A> readMap(Key id, Map<String, Object> arguments) {
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
                    parseResult(arguments),
                    showNotification(arguments)
            );
        }

        @Override
        public CustomBrewingRecipe<A> readJson(Key id, JsonObject json) {
            throw new UnsupportedOperationException();
        }
    }
}
