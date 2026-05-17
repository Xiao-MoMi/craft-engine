package net.momirealms.craftengine.core.item.recipe;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.item.recipe.input.CraftingInput;
import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.context.CommonConditions;
import net.momirealms.craftengine.core.plugin.context.CommonFunctions;
import net.momirealms.craftengine.core.plugin.context.Context;
import net.momirealms.craftengine.core.plugin.context.function.Function;
import net.momirealms.craftengine.core.util.Color;
import net.momirealms.craftengine.core.util.DyeColor;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;

public final class CustomDyeRecipe extends CustomCraftingTableRecipe {
    public static final Serializer SERIALIZER = new Serializer();
    private final Ingredient target;
    private final Ingredient dye;
    private final boolean transmute;

    public CustomDyeRecipe(Key id,
                           boolean showNotification,
                           CustomRecipeResult result,
                           @Nullable CustomRecipeResult visualResult,
                           String group,
                           CraftingRecipeCategory category,
                           Ingredient target,
                           Ingredient dye,
                           Function<Context>[] craftingFunctions,
                           Predicate<Context> craftingCondition,
                           boolean alwaysRebuildResult,
                           boolean transmute) {
        super(id, showNotification, result, visualResult, group, category, craftingFunctions, craftingCondition, alwaysRebuildResult);
        this.transmute = transmute;
        this.target = target;
        this.dye = dye;
    }

    public Ingredient target() {
        return this.target;
    }

    public Ingredient dye() {
        return this.dye;
    }

    @Override
    public boolean matches(RecipeInput input) {
        CraftingInput craftingInput = (CraftingInput) input;
        if (craftingInput.ingredientCount() < 2) {
            return false;
        }
        boolean hasTarget = false;
        boolean hasDyes = false;
        for (int i = 0; i < craftingInput.size(); i++) {
            UniqueIdItem item = craftingInput.getItem(i);
            if (!item.isEmpty()) {
                if (this.target.test(item)) {
                    if (hasTarget) {
                        return false;
                    }
                    hasTarget = true;
                } else {
                    if (!this.dye.test(item) || !isDye(item)) {
                        return false;
                    }
                    hasDyes = true;
                }
            }
        }
        return hasDyes && hasTarget;
    }

    @Override
    public Item assemble(RecipeInput input, ItemBuildContext context) {
        CraftingInput craftingInput = (CraftingInput) input;
        List<Color> colors = new ArrayList<>();
        Item itemToDye = null;
        for (int i = 0; i < craftingInput.size(); i++) {
            UniqueIdItem item = craftingInput.getItem(i);
            if (!item.isEmpty()) {
                if (this.target.test(item)) {
                    if (itemToDye != null) {
                        return Item.empty();
                    }
                    itemToDye = item.item().copyWithCount(1);
                } else {
                    if (!this.dye.test(item)) {
                        return Item.empty();
                    }
                    Color dyeColor = getDyeColor(item.item());
                    if (dyeColor != null) {
                        colors.add(dyeColor);
                    } else {
                        return Item.empty();
                    }
                }
            }
        }
        if (itemToDye == null) {
            return Item.empty();
        }
        if (colors.isEmpty()) {
            return Item.empty();
        }

        if (this.transmute) {
            Item resultItem = super.result.buildItem(context);
            Item transmuteCopy = itemToDye.transmuteCopy(resultItem.vanillaId());
            transmuteCopy.merge(resultItem);
            return transmuteCopy.applyDyedColors(colors);
        } else {
            return itemToDye.applyDyedColors(colors);
        }
    }

    private Color getDyeColor(final Item dyeItem) {
        Optional<ItemDefinition> optionalCustomItem = dyeItem.getDefinition();
        if (optionalCustomItem.isPresent()) {
            ItemDefinition itemDefinition = optionalCustomItem.get();
            return Optional.ofNullable(itemDefinition.settings().dyeColor()).orElseGet(() -> getVanillaDyeColor(dyeItem));
        }
        return getVanillaDyeColor(dyeItem);
    }

    private Color getVanillaDyeColor(final Item dyeItem) {
        String colorType = (String) dyeItem.getComponentAsJava(DataComponentKeys.DYE);
        if (colorType == null) {
            return null;
        }
        return new Color(DyeColor.valueOf(colorType.toUpperCase(Locale.ROOT)).textureDiffuseColor());
    }

    private boolean isDye(final UniqueIdItem item) {
        Optional<ItemDefinition> optionalItemDefinition = item.item().getDefinition();
        if (optionalItemDefinition.isPresent()) {
            ItemDefinition itemDefinition = optionalItemDefinition.get();
            if (itemDefinition.settings().dyeColor() != null) {
                return true;
            }
        }
        return item.item().hasComponent(DataComponentKeys.DYE);
    }

    @Override
    public void takeInput(@NotNull RecipeInput input, int ignore) {
    }

    @Override
    public List<Ingredient> ingredientsInUse() {
        return List.of(this.dye, this.target);
    }

    @Override
    public @NotNull Key serializerType() {
        return RecipeSerializers.DYE;
    }

    @Override
    public boolean canBeSearched() {
        return false;
    }

    public static class Serializer extends AbstractRecipeSerializer<CustomDyeRecipe> {
        private static final String[] RESULT_OR_TARGET = new String[] {"result", "target"}; // 必须先 result 后 target

        @SuppressWarnings("unchecked")
        @Override
        public CustomDyeRecipe readConfig(Key id, ConfigSection section) {
            return new CustomDyeRecipe(
                    id,
                    section.getBoolean(SHOW_NOTIFICATIONS, true),
                    super.parseResult(section.getNonNullValue(RESULT_OR_TARGET, ConfigConstants.ARGUMENT_SECTION)),
                    null,
                    section.getString("group"),
                    section.getEnum("category", CraftingRecipeCategory.class, CraftingRecipeCategory.MISC),
                    section.getNonNullValue("target", ConfigConstants.ARGUMENT_LIST, super::parseIngredient),
                    section.getNonNullValue("dye", ConfigConstants.ARGUMENT_LIST, super::parseIngredient),
                    section.getList(FUNCTIONS, CommonFunctions::fromConfig).toArray(new Function[0]),
                    MiscUtils.allOf(section.getList(CONDITIONS, CommonConditions::fromConfig)),
                    section.getBoolean(ALWAYS_REBUILD_RESULT, section.containsKey("result")),
                    section.containsKey("result")
            );
        }

        @Override
        public CustomDyeRecipe readJson(Key id, JsonObject json) {
            return new CustomDyeRecipe(
                    id,
                    VANILLA_RECIPE_HELPER.showNotification(json),
                    parseResult(VANILLA_RECIPE_HELPER.craftingResult(json.get("result"))),
                    null,
                    VANILLA_RECIPE_HELPER.readGroup(json),
                    VANILLA_RECIPE_HELPER.craftingCategory(json),
                    parseVanillaIngredient(VANILLA_RECIPE_HELPER.singleIngredient(json.get("target"))),
                    parseVanillaIngredient(VANILLA_RECIPE_HELPER.singleIngredient(json.get("dye"))),
                    null,
                    null,
                    false,
                    true
            );
        }
    }
}
