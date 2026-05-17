package net.momirealms.craftengine.core.item.recipe;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.input.SmithingInput;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.item.recipe.transform.ItemTransformDataProcessor;
import net.momirealms.craftengine.core.item.recipe.transform.ItemTransformDataProcessors;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.CommonFunctions;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public final class CustomSmithingTransformRecipe extends AbstractFixedResultRecipe
        implements ConditionalRecipe, VisualResultRecipe, FunctionalRecipe {
    public static final Serializer SERIALIZER = new Serializer();
    private final Ingredient base;
    private final Ingredient template;
    private final Ingredient addition;
    private final boolean mergeComponents;
    private final boolean mergeEnchantments;
    private final List<ItemTransformDataProcessor> processors;
    private final Predicate<Context> condition;
    private final Function<Context>[] smithingFunctions;
    private final CustomRecipeResult visualResult;
    private final boolean ingredientCountSupport;

    public CustomSmithingTransformRecipe(Key id,
                                         boolean showNotification,
                                         @Nullable Ingredient template,
                                         @NotNull Ingredient base,
                                         @Nullable Ingredient addition,
                                         CustomRecipeResult result,
                                         @Nullable CustomRecipeResult visualResult,
                                         List<ItemTransformDataProcessor> processors,
                                         boolean mergeComponents,
                                         boolean mergeEnchantments,
                                         Function<Context>[] smithingFunctions,
                                         Predicate<Context> condition,
                                         boolean ingredientCountSupport
    ) {
        super(id, showNotification, result);
        this.base = base;
        this.template = template;
        this.addition = addition;
        this.processors = processors;
        this.mergeComponents = mergeComponents;
        this.mergeEnchantments = mergeEnchantments;
        this.condition = condition;
        this.smithingFunctions = smithingFunctions;
        this.visualResult = visualResult;
        this.ingredientCountSupport = ingredientCountSupport;
    }

    public boolean ingredientCountSupport() {
        return this.ingredientCountSupport;
    }

    public boolean mergeComponents() {
        return this.mergeComponents;
    }

    public boolean mergeEnchantments() {
        return this.mergeEnchantments;
    }

    @Override
    public Function<Context>[] functions() {
        return this.smithingFunctions;
    }

    @Override
    public CustomRecipeResult visualResult() {
        return this.visualResult;
    }

    @Override
    public boolean canUse(Context context) {
        if (this.condition != null) return this.condition.test(context);
        return true;
    }

    @Override
    public boolean hasCondition() {
        return this.condition != null;
    }

    @Override
    public void takeInput(@NotNull RecipeInput input, int ignore) {
        SmithingInput smithingInput = (SmithingInput) input;
        super.takeIngredient(this.base, smithingInput.base().item(), ignore);
        if (this.template != null) super.takeIngredient(this.template, smithingInput.template().item(), ignore);
        if (this.addition != null) super.takeIngredient(this.addition, smithingInput.addition().item(), ignore);
    }

    @Override
    public boolean matches(RecipeInput input) {
        SmithingInput smithingInput = (SmithingInput) input;
        return checkIngredient(this.base, smithingInput.base())
                && checkIngredient(this.template, smithingInput.template())
                && checkIngredient(this.addition, smithingInput.addition());
    }

    private boolean checkIngredient(Ingredient ingredient, UniqueIdItem item) {
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
    public List<Ingredient> ingredientsInUse() {
        List<Ingredient> ingredients = new ArrayList<>();
        ingredients.add(this.base);
        if (this.template != null) {
            ingredients.add(this.template);
        }
        if (this.addition != null) {
            ingredients.add(this.addition);
        }
        return ingredients;
    }

    @Override
    public @NotNull Key serializerType() {
        return RecipeSerializers.SMITHING_TRANSFORM;
    }

    @Override
    public RecipeType type() {
        return RecipeType.SMITHING;
    }

    @Override
    public Item assembleVisual(RecipeInput input, ItemBuildContext context) {
        SmithingInput smithingInput = ((SmithingInput) input);
        Item base = smithingInput.base().item();
        Item result = this.visualResult().buildItem(context);
        return createSmithingResult(base, result);
    }

    @Override
    public Item assemble(RecipeInput input, ItemBuildContext context) {
        SmithingInput smithingInput = ((SmithingInput) input);
        Item base = smithingInput.base().item();
        Item result = this.result(context);
        return createSmithingResult(base, result);
    }

    private Item createSmithingResult(Item base, Item result) {
        Item finalResult = result;
        if (this.mergeComponents) {
            finalResult = base.mergeCopy(result);
        }
        if (this.processors != null) {
            for (ItemTransformDataProcessor processor : this.processors) {
                processor.accept(base, result, finalResult);
            }
        }
        return finalResult;
    }

    @NotNull
    public Ingredient base() {
        return this.base;
    }

    @Nullable
    public Ingredient template() {
        return template;
    }

    @Nullable
    public Ingredient addition() {
        return addition;
    }

    @SuppressWarnings({"DuplicatedCode"})
    public static class Serializer extends AbstractRecipeSerializer<CustomSmithingTransformRecipe> {
        private static final String[] TEMPLATE_TYPE = new String[]{"template_type", "template-type"};
        private static final String[] MERGE_COMPONENTS = new String[]{"merge-components", "merge_components"};
        private static final String[] MERGE_ENCHANTMENTS = new String[]{"merge-enchantments", "merge_enchantments"};

        @SuppressWarnings("unchecked")
        @Override
        public CustomSmithingTransformRecipe readConfig(Key id, ConfigSection section) {
            Ingredient templateIngredient = section.getValue(TEMPLATE_TYPE, super::parseIngredient);
            Ingredient baseIngredient = section.getNonNullValue("base", ConfigConstants.ARGUMENT_LIST, super::parseIngredient);
            Ingredient additionIngredient = section.getValue("addition", super::parseIngredient);
            boolean countSupport = false;
            if (/* !countSupport && */ templateIngredient != null && templateIngredient.count() > 1) {
                countSupport = true;
            }
            if (!countSupport && additionIngredient != null && additionIngredient.count() > 1) {
                countSupport = true;
            }
            if (!countSupport && /* baseIngredient != null && */ baseIngredient.count() > 1) {
                countSupport = true;
            }
            return new CustomSmithingTransformRecipe(id,
                    section.getBoolean(SHOW_NOTIFICATIONS, true),
                    templateIngredient,
                    baseIngredient,
                    additionIngredient,
                    super.parseResult(section.getNonNullValue("result", ConfigConstants.ARGUMENT_SECTION)),
                    section.getValue(VISUAL_RESULT, super::parseResult),
                    section.getList(POST_PROCESSOR, ItemTransformDataProcessors::fromConfig),
                    section.getBoolean(MERGE_COMPONENTS, true),
                    section.getBoolean(MERGE_ENCHANTMENTS, false),
                    section.getList(FUNCTIONS, CommonFunctions::fromConfig).toArray(new Function[0]),
                    MiscUtils.allOf(section.getList(CONDITIONS, CommonConditions::fromConfig)),
                    countSupport
            );
        }

        @Override
        public CustomSmithingTransformRecipe readJson(Key id, JsonObject json) {
            return new CustomSmithingTransformRecipe(
                    id,
                    true,
                    parseVanillaIngredient(VANILLA_RECIPE_HELPER.singleIngredient(json.get("template"))),
                    Objects.requireNonNull(parseVanillaIngredient(VANILLA_RECIPE_HELPER.singleIngredient(json.get("base")))),
                    parseVanillaIngredient(VANILLA_RECIPE_HELPER.singleIngredient(json.get("addition"))),
                    parseResult(VANILLA_RECIPE_HELPER.smithingResult(json.getAsJsonObject("result"))),
                    null,
                    null,
                    true,
                    false,
                    null,
                    null,
                    false
            );
        }
    }
}
