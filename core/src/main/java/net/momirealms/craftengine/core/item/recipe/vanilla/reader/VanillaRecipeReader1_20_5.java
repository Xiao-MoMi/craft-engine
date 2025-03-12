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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.recipe.vanilla.RecipeResult;
import org.jetbrains.annotations.NotNull;

public class VanillaRecipeReader1_20_5 extends VanillaRecipeReader1_20 {

    @Override
    protected @NotNull RecipeResult readCraftingResult(JsonObject object) {
        String item = object.get("id").getAsString();
        JsonObject components = object.has("components") ? object.getAsJsonObject("components") : null;
        int count = object.has("count") ? object.get("count").getAsInt() : 1;
        return new RecipeResult(item, count, components);
    }

    @NotNull
    @Override
    protected RecipeResult readCookingResult(JsonElement object) {
        return readCraftingResult(object.getAsJsonObject());
    }

    @NotNull
    @Override
    protected RecipeResult readStoneCuttingResult(JsonObject json) {
        return readCraftingResult(json.getAsJsonObject("result"));
    }
}
