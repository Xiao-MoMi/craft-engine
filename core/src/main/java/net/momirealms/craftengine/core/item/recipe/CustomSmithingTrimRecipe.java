package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.input.SmithingInput;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomSmithingTrimRecipe<T> implements Recipe<T> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final Key id;
    private final Ingredient<T> base;
    private final Ingredient<T> template;
    private final Ingredient<T> addition;
    @Nullable // 1.21.5
    private final Key pattern;

    public CustomSmithingTrimRecipe(@NotNull Key id,
                                    @NotNull Ingredient<T> base,
                                    @NotNull Ingredient<T> template,
                                    @NotNull Ingredient<T> addition,
                                    @Nullable Key pattern
    ) {
        this.id = id;
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
        if (ingredient != null) {
            if (item == null || item.isEmpty()) {
                return false;
            }
            return ingredient.test(item);
        } else {
            return item == null || item.isEmpty();
        }
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
    public @NotNull Key type() {
        return RecipeTypes.SMITHING_TRIM;
    }

    @Override
    public Key id() {
        return this.id;
    }

    @Nullable
    public Ingredient<T> base() {
        return this.base;
    }

    @Nullable
    public Ingredient<T> template() {
        return template;
    }

    @Nullable
    public Ingredient<T> addition() {
        return addition;
    }

    @Nullable
    public Key pattern() {
        return pattern;
    }

    @SuppressWarnings({"DuplicatedCode"})
    public static class Factory<A> implements RecipeFactory<A> {

        @Override
        public Recipe<A> create(Key id, Map<String, Object> arguments) {
            List<String> base = MiscUtils.getAsStringList(arguments.get("base"));
            if (base.isEmpty()) {
                throw new LocalizedResourceConfigException("warning.config.recipe.smithing_trim.missing_base");
            }
            List<String> addition = MiscUtils.getAsStringList(arguments.get("addition"));
            if (addition.isEmpty()) {
                throw new LocalizedResourceConfigException("warning.config.recipe.smithing_trim.missing_addition");
            }
            List<String> template = MiscUtils.getAsStringList(arguments.get("template-type"));
            if (template.isEmpty()) {
                throw new LocalizedResourceConfigException("warning.config.recipe.smithing_trim.missing_template_type");
            }
            Key pattern = VersionHelper.isOrAbove1_21_5() ? Key.of(ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("pattern"), "warning.config.recipe.smithing_trim.missing_pattern")) : null;
            return new CustomSmithingTrimRecipe<>(id,
                    ResourceConfigUtils.requireNonNullOrThrow(toIngredient(base), "warning.config.recipe.smithing_trim.missing_base"),
                    ResourceConfigUtils.requireNonNullOrThrow(toIngredient(template), "warning.config.recipe.smithing_trim.missing_template_type"),
                    ResourceConfigUtils.requireNonNullOrThrow(toIngredient(addition), "warning.config.recipe.smithing_trim.missing_addition"),
                    pattern);
        }
    }
}
