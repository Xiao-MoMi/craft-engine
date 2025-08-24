package net.momirealms.craftengine.core.item.recipe;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.input.SingleItemInput;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class CustomStoneCuttingRecipe<T> extends AbstractGroupedRecipe<T> {
    public static final Serializer<?> SERIALIZER = new Serializer<>();
    protected final Ingredient<T> ingredient;

    public CustomStoneCuttingRecipe(Key id, boolean showNotification, CustomRecipeResult<T> result, String group, Ingredient<T> ingredient) {
        super(id, showNotification, result, group);
        this.ingredient = ingredient;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(RecipeInput input) {
        return this.ingredient.test(((SingleItemInput<T>) input).input());
    }

    @Override
    public List<Ingredient<T>> ingredientsInUse() {
        return List.of(this.ingredient);
    }

    @Override
    public @NotNull Key serializerType() {
        return RecipeSerializers.STONECUTTING;
    }

    @Override
    public RecipeType type() {
        return RecipeType.STONECUTTING;
    }

    public Ingredient<T> ingredient() {
        return this.ingredient;
    }

    public static class Serializer<A> extends AbstractRecipeSerializer<A, CustomStoneCuttingRecipe<A>> {

        @SuppressWarnings({"DuplicatedCode"})
        @Override
        public CustomStoneCuttingRecipe<A> readMap(Key id, Map<String, Object> arguments) {
            String group = arguments.containsKey("group") ? arguments.get("group").toString() : null;
            return new CustomStoneCuttingRecipe<>(id,
                    showNotification(arguments),
                    parseResult(arguments), group,
                    singleInputIngredient(arguments)
            );
        }

        @Override
        public CustomStoneCuttingRecipe<A> readJson(Key id, JsonObject json) {
            String group = VANILLA_RECIPE_HELPER.readGroup(json);
            return new CustomStoneCuttingRecipe<>(id,
                    true,
                    parseResult(VANILLA_RECIPE_HELPER.stoneCuttingResult(json)), group,
                    toIngredient(VANILLA_RECIPE_HELPER.singleIngredient(json.get("ingredient")))
            );
        }
    }
}
