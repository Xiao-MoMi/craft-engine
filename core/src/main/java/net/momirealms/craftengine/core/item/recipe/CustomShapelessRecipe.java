package net.momirealms.craftengine.core.item.recipe;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.recipe.input.CraftingInput;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.CommonFunctions;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class CustomShapelessRecipe extends CustomCraftingTableRecipe {
    public static final Serializer SERIALIZER = new Serializer();
    private final List<Ingredient> ingredients;
    private final PlacementInfo placementInfo;
    private final boolean ingredientCountSupport;

    public CustomShapelessRecipe(Key id,
                                 boolean showNotification,
                                 CustomRecipeResult result,
                                 CustomRecipeResult visualResult,
                                 String group,
                                 CraftingRecipeCategory category,
                                 List<Ingredient> ingredients,
                                 Function<Context>[] craftingFunctions,
                                 Predicate<Context> craftingCondition,
                                 boolean alwaysRebuildOutput,
                                 boolean ingredientCountSupport) {
        super(id, showNotification, result, visualResult, group, category, craftingFunctions, craftingCondition, alwaysRebuildOutput);
        this.ingredients = ingredients;
        this.placementInfo = PlacementInfo.create(ingredients);
        this.ingredientCountSupport = ingredientCountSupport;
    }

    public PlacementInfo placementInfo() {
        return this.placementInfo;
    }

    @Override
    public List<Ingredient> ingredientsInUse() {
        return this.ingredients;
    }

    @Override
    public boolean matches(RecipeInput input) {
        return matches((CraftingInput) input);
    }

    private boolean matches(CraftingInput input) {
        if (input.ingredientCount() != this.ingredients.size()) {
            return false;
        }
        if (input.size() == 1 && this.ingredients.size() == 1) {
            return this.ingredients.getFirst().test(input.getItem(0));
        }
        return input.finder().canCraft(this);
    }

    @Override
    public void takeInput(@NotNull RecipeInput in, int ignore) {
        CraftingInput input = (CraftingInput) in;
        if (input.ingredientCount() != this.ingredients.size()) {
            return;
        }
        if (input.size() == 1 && this.ingredients.size() == 1) {
            UniqueIdItem item = input.getItem(0);
            Ingredient first = this.ingredients.getFirst();
            item.item().shrink(first.count() - ignore);
            return;
        }
        List<UniqueIdItem> inputItems = new ArrayList<>(input.items);
        // 数量多的排前面
        inputItems.sort((o1, o2) -> Integer.compare(o2.item().count(), o1.item().count()));
        boolean[] taken = new boolean[inputItems.size()];
        outer:
        for (Ingredient ingredient : this.ingredients) {
            for (int j = 0; j < taken.length; j++) {
                if (!taken[j]) {
                    UniqueIdItem inputItem = inputItems.get(j);
                    if (ingredient.test(inputItem)) {
                        int toShrink = ingredient.count() - ignore;
                        if (toShrink > 0) {
                            inputItem.item().shrink(toShrink);
                        }
                        taken[j] = true;
                        continue outer;
                    }
                }
            }
        }
    }

    public boolean ingredientCountSupport() {
        return this.ingredientCountSupport;
    }

    @Override
    public @NotNull Key serializerType() {
        return RecipeSerializers.SHAPELESS;
    }

    public static class Serializer extends AbstractRecipeSerializer<CustomShapelessRecipe> {

        @SuppressWarnings({"unchecked", "DuplicatedCode"})
        @Override
        public CustomShapelessRecipe readConfig(Key id, ConfigSection section) {
            List<Ingredient> ingredients = new ArrayList<>(4);
            boolean hasAdditionalInput = false;
            ConfigValue ingredientsValue = section.getNonNullValue(INGREDIENTS, ConfigConstants.ARGUMENT_LIST);
            if (ingredientsValue.is(Map.class)) {
                ConfigSection ingredientSection = ingredientsValue.getAsSection();
                for (String key : ingredientSection.keySet()) {
                    Ingredient value = ingredientSection.getNonNullValue(key, ConfigConstants.ARGUMENT_LIST, super::parseIngredient);
                    ingredients.add(value);
                    if (value.count() > 1) {
                        hasAdditionalInput = true;
                    }
                }
            } else if (ingredientsValue.is(List.class)) {
                ingredients.addAll(ingredientsValue.getAsList(super::parseIngredient));
                for (Ingredient ingredient : ingredients) {
                    if (ingredient.count() > 1) {
                        hasAdditionalInput = true;
                        break;
                    }
                }
            } else {
                Ingredient ingredient = super.parseIngredient(ingredientsValue);
                ingredients.add(ingredient);
                if (ingredient.count() > 1) {
                    hasAdditionalInput = true;
                }
            }
            // 按照数量从多到少排序
            if (hasAdditionalInput) {
                ingredients.sort((o1, o2) -> Integer.compare(o2.count(), o1.count()));
            }
            return new CustomShapelessRecipe(
                    id,
                    section.getBoolean(SHOW_NOTIFICATIONS, true),
                    super.parseResult(section.getNonNullValue("result", ConfigConstants.ARGUMENT_SECTION)),
                    section.getValue(VISUAL_RESULT, super::parseResult),
                    section.getString("group"),
                    section.getEnum("category", CraftingRecipeCategory.class),
                    ingredients,
                    section.getList(FUNCTIONS, CommonFunctions::fromConfig).toArray(new Function[0]),
                    MiscUtils.allOf(section.getList(CONDITIONS, CommonConditions::fromConfig)),
                    section.getBoolean(ALWAYS_REBUILD_RESULT, true),
                    hasAdditionalInput
            );
        }

        @Override
        public CustomShapelessRecipe readJson(Key id, JsonObject json) {
            return new CustomShapelessRecipe(
                    id,
                    true,
                    parseResult(VANILLA_RECIPE_HELPER.craftingResult(json.get("result"))),
                    null,
                    VANILLA_RECIPE_HELPER.readGroup(json), VANILLA_RECIPE_HELPER.craftingCategory(json),
                    VANILLA_RECIPE_HELPER.shapelessIngredients(json.getAsJsonArray("ingredients")).stream().map(this::parseVanillaIngredient).toList(),
                    null,
                    null,
                    false,
                    false
            );
        }
    }
}
