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

package net.momirealms.craftengine.core.item.recipe.vanilla.reader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VanillaRecipeReader1_21_2 extends VanillaRecipeReader1_20_5 {

    @Override
    protected List<String> readSingleIngredient(JsonElement json) {
        if (json.isJsonPrimitive()) {
            return List.of(json.getAsString());
        } else {
            JsonArray array = json.getAsJsonArray();
            List<String> ingredients = new ArrayList<>();
            for (JsonElement element : array) {
                ingredients.add(element.getAsString());
            }
            return ingredients;
        }
    }

    @Override
    protected Map<Character, List<String>> readShapedIngredientMap(JsonObject json) {
        Map<Character, List<String>> ingredients = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            char c = entry.getKey().charAt(0);
            if (entry.getValue().isJsonPrimitive()) {
                ingredients.put(c, List.of(entry.getValue().getAsString()));
            } else if (entry.getValue().isJsonArray()) {
                List<String> list = new ArrayList<>();
                for (JsonElement element : entry.getValue().getAsJsonArray()) {
                    list.add(element.getAsString());
                }
                ingredients.put(c, list);
            }
        }
        return ingredients;
    }

    @Override
    protected List<List<String>> readShapelessIngredients(JsonArray json) {
        List<List<String>> ingredients = new ArrayList<>();
        for (JsonElement element : json) {
            if (element.isJsonPrimitive()) {
                ingredients.add(List.of(element.getAsString()));
            } else if (element.isJsonArray()) {
                List<String> list = new ArrayList<>();
                for (JsonElement inner : element.getAsJsonArray()) {
                    list.add(inner.getAsString());
                }
                ingredients.add(list);
            }
        }
        return ingredients;
    }
}
