/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
