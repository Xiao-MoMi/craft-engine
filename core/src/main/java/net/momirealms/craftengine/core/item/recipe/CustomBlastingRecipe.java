package net.momirealms.craftengine.core.item.recipe;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class CustomBlastingRecipe<T> extends CustomCookingRecipe<T> {
    public static final Serializer<?> SERIALIZER = new Serializer<>();

    public CustomBlastingRecipe(Key id,
                                boolean showNotification,
                                CustomRecipeResult<T> result,
                                String group,
                                CookingRecipeCategory category,
                                Ingredient<T> ingredient,
                                int cookingTime,
                                float experience) {
        super(id, showNotification, result, group, category, ingredient, cookingTime, experience);
    }

    @Override
    public @NotNull Key serializerType() {
        return RecipeSerializers.BLASTING;
    }

    @Override
    public RecipeType type() {
        return RecipeType.BLASTING;
    }

    public static class Serializer<A> extends AbstractRecipeSerializer<A, CustomBlastingRecipe<A>> {

        @SuppressWarnings({"unchecked", "rawtypes", "DuplicatedCode"})
        @Override
        public CustomBlastingRecipe<A> readMap(Key id, Map<String, Object> arguments) {
            return new CustomBlastingRecipe(id,
                    showNotification(arguments),
                    parseResult(arguments), arguments.containsKey("group") ? arguments.get("group").toString() : null, cookingRecipeCategory(arguments),
                    singleInputIngredient(arguments),
                    ResourceConfigUtils.getAsInt(arguments.getOrDefault("time", 80), "time"),
                    ResourceConfigUtils.getAsFloat(arguments.getOrDefault("experience", 0.0f), "experience")
            );
        }

        @Override
        public CustomBlastingRecipe<A> readJson(Key id, JsonObject json) {
            return new CustomBlastingRecipe<>(id,
                    true,
                    parseResult(VANILLA_RECIPE_HELPER.cookingResult(json.get("result"))), VANILLA_RECIPE_HELPER.readGroup(json), VANILLA_RECIPE_HELPER.cookingCategory(json),
                    toIngredient(VANILLA_RECIPE_HELPER.singleIngredient(json.get("ingredient"))),
                    VANILLA_RECIPE_HELPER.cookingTime(json),
                    VANILLA_RECIPE_HELPER.cookingExperience(json)
            );
        }
    }
}
