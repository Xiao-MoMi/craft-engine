package net.momirealms.craftengine.core.item.recipe;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.recipe.input.CraftingInput;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.plugin.context.Condition;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomShapelessRecipe<T> extends CustomCraftingTableRecipe<T> {
    public static final Serializer<?> SERIALIZER = new Serializer<>();
    private final List<Ingredient<T>> ingredients;
    private final PlacementInfo<T> placementInfo;

    public CustomShapelessRecipe(Key id,
                                 boolean showNotification,
                                 CustomRecipeResult<T> result,
                                 CustomRecipeResult<T> visualResult,
                                 String group,
                                 CraftingRecipeCategory category,
                                 List<Ingredient<T>> ingredients,
                                 Function<Context>[] craftingFunctions,
                                 Condition<Context> craftingCondition,
                                 boolean alwaysRebuildOutput) {
        super(id, showNotification, result, visualResult, group, category, craftingFunctions, craftingCondition, alwaysRebuildOutput);
        this.ingredients = ingredients;
        this.placementInfo = PlacementInfo.create(ingredients);
    }

    public PlacementInfo<T> placementInfo() {
        return this.placementInfo;
    }

    @Override
    public List<Ingredient<T>> ingredientsInUse() {
        return this.ingredients;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(RecipeInput input) {
        return matches((CraftingInput<T>) input);
    }

    private boolean matches(CraftingInput<T> input) {
        if (input.ingredientCount() != this.ingredients.size()) {
            return false;
        }
        if (input.size() == 1 && this.ingredients.size() == 1) {
            return this.ingredients.getFirst().test(input.getItem(0));
        }
        return input.finder().canCraft(this);
    }

    @Override
    public @NotNull Key serializerType() {
        return RecipeSerializers.SHAPELESS;
    }

    public static class Serializer<A> extends AbstractRecipeSerializer<A, CustomShapelessRecipe<A>> {

        @SuppressWarnings({"unchecked", "rawtypes", "DuplicatedCode"})
        @Override
        public CustomShapelessRecipe<A> readMap(Key id, Map<String, Object> arguments) {
            List<Ingredient<A>> ingredients = new ArrayList<>();
            Object ingredientsObject = getIngredientOrThrow(arguments);
            if (ingredientsObject instanceof Map<?,?> map) {
                for (Map.Entry<String, Object> entry : (MiscUtils.castToMap(map, false)).entrySet()) {
                    if (entry.getValue() == null) continue;
                    ingredients.add(parseIngredient(entry.getValue()));
                }
            } else if (ingredientsObject instanceof List<?> list) {
                for (Object obj : list) {
                    ingredients.add(parseIngredient(obj));
                }
            } else {
                ingredients.add(parseIngredient(ingredientsObject));
            }
            return new CustomShapelessRecipe(id,
                    showNotification(arguments),
                    parseResult(arguments),
                    parseVisualResult(arguments),
                    arguments.containsKey("group") ? arguments.get("group").toString() : null, craftingRecipeCategory(arguments),
                    ingredients,
                    functions(arguments),
                    conditions(arguments),
                    ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("always-rebuild-result", true), "always-rebuild-result")
            );
        }

        @Override
        public CustomShapelessRecipe<A> readJson(Key id, JsonObject json) {
            return new CustomShapelessRecipe<>(id,
                    true,
                    parseResult(VANILLA_RECIPE_HELPER.craftingResult(json.get("result"))),
                    null,
                    VANILLA_RECIPE_HELPER.readGroup(json), VANILLA_RECIPE_HELPER.craftingCategory(json),
                    VANILLA_RECIPE_HELPER.shapelessIngredients(json.getAsJsonArray("ingredients")).stream().map(this::toIngredient).toList(),
                    null,
                    null,
                    false
            );
        }
    }
}
