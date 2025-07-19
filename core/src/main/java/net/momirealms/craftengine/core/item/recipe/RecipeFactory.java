package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.UniqueKey;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RecipeFactory<T> {

    Recipe<T> create(Key id, Map<String, Object> arguments);

    @SuppressWarnings({"unchecked", "rawtypes"})
    default CustomRecipeResult<T> parseResult(Map<String, Object> arguments) {
        Map<String, Object> resultMap = MiscUtils.castToMap(arguments.get("result"), true);
        if (resultMap == null) {
            throw new LocalizedResourceConfigException("warning.config.recipe.missing_result");
        }
        String id = ResourceConfigUtils.requireNonEmptyStringOrThrow(resultMap.get("id"), "warning.config.recipe.result.missing_id");
        int count = ResourceConfigUtils.getAsInt(resultMap.getOrDefault("count", 1), "count");
        return new CustomRecipeResult(
                CraftEngine.instance().itemManager().getBuildableItem(Key.of(id)).orElseThrow(
                        () -> new LocalizedResourceConfigException("warning.config.recipe.invalid_result", id)),
                count,
                null
        );
    }

    default Ingredient<T> toIngredient(List<String> items) {
        Set<UniqueKey> holders = new HashSet<>();
        for (String item : items) {
            if (item.charAt(0) == '#') {
                holders.addAll(CraftEngine.instance().itemManager().tagToItems(Key.of(item.substring(1))));
            } else {
                holders.add(UniqueKey.create(Key.of(item)));
            }
        }
        return holders.isEmpty() ? null : Ingredient.of(holders);
    }
}
