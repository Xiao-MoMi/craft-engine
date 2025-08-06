package net.momirealms.craftengine.core.item.recipe;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.input.SmithingInput;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CustomSmithingTrimRecipe<T> extends AbstractRecipe<T> {
    public static final Serializer<?> SERIALIZER = new Serializer<>();
    private final Ingredient<T> base;
    private final Ingredient<T> template;
    private final Ingredient<T> addition;
    @Nullable // 1.21.5
    private final Key pattern;

    public CustomSmithingTrimRecipe(@NotNull Key id,
                                    boolean showNotification,
                                    @NotNull Ingredient<T> template,
                                    @NotNull Ingredient<T> base,
                                    @NotNull Ingredient<T> addition,
                                    @Nullable Key pattern
    ) {
        super(id, showNotification);
        this.base = base;
        this.template = template;
        this.addition = addition;
        this.pattern = pattern;
        if (pattern == null && VersionHelper.isOrAbove1_21_5()) {
            throw new IllegalStateException("SmithingTrimRecipe cannot have a null pattern on 1.21.5 and above.");
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T assemble(RecipeInput input, ItemBuildContext context) {
        SmithingInput<T> smithingInput = (SmithingInput<T>) input;
        Item<T> processed = (Item<T>) CraftEngine.instance().itemManager().applyTrim((Item<Object>) smithingInput.base().item(), (Item<Object>) smithingInput.addition().item(), (Item<Object>) smithingInput.template().item(), this.pattern);
        return processed.getItem();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(RecipeInput input) {
        SmithingInput<T> smithingInput = (SmithingInput<T>) input;
        return checkIngredient(this.base, smithingInput.base())
                && checkIngredient(this.template, smithingInput.template())
                && checkIngredient(this.addition, smithingInput.addition());
    }

    private boolean checkIngredient(Ingredient<T> ingredient, UniqueIdItem<T> item) {
        return ingredient.test(item);
    }

    @Override
    public List<Ingredient<T>> ingredientsInUse() {
        List<Ingredient<T>> ingredients = new ArrayList<>();
        ingredients.add(this.base);
        ingredients.add(this.template);
        ingredients.add(this.addition);
        return ingredients;
    }

    @Override
    public @NotNull Key serializerType() {
        return RecipeSerializers.SMITHING_TRIM;
    }

    @Override
    public RecipeType type() {
        return RecipeType.SMITHING;
    }

    @NotNull
    public Ingredient<T> base() {
        return this.base;
    }

    @NotNull
    public Ingredient<T> template() {
        return template;
    }

    @NotNull
    public Ingredient<T> addition() {
        return addition;
    }

    @Nullable
    public Key pattern() {
        return pattern;
    }

    @SuppressWarnings({"DuplicatedCode"})
    public static class Serializer<A> extends AbstractRecipeSerializer<A, CustomSmithingTrimRecipe<A>> {

        @Override
        public CustomSmithingTrimRecipe<A> readMap(Key id, Map<String, Object> arguments) {
            List<String> base = MiscUtils.getAsStringList(arguments.get("base"));
            List<String> template = MiscUtils.getAsStringList(arguments.get("template-type"));
            List<String> addition = MiscUtils.getAsStringList(arguments.get("addition"));
            Key pattern = VersionHelper.isOrAbove1_21_5() ? Key.of(ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("pattern"), "warning.config.recipe.smithing_trim.missing_pattern")) : null;
            return new CustomSmithingTrimRecipe<>(id,
                    showNotification(arguments),
                    ResourceConfigUtils.requireNonNullOrThrow(toIngredient(template), "warning.config.recipe.smithing_trim.missing_template_type"),
                    ResourceConfigUtils.requireNonNullOrThrow(toIngredient(base), "warning.config.recipe.smithing_trim.missing_base"),
                    ResourceConfigUtils.requireNonNullOrThrow(toIngredient(addition), "warning.config.recipe.smithing_trim.missing_addition"),
                    pattern
            );
        }

        @Override
        public CustomSmithingTrimRecipe<A> readJson(Key id, JsonObject json) {
            return new CustomSmithingTrimRecipe<>(id,
                    VANILLA_RECIPE_HELPER.showNotification(json),
                    Objects.requireNonNull(toIngredient(VANILLA_RECIPE_HELPER.singleIngredient(json.get("template")))),
                    Objects.requireNonNull(toIngredient(VANILLA_RECIPE_HELPER.singleIngredient(json.get("base")))),
                    Objects.requireNonNull(toIngredient(VANILLA_RECIPE_HELPER.singleIngredient(json.get("addition")))),
                    VersionHelper.isOrAbove1_21_5() ? Key.of(json.get("pattern").getAsString()) : null
            );
        }
    }
}
