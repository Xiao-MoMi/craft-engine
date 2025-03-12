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

import net.momirealms.craftengine.core.util.Key;

public abstract class CustomCraftingTableRecipe<T> extends AbstractRecipe<T> {
    protected final CraftingRecipeCategory category;

    protected CustomCraftingTableRecipe(Key id, CraftingRecipeCategory category, String group, CustomRecipeResult<T> result) {
        super(id, group, result);
        this.category = category;
    }

    public CraftingRecipeCategory category() {
        return category;
    }
}
