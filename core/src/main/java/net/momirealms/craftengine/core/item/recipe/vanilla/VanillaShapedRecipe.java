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

import net.momirealms.craftengine.core.item.recipe.CraftingRecipeCategory;

import java.util.List;
import java.util.Map;

public class VanillaShapedRecipe extends VanillaCraftingRecipe {
    private final String[] pattern;
    private final Map<Character, List<String>> key;

    public VanillaShapedRecipe(CraftingRecipeCategory category,
                               String group,
                               Map<Character, List<String>> key,
                               String[] pattern,
                               RecipeResult result) {
        super(category, group, result);
        this.key = key;
        this.pattern = pattern;
    }

    public Map<Character, List<String>> ingredients() {
        return key;
    }

    public String[] pattern() {
        return pattern;
    }
}
