package net.momirealms.craftengine.core.item.recipe;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public final class CustomSmeltingRecipe extends CustomCookingRecipe {
    public static final Serializer SERIALIZER = new Serializer();

    public CustomSmeltingRecipe(Key id,
                                boolean showNotification,
                                CustomRecipeResult result,
                                String group,
                                CookingRecipeCategory category,
                                Ingredient ingredient,
                                int cookingTime,
                                float experience,
                                Predicate<Context> craftingCondition) {
        super(id, showNotification, result, group, category, ingredient, cookingTime, experience, craftingCondition);
    }

    @Override
    public @NotNull Key serializerType() {
        return RecipeSerializers.SMELTING;
    }

    @Override
    public RecipeType type() {
        return RecipeType.SMELTING;
    }

    public static class Serializer extends AbstractRecipeSerializer<CustomSmeltingRecipe> {

        @SuppressWarnings("DuplicatedCode")
        @Override
        public CustomSmeltingRecipe readConfig(Key id, ConfigSection section) {
            return new CustomSmeltingRecipe(
                    id,
                    section.getBoolean(SHOW_NOTIFICATIONS, true),
                    super.parseResult(section.getNonNullValue("result", ConfigConstants.ARGUMENT_SECTION)),
                    section.getString("group"),
                    section.getEnum("category", CookingRecipeCategory.class),
                    section.getNonNullValue(INGREDIENTS, ConfigConstants.ARGUMENT_LIST, super::parseIngredient),
                    section.getInt("time", 80),
                    section.getFloat(EXP),
                    section.containsKey(CONDITIONS) ? MiscUtils.allOf(section.getList(CONDITIONS, CommonConditions::fromConfig)) : null
            );
        }

        @Override
        public CustomSmeltingRecipe readJson(Key id, JsonObject json) {
            return new CustomSmeltingRecipe(
                    id,
                    true,
                    parseResult(VANILLA_RECIPE_HELPER.cookingResult(json.get("result"))), VANILLA_RECIPE_HELPER.readGroup(json), VANILLA_RECIPE_HELPER.cookingCategory(json),
                    parseVanillaIngredient(VANILLA_RECIPE_HELPER.singleIngredient(json.get("ingredient"))),
                    VANILLA_RECIPE_HELPER.cookingTime(json),
                    VANILLA_RECIPE_HELPER.cookingExperience(json),
                    null
            );
        }
    }
}
