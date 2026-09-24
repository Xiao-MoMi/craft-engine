package net.momirealms.craftengine.core.item.recipe;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.recipe.input.BrewingInput;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class CustomBrewingRecipe extends AbstractFixedResultRecipe {
    public static final Serializer<?> SERIALIZER = new Serializer<>();
    private final Ingredient container;
    private final Ingredient ingredient;

    public CustomBrewingRecipe(@NotNull Key id,
                               boolean showNotification,
                               @NotNull Ingredient ingredient,
                               @NotNull CustomRecipeResult result,
                               @NotNull Ingredient container) {
        super(id, showNotification, result);
        this.container = container;
        this.ingredient = ingredient;
        this.result = result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(RecipeInput input) {
        BrewingInput<Object> brewingInput = (BrewingInput<Object>) input;
        return this.container.test(brewingInput.container()) && this.ingredient.test(brewingInput.ingredient());
    }

    @Override
    public List<Ingredient> ingredientsInUse() {
        List<Ingredient> ingredients = new ArrayList<>();
        ingredients.add(this.container);
        ingredients.add(this.ingredient);
        return ingredients;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void takeInput(@NotNull RecipeInput input, int ignore) {
        BrewingInput<Object> brewingInput = (BrewingInput<Object>) input;
        takeIngredient(this.container, brewingInput.container().item(), ignore);
        takeIngredient(this.ingredient, brewingInput.ingredient().item(), ignore);
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
    public Ingredient container() {
        return this.container;
    }

    @NotNull
    public Ingredient ingredient() {
        return this.ingredient;
    }

    @SuppressWarnings({"DuplicatedCode"})
    public static class Serializer<A> extends AbstractRecipeSerializer<CustomBrewingRecipe> {
        private static final String[] REAGENT = ConfigKeys.of("ingredient(s)|reagent");
        private static final String[] OUTPUT = ConfigKeys.of("result|output");
        private static final String[] INPUT = ConfigKeys.of("container|input");

        @Override
        public CustomBrewingRecipe readConfig(Key id, ConfigSection section) {
            return new CustomBrewingRecipe(
                    id,
                    section.getBoolean(SHOW_NOTIFICATIONS, true),
                    section.getNonNullValue(REAGENT, ConfigConstants.ARGUMENT_LIST, super::parseIngredient),
                    super.parseResult(section.getNonNullValue(OUTPUT, ConfigConstants.ARGUMENT_SECTION)),
                    section.getNonNullValue(INPUT, ConfigConstants.ARGUMENT_LIST, super::parseIngredient)
            );
        }

        @Override
        public CustomBrewingRecipe readJson(Key id, JsonObject json) {
            return new CustomBrewingRecipe(
                    id,
                    false,
                    parsePotionIngredient(json.getAsJsonObject("reagent")),
                    parseResult(VANILLA_RECIPE_HELPER.craftingResult(json.get("output"))),
                    parsePotionIngredient(json.getAsJsonObject("input"))
            );
        }

        private Ingredient parsePotionIngredient(JsonObject json) {
            List<String> itemIds = VANILLA_RECIPE_HELPER.singleIngredient(json.get("item"));
            Ingredient ingredient = parseVanillaIngredient(itemIds);
            if (ingredient == null) {
                throw new IllegalArgumentException("Brewing ingredient cannot be empty");
            }
            if (!json.has("potion_contents")) return ingredient;
            return Ingredient.of(ingredient.elements(),
                    new HashSet<>(ingredient.items()),
                    new HashSet<>(ingredient.minecraftItems()),
                    ingredient.hasCustomItem(),
                    ingredient.count(),
                    CraftEngine.instance().recipeManager().parsePotionContentsPredicate(json.getAsJsonObject("potion_contents"))
            );
        }
    }
}
