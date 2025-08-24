package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.*;
import net.momirealms.craftengine.core.item.recipe.reader.VanillaRecipeReader;
import net.momirealms.craftengine.core.item.recipe.reader.VanillaRecipeReader1_20;
import net.momirealms.craftengine.core.item.recipe.reader.VanillaRecipeReader1_20_5;
import net.momirealms.craftengine.core.item.recipe.reader.VanillaRecipeReader1_21_2;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.item.recipe.result.PostProcessor;
import net.momirealms.craftengine.core.item.recipe.result.PostProcessors;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class AbstractRecipeSerializer<T, R extends Recipe<T>> implements RecipeSerializer<T, R> {
    protected static final VanillaRecipeReader VANILLA_RECIPE_HELPER =
            VersionHelper.isOrAbove1_21_2() ?
            new VanillaRecipeReader1_21_2() :
            VersionHelper.isOrAbove1_20_5() ?
            new VanillaRecipeReader1_20_5() :
            new VanillaRecipeReader1_20();

    protected boolean showNotification(Map<String, Object> arguments) {
        return ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("show-notification", true), "show-notification");
    }

    protected Ingredient<T> singleInputIngredient(Map<String, Object> arguments) {
        List<String> ingredients = MiscUtils.getAsStringList(getIngredientOrThrow(arguments));
        return toIngredient(ingredients);
    }

    // 不确定的类型
    protected Object getIngredientOrThrow(Map<String, Object> arguments) {
        Object ingredient = ResourceConfigUtils.get(arguments, "ingredient", "ingredients");
        if (ingredient == null) {
            throw new LocalizedResourceConfigException("warning.config.recipe.missing_ingredient");
        }
        return ingredient;
    }

    protected CookingRecipeCategory cookingRecipeCategory(Map<String, Object> arguments) {
        CookingRecipeCategory recipeCategory;
        try {
            recipeCategory = arguments.containsKey("category") ? CookingRecipeCategory.valueOf(arguments.get("category").toString().toUpperCase(Locale.ENGLISH)) : null;
        } catch (IllegalArgumentException e) {
            throw new LocalizedResourceConfigException("warning.config.recipe.cooking.invalid_category", e, arguments.get("category").toString(), EnumUtils.toString(CookingRecipeCategory.values()));
        }
        return recipeCategory;
    }

    protected CraftingRecipeCategory craftingRecipeCategory(Map<String, Object> arguments) {
        CraftingRecipeCategory recipeCategory;
        try {
            recipeCategory = arguments.containsKey("category") ? CraftingRecipeCategory.valueOf(arguments.get("category").toString().toUpperCase(Locale.ENGLISH)) : null;
        } catch (IllegalArgumentException e) {
            throw new LocalizedResourceConfigException("warning.config.recipe.crafting.invalid_category", e, arguments.get("category").toString(), EnumUtils.toString(CraftingRecipeCategory.values()));
        }
        return recipeCategory;
    }

    @SuppressWarnings({"unchecked"})
    protected CustomRecipeResult<T> parseResult(Map<String, Object> arguments) {
        Map<String, Object> resultMap = MiscUtils.castToMap(arguments.get("result"), true);
        if (resultMap == null) {
            throw new LocalizedResourceConfigException("warning.config.recipe.missing_result");
        }
        String id = ResourceConfigUtils.requireNonEmptyStringOrThrow(resultMap.get("id"), "warning.config.recipe.result.missing_id");
        int count = ResourceConfigUtils.getAsInt(resultMap.getOrDefault("count", 1), "count");
        BuildableItem<T> resultItem = (BuildableItem<T>) CraftEngine.instance().itemManager().getBuildableItem(Key.of(id)).orElseThrow(() -> new LocalizedResourceConfigException("warning.config.recipe.invalid_result", id));
        if (resultItem.isEmpty()) {
            throw new LocalizedResourceConfigException("warning.config.recipe.invalid_result", id);
        }
        List<PostProcessor<T>> processors = ResourceConfigUtils.parseConfigAsList(resultMap.get("post-processors"), PostProcessors::fromMap);
        return new CustomRecipeResult<>(
                resultItem,
                count,
                processors.isEmpty() ? null : processors.toArray(new PostProcessor[0])
        );
    }

    @SuppressWarnings("unchecked")
    protected CustomRecipeResult<T> parseResult(DatapackRecipeResult recipeResult) {
        Item<T> result = (Item<T>) CraftEngine.instance().itemManager().build(recipeResult);
        return new CustomRecipeResult<>(CloneableConstantItem.of(result), recipeResult.count(), null);
    }

    @Nullable
    protected Ingredient<T> toIngredient(String item) {
        return toIngredient(List.of(item));
    }

    @Nullable
    protected Ingredient<T> toIngredient(List<String> items) {
        Set<UniqueKey> itemIds = new HashSet<>();
        Set<UniqueKey> minecraftItemIds = new HashSet<>();
        ItemManager<T> itemManager = CraftEngine.instance().itemManager();
        for (String item : items) {
            if (item.charAt(0) == '#') itemIds.addAll(itemManager.itemIdsByTag(Key.of(item.substring(1))));
            else {
                Key itemId = Key.of(item);
                if (itemManager.getBuildableItem(itemId).isEmpty()) {
                    throw new LocalizedResourceConfigException("warning.config.recipe.invalid_ingredient", item);
                }
                itemIds.add(UniqueKey.create(itemId));
            }
        }
        boolean hasCustomItem = false;
        for (UniqueKey holder : itemIds) {
            Optional<CustomItem<T>> optionalCustomItem = itemManager.getCustomItem(holder.key());
            UniqueKey vanillaItem;
            if (optionalCustomItem.isPresent()) {
                CustomItem<T> customItem = optionalCustomItem.get();
                if (customItem.isVanillaItem()) {
                    vanillaItem = holder;
                } else {
                    vanillaItem = UniqueKey.create(customItem.material());
                    hasCustomItem = true;
                }
            } else {
                if (itemManager.isVanillaItem(holder.key())) {
                    vanillaItem = holder;
                } else {
                    throw new LocalizedResourceConfigException("warning.config.recipe.invalid_ingredient", holder.key().asString());
                }
            }
            if (vanillaItem == UniqueKey.AIR) {
                throw new LocalizedResourceConfigException("warning.config.recipe.invalid_ingredient", holder.key().asString());
            }
            minecraftItemIds.add(vanillaItem);
        }
        return itemIds.isEmpty() ? null : Ingredient.of(itemIds, minecraftItemIds, hasCustomItem);
    }
}
