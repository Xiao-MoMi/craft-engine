package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.locale.LocalizedResourceConfigException;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;

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
                count
        );
    }
}
