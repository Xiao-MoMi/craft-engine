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

import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;

public class RecipeFinder {
    private final StackedContents<Holder<Key>> stackedContents = new StackedContents<>();

    public <T> void addInput(OptimizedIDItem<T> item) {
        if (!item.isEmpty()) {
            this.stackedContents.add(item.id(), 1);
        }
    }

    public <T> boolean canCraft(CustomShapelessRecipe<T> recipe) {
        PlacementInfo<T> placementInfo = recipe.placementInfo();
        return !placementInfo.isImpossibleToPlace() && canCraft(placementInfo.ingredients());
    }

    private boolean canCraft(List<? extends StackedContents.IngredientInfo<Holder<Key>>> rawIngredients) {
        return this.stackedContents.tryPick(rawIngredients);
    }
}
