package net.momirealms.craftengine.core.item.recipe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.*;
import net.momirealms.craftengine.core.item.recipe.predicate.AllOfDataComponentPredicate;
import net.momirealms.craftengine.core.item.recipe.predicate.DataComponentPredicate;
import net.momirealms.craftengine.core.item.recipe.predicate.DataComponentPredicates;
import net.momirealms.craftengine.core.item.recipe.reader.*;
import net.momirealms.craftengine.core.item.recipe.result.CustomRecipeResult;
import net.momirealms.craftengine.core.item.recipe.result.PostProcessor;
import net.momirealms.craftengine.core.item.recipe.result.PostProcessors;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.*;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.UniqueKey;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.jetbrains.annotations.NotNull;
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
    protected static final String[] SHOW_NOTIFICATIONS = ConfigKeys.of("show_notification");
    protected static final String[] INGREDIENTS = ConfigKeys.of("ingredient(s)");
    protected static final String[] EXP = ConfigKeys.of("exp|experience");
    protected static final String[] ITEMS = ConfigKeys.of("item(s)");
    protected static final String[] TRANSFORM_PROCESSOR = ConfigKeys.of("transform_processors|post_processors");
    protected static final String[] VISUAL_RESULT = ConfigKeys.of("visual_result");
    protected static final String[] FUNCTIONS = ConfigKeys.of("function(s)");
    protected static final String[] CONDITIONS = ConfigKeys.of("condition(s)");
    protected static final String[] ALWAYS_REBUILD_RESULT = ConfigKeys.of("always_rebuild_result");

    protected CustomRecipeResult parseResult(DatapackRecipeResult recipeResult) {
        Item result = CraftEngine.instance().itemManager().build(recipeResult);
        // 产出物品不存在时（例如 mod 用条件过滤掉的联动配方），冶炼类配方在同步给客户端时
        // 会在 ItemStack.STREAM_CODEC 抛 "Empty ItemStack not allowed"，直接打断玩家进服
        if (result.isEmpty()) {
            throw new IllegalArgumentException("Non-existent recipe result: " + recipeResult.id());
        }
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
        List<PostProcessor> processors = section.getList(TRANSFORM_PROCESSOR, v -> PostProcessors.fromConfig(v.getAsSection()));
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
        DataComponentPredicate predicate = null;
        if (value.is(Map.class)) {
            ConfigSection section = value.getAsSection();
            count = section.getInt("count", 1);
            itemsValue = section.getNonNullValue(ITEMS, ConfigConstants.ARGUMENT_LIST);
            List<DataComponentPredicate> predicates = section.getSectionList("predicate", DataComponentPredicates::fromConfig);
            if (!predicates.isEmpty()) {
                predicate = predicates.size() == 1 ? predicates.getFirst() : new AllOfDataComponentPredicate(predicates);
            }
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
        return Ingredient.of(elements, itemIds, minecraftItemIds, hasCustomItem, count, predicate);
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
        return Ingredient.of(elements, itemIds, minecraftItemIds, hasCustomItem);
    }

    // 原生读取器只认得字符串和数组，NeoForge 的扩展原料类型会被直接丢弃
    @NotNull
    protected Ingredient parseNeoForgeIngredient(JsonObject json) {
        List<String> items = new ArrayList<>(resolveIngredientItems(json));
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Empty NeoForge ingredient: " + json);
        }
        Ingredient ingredient = parseVanillaIngredient(items);
        if (ingredient == null) {
            throw new IllegalArgumentException("Empty NeoForge ingredient: " + json);
        }
        return ingredient;
    }

    private Set<String> resolveIngredientItems(JsonElement element) {
        if (element == null) {
            throw new IllegalArgumentException("Missing NeoForge ingredient element");
        }
        if (element.isJsonPrimitive()) {
            String value = element.getAsString();
            return value.startsWith("#") ? expandItemTag(value.substring(1)) : new LinkedHashSet<>(List.of(value));
        }
        if (element.isJsonArray()) {
            Set<String> items = new LinkedHashSet<>();
            for (JsonElement child : element.getAsJsonArray()) {
                items.addAll(resolveIngredientItems(child));
            }
            return items;
        }
        JsonObject json = element.getAsJsonObject();
        if (json.has("item")) {
            return new LinkedHashSet<>(List.of(json.get("item").getAsString()));
        }
        if (json.has("tag")) {
            return expandItemTag(json.get("tag").getAsString());
        }
        if (!json.has("type")) {
            throw new IllegalArgumentException("Unsupported ingredient: " + json);
        }
        String type = json.get("type").getAsString();
        switch (type) {
            case "neoforge:difference" -> {
                Set<String> base = resolveIngredientItems(json.get("base"));
                base.removeAll(resolveIngredientItems(json.get("subtracted")));
                return base;
            }
            case "neoforge:compound" -> {
                return resolveIngredientItems(json.has("children") ? json.get("children") : json.get("ingredients"));
            }
            case "neoforge:intersection" -> {
                Set<String> items = null;
                for (JsonElement child : json.getAsJsonArray("children")) {
                    Set<String> childItems = resolveIngredientItems(child);
                    if (items == null) {
                        items = childItems;
                    } else {
                        items.retainAll(childItems);
                    }
                }
                return items == null ? Collections.emptySet() : items;
            }
            default -> throw new IllegalArgumentException("Unsupported ingredient type: " + type);
        }
    }

    private Set<String> expandItemTag(String tag) {
        Key key = Key.of(tag);
        List<UniqueKey> items = CraftEngine.instance().itemManager().itemIdsByTag(key);
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Unknown or empty item tag: " + key);
        }
        Set<String> result = new LinkedHashSet<>(items.size());
        for (UniqueKey item : items) {
            result.add(item.toString());
        }
        return result;
    }
}
