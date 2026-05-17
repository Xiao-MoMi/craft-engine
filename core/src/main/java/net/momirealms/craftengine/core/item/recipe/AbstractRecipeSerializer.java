package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.*;
import net.momirealms.craftengine.core.item.recipe.reader.*;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.item.recipe.result.PostProcessor;
import net.momirealms.craftengine.core.item.recipe.result.PostProcessors;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.ConfigConstants;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.UniqueKey;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class AbstractRecipeSerializer<R extends Recipe> implements RecipeSerializer<R> {
    protected static final VanillaRecipeReader VANILLA_RECIPE_HELPER =
            VersionHelper.isOrAbove26_1 ?
            new VanillaRecipeReader26_1() :
            VersionHelper.isOrAbove1_21_2 ?
            new VanillaRecipeReader1_21_2() :
            VersionHelper.isOrAbove1_20_5 ?
            new VanillaRecipeReader1_20_5() :
            new VanillaRecipeReader1_20();
    protected static final String[] SHOW_NOTIFICATIONS = new String[] {"show_notification", "show-notification"};
    protected static final String[] INGREDIENTS = new String[] {"ingredients", "ingredient"};
    protected static final String[] EXP = new String[] {"exp", "experience"};
    protected static final String[] ITEMS = new String[] {"items", "item"};
    protected static final String[] POST_PROCESSOR = new String[] {"post_processors", "post-processors"};
    protected static final String[] VISUAL_RESULT = new String[] {"visual_result", "visual-result"};
    protected static final String[] FUNCTIONS = new String[] {"functions", "function"};
    protected static final String[] CONDITIONS = new String[] {"conditions", "condition"};
    protected static final String[] ALWAYS_REBUILD_RESULT = new String[] {"always_rebuild_result", "always-rebuild-result"};

    protected CustomRecipeResult parseResult(DatapackRecipeResult recipeResult) {
        Item result = CraftEngine.instance().itemManager().build(recipeResult);
        return new CustomRecipeResult(CloneableItem.of(result), recipeResult.count(), null);
    }

    protected CustomRecipeResult parseResult(ConfigValue value) {
        if (value.is(Map.class)) {
            return parseResult(value.getAsSection());
        } else {
            Key id = value.getAsIdentifier();
            Optional<? extends BuildableItem> buildableItem = CraftEngine.instance().itemManager().getBuildableItem(id);
            if (buildableItem.isEmpty()) {
                throw new KnownResourceException("resource.recipe.result.item_not_exist", value.path(), id.asString());
            }
            return new CustomRecipeResult(buildableItem.get(), 1, null);
        }
    }

    protected CustomRecipeResult parseResult(ConfigSection section) {
        Key id = section.getNonNullIdentifier("id");
        int count = section.getInt("count", 1);
        Optional<? extends BuildableItem> buildableItem = CraftEngine.instance().itemManager().getBuildableItem(id);
        if (buildableItem.isEmpty()) {
            throw new KnownResourceException("resource.recipe.result.item_not_exist", section.assemblePath("id"), id.asString());
        }
        List<PostProcessor> processors = section.getList(POST_PROCESSOR, v -> PostProcessors.fromConfig(v.getAsSection()));
        return new CustomRecipeResult(
                buildableItem.get(),
                count,
                processors.isEmpty() ? null : processors.toArray(new PostProcessor[0])
        );
    }

    protected Ingredient parseIngredient(ConfigValue value) {
        int count = 1;

        // 如果是 map 就说明用了count，或是未来的predicate
        ConfigValue itemsValue;
        if (value.is(Map.class)) {
            ConfigSection section = value.getAsSection();
            count = section.getInt("count", 1);
            itemsValue = section.getNonNullValue(ITEMS, ConfigConstants.ARGUMENT_LIST);
        } else {
            itemsValue = value;
        }
        Set<UniqueKey> itemIds = new HashSet<>();
        Set<UniqueKey> minecraftItemIds = new HashSet<>();
        List<IngredientElement> elements = new ArrayList<>();
        ItemManager itemManager = CraftEngine.instance().itemManager();
        itemsValue.forEach(v -> {
            String itemOrTag = v.getAsString();
            if (itemOrTag.charAt(0) == '#') {
                Key tag = Key.of(itemOrTag.substring(1));
                IngredientElement.Tag itemTag = IngredientElement.tag(tag);
                elements.add(itemTag);
                List<UniqueKey> items = itemManager.itemIdsByTag(tag);
                if (items.isEmpty()) {
                    throw new KnownResourceException("resource.recipe.ingredient.invalid_tag", v.path(), itemOrTag);
                }
                itemIds.addAll(items);
                for (UniqueKey uniqueKey : items) {
                    List<UniqueKey> ingredientSubstitutes = itemManager.getIngredientSubstitutes(uniqueKey.key());
                    if (!ingredientSubstitutes.isEmpty()) {
                        itemIds.addAll(ingredientSubstitutes);
                    }
                }
            } else {
                Key itemId = Key.of(itemOrTag);
                elements.add(new IngredientElement.Item(itemId));
                if (itemManager.getBuildableItem(itemId).isEmpty()) {
                    throw new KnownResourceException("resource.recipe.ingredient.item_not_exist", v.path(), itemOrTag);
                }
                itemIds.add(UniqueKey.create(itemId));
                List<UniqueKey> ingredientSubstitutes = itemManager.getIngredientSubstitutes(itemId);
                if (!ingredientSubstitutes.isEmpty()) {
                    itemIds.addAll(ingredientSubstitutes);
                }
            }
        });
        boolean hasCustomItem = false;
        for (UniqueKey holder : itemIds) {
            Optional<ItemDefinition> optionalCustomItem = itemManager.getItemDefinition(holder.key());
            UniqueKey vanillaItem = holder;
            if (optionalCustomItem.isPresent()) {
                ItemDefinition itemDefinition = optionalCustomItem.get();
                if (!itemDefinition.isVanillaItem()) {
                    vanillaItem = UniqueKey.create(itemDefinition.material());
                    hasCustomItem = true;
                }
            }
            minecraftItemIds.add(vanillaItem);
        }
        return Ingredient.of(elements, itemIds, minecraftItemIds, hasCustomItem, count);
    }

    // 解析原版数据包的物品为ingredient
    @Nullable
    protected Ingredient parseVanillaIngredient(List<String> items) {
        Set<UniqueKey> itemIds = new HashSet<>();
        Set<UniqueKey> minecraftItemIds = new HashSet<>();
        ItemManager itemManager = CraftEngine.instance().itemManager();
        List<IngredientElement> elements = new ArrayList<>();

        for (String item : items) {
            if (item.charAt(0) == '#') {
                Key tag = Key.of(item.substring(1));
                elements.add(new IngredientElement.Tag(tag));
                List<UniqueKey> uniqueKeys = itemManager.itemIdsByTag(tag);

                if (uniqueKeys.isEmpty()) {
                    throw new IllegalArgumentException("Unknown or empty item tag: " + tag);
                }

                itemIds.addAll(uniqueKeys);
                for (UniqueKey uniqueKey : uniqueKeys) {
                    List<UniqueKey> ingredientSubstitutes = itemManager.getIngredientSubstitutes(uniqueKey.key());
                    if (!ingredientSubstitutes.isEmpty()) {
                        itemIds.addAll(ingredientSubstitutes);
                    }
                }
            } else {
                Key itemId = Key.of(item);
                elements.add(new IngredientElement.Item(itemId));

                if (itemManager.getBuildableItem(itemId).isEmpty()) {
                    throw new IllegalArgumentException("Unknown item identifier: " + itemId);
                }

                itemIds.add(UniqueKey.create(itemId));
                List<UniqueKey> ingredientSubstitutes = itemManager.getIngredientSubstitutes(itemId);
                if (!ingredientSubstitutes.isEmpty()) {
                    itemIds.addAll(ingredientSubstitutes);
                }
            }
        }

        boolean hasCustomItem = false;
        for (UniqueKey holder : itemIds) {
            Optional<ItemDefinition> optionalCustomItem = itemManager.getItemDefinition(holder.key());
            UniqueKey vanillaItem;

            if (optionalCustomItem.isPresent()) {
                ItemDefinition itemDefinition = optionalCustomItem.get();
                if (itemDefinition.isVanillaItem()) {
                    vanillaItem = holder;
                } else {
                    vanillaItem = UniqueKey.create(itemDefinition.material());
                    hasCustomItem = true;
                }
            } else {
                if (itemManager.isVanillaItem(holder.key())) {
                    vanillaItem = holder;
                } else {
                    throw new IllegalStateException("Invalid item reference (neither custom nor vanilla): " + holder.key());
                }
            }

            if (vanillaItem == UniqueKey.AIR) {
                throw new IllegalArgumentException("Ingredient cannot be air!");
            }

            minecraftItemIds.add(vanillaItem);
        }

        if (itemIds.isEmpty()) {
            return null;
        }
        return Ingredient.of(elements, itemIds, minecraftItemIds, hasCustomItem, 1);
    }
}
