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

public abstract class VanillaCookingRecipe extends VanillaRecipe {
    protected final List<String> ingredient;
    protected final CookingRecipeCategory category;
    protected final float experience;
    protected final int cookingTime;

    protected VanillaCookingRecipe(CookingRecipeCategory category, String group, RecipeResult result, List<String> ingredient, float experience, int cookingTime) {
        super(group, result);
        this.ingredient = ingredient;
        this.experience = experience;
        this.cookingTime = cookingTime;
        this.category = category;
    }

    public CookingRecipeCategory category() {
        return category;
    }

    public List<String> ingredient() {
        return ingredient;
    }

    public float experience() {
        return experience;
    }

    public int cookingTime() {
        return cookingTime;
    }
}
