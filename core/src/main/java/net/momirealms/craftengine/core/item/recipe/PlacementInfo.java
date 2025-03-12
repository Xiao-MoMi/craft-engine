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

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

public class PlacementInfo<T> {
    private final List<Ingredient<T>> ingredients;
    private final IntList slotsToIngredientIndex;

    private PlacementInfo(List<Ingredient<T>> ingredients, IntList placementSlots) {
        this.ingredients = ingredients;
        this.slotsToIngredientIndex = placementSlots;
    }

    public static <T> PlacementInfo<T> create(List<Ingredient<T>> ingredients) {
        int i = ingredients.size();
        IntList intList = new IntArrayList(i);
        for (int j = 0; j < i; j++) {
            Ingredient<T> ingredient = ingredients.get(j);
            if (ingredient.isEmpty()) {
                return new PlacementInfo<>(List.of(), IntList.of());
            }
            intList.add(j);
        }
        return new PlacementInfo<>(ingredients, intList);
    }

    public List<Ingredient<T>> ingredients() {
        return this.ingredients;
    }

    public boolean isImpossibleToPlace() {
        return this.slotsToIngredientIndex.isEmpty();
    }
}
