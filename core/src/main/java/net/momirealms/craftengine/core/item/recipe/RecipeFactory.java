package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.Map;

public interface RecipeFactory<T> {

    Recipe<T> create(Key id, Map<String, Object> arguments);

    @SuppressWarnings({"unchecked", "rawtypes"})
    default CustomRecipeResult<T> parseResult(Map<String, Object> arguments) {
        Map<String, Object> resultMap = MiscUtils.castToMap(arguments.get("result"), true);
        if (resultMap == null) {
            throw new IllegalArgumentException("result cannot be empty");
        }
        String id = (String) resultMap.get("id");
        if (id == null) {
            throw new IllegalArgumentException("result.id cannot be empty");
        }
        int count = MiscUtils.getAsInt(resultMap.getOrDefault("count", 1));
        return new CustomRecipeResult(
                CraftEngine.instance().itemManager().getBuildableItem(Key.of(id)).orElseThrow(() -> new IllegalArgumentException("Unknown recipe result item id: " + id)),
                count
        );
    }
}
