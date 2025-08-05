package net.momirealms.craftengine.core.item.recipe;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class CustomCampfireRecipe<T> extends CustomCookingRecipe<T> {
    public static final Serializer<?> SERIALIZER = new Serializer<>();

    public CustomCampfireRecipe(Key id, CookingRecipeCategory category, String group, Ingredient<T> ingredient, int cookingTime, float experience, CustomRecipeResult<T> result) {
        super(id, category, group, ingredient, cookingTime, experience, result);
    }

    @Override
    public @NotNull Key serializerType() {
        return RecipeSerializers.CAMPFIRE_COOKING;
    }

    @Override
    public RecipeType type() {
        return RecipeType.CAMPFIRE_COOKING;
    }

    public static class Serializer<A> extends AbstractRecipeSerializer<A, CustomCampfireRecipe<A>> {

        @SuppressWarnings({"unchecked", "rawtypes", "DuplicatedCode"})
        @Override
        public CustomCampfireRecipe<A> readMap(Key id, Map<String, Object> arguments) {
            return new CustomCampfireRecipe(id,
                    cookingRecipeCategory(arguments),
                    arguments.containsKey("group") ? arguments.get("group").toString() : null,
                    singleInputIngredient(arguments),
                    ResourceConfigUtils.getAsInt(arguments.getOrDefault("time", 80), "time"),
                    ResourceConfigUtils.getAsFloat(arguments.getOrDefault("experience", 0.0f), "experience"),
                    parseResult(arguments)
            );
        }

        @Override
        public CustomCampfireRecipe<A> readJson(Key id, JsonObject json) {
            return new CustomCampfireRecipe<>(id,
                    VANILLA_RECIPE_HELPER.cookingCategory(json),
                    VANILLA_RECIPE_HELPER.readGroup(json),
                    toIngredient(VANILLA_RECIPE_HELPER.singleIngredient(json.get("ingredient"))),
                    VANILLA_RECIPE_HELPER.cookingTime(json),
                    VANILLA_RECIPE_HELPER.cookingExperience(json),
                    parseResult(VANILLA_RECIPE_HELPER.cookingResult(json.get("result")))
            );
        }
    }
}
