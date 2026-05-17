package net.momirealms.craftengine.core.item.recipe;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.input.SingleItemInput;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CustomStoneCuttingRecipe extends AbstractGroupedRecipe {
    public static final Serializer SERIALIZER = new Serializer();
    private final Ingredient ingredient;

    public CustomStoneCuttingRecipe(Key id,
                                    boolean showNotification,
                                    CustomRecipeResult result,
                                    String group,
                                    Ingredient ingredient) {
        super(id, showNotification, result, group);
        this.ingredient = ingredient;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(RecipeInput input) {
        return this.ingredient.test(((SingleItemInput) input).input());
    }

    @Override
    public List<Ingredient> ingredientsInUse() {
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

    public Ingredient ingredient() {
        return this.ingredient;
    }

    @Override
    public void takeInput(@NotNull RecipeInput in, int ignore) {
        SingleItemInput input = (SingleItemInput) in;
        takeIngredient(this.ingredient, input.input().item(), ignore);
    }

    public static class Serializer extends AbstractRecipeSerializer<CustomStoneCuttingRecipe> {

        @SuppressWarnings({"DuplicatedCode"})
        @Override
        public CustomStoneCuttingRecipe readConfig(Key id, ConfigSection section) {
            return new CustomStoneCuttingRecipe(id,
                    section.getBoolean(SHOW_NOTIFICATIONS, true),
                    super.parseResult(section.getNonNullValue("result", ConfigConstants.ARGUMENT_SECTION)),
                    section.getString("group"),
                    section.getNonNullValue(INGREDIENTS, ConfigConstants.ARGUMENT_LIST, super::parseIngredient)
            );
        }

        @Override
        public CustomStoneCuttingRecipe readJson(Key id, JsonObject json) {
            String group = VANILLA_RECIPE_HELPER.readGroup(json);
            return new CustomStoneCuttingRecipe(id,
                    true,
                    parseResult(VANILLA_RECIPE_HELPER.stoneCuttingResult(json)), group,
                    parseVanillaIngredient(VANILLA_RECIPE_HELPER.singleIngredient(json.get("ingredient")))
            );
        }
    }
}
