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

package net.momirealms.craftengine.core.item.recipe.vanilla;

import net.momirealms.craftengine.core.item.recipe.CookingRecipeCategory;

import java.util.List;

public class VanillaCampfireRecipe extends VanillaCookingRecipe {

    public VanillaCampfireRecipe(CookingRecipeCategory category, String group, RecipeResult result, List<String> ingredient, float experience, int cookingTime) {
        super(category, group, result, ingredient, experience, cookingTime);
    }
}
