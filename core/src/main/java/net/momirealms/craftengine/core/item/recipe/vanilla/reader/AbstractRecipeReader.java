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
import net.momirealms.craftengine.core.item.recipe.CookingRecipeCategory;
import net.momirealms.craftengine.core.item.recipe.CraftingRecipeCategory;
import net.momirealms.craftengine.core.item.recipe.vanilla.VanillaRecipeReader;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public abstract class AbstractRecipeReader implements VanillaRecipeReader {

    protected String[] readPattern(JsonObject object) {
        JsonArray pattern = object.getAsJsonArray("pattern");
        List<String> patternList = new ArrayList<>();
        for (JsonElement element : pattern) {
            patternList.add(element.getAsString());
        }
        return patternList.toArray(new String[0]);
    }

    @Nullable
    protected String readGroup(JsonObject object) {
        return object.has("group") ? object.get("group").getAsString() : null;
    }

    @Nullable
    protected CraftingRecipeCategory readCraftingCategory(JsonObject object) {
        return object.has("category") ? CraftingRecipeCategory.valueOf(object.get("category").getAsString().toUpperCase(Locale.ENGLISH)) : null;
    }

    @Nullable
    protected CookingRecipeCategory readCookingCategory(JsonObject object) {
        return object.has("category") ? CookingRecipeCategory.valueOf(object.get("category").getAsString().toUpperCase(Locale.ENGLISH)) : null;
    }

    protected float readExperience(JsonObject object) {
        return object.has("experience") ? object.get("experience").getAsFloat() : 0;
    }

    protected int readCookingTime(JsonObject object) {
        return object.has("cookingtime") ? object.get("cookingtime").getAsInt() : 200;
    }
}
