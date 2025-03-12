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

import net.momirealms.craftengine.core.item.recipe.input.RecipeInput;
import net.momirealms.craftengine.core.item.recipe.input.SingleItemInput;
import net.momirealms.craftengine.core.util.Key;

public abstract class CustomCookingRecipe<T> extends AbstractRecipe<T> {
    protected final CookingRecipeCategory category;
    protected final Ingredient<T> ingredient;
    protected final float experience;
    protected final int cookingTime;

    protected CustomCookingRecipe(Key id,
                                  CookingRecipeCategory category,
                                  String group,
                                  Ingredient<T> ingredient,
                                  int cookingTime,
                                  float experience,
                                  CustomRecipeResult<T> result) {
        super(id, group, result);
        this.category = category;
        this.ingredient = ingredient;
        this.experience = experience;
        this.cookingTime = cookingTime;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean matches(RecipeInput input) {
        return this.ingredient.test(((SingleItemInput<T>) input).input());
    }

    public CookingRecipeCategory category() {
        return category;
    }

    public Ingredient<T> ingredient() {
        return ingredient;
    }

    public CustomRecipeResult<T> result() {
        return result;
    }

    public float experience() {
        return experience;
    }

    public int cookingTime() {
        return cookingTime;
    }
}
