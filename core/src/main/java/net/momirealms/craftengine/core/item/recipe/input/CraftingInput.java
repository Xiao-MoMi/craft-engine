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

package net.momirealms.craftengine.core.item.recipe.input;

import net.momirealms.craftengine.core.item.recipe.OptimizedIDItem;
import net.momirealms.craftengine.core.item.recipe.RecipeFinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CraftingInput<T> implements RecipeInput {
    private final int width;
    private final int height;
    private final List<OptimizedIDItem<T>> items;
    private final int ingredientCount;
    private final RecipeFinder finder = new RecipeFinder();

    private CraftingInput(int width, int height, List<OptimizedIDItem<T>> items) {
        this.height = height;
        this.width = width;
        this.items = items;
        int i = 0;
        for (OptimizedIDItem<T> item : items) {
            if (!item.isEmpty()) {
                i++;
                this.finder.addInput(item);
            }
        }
        this.ingredientCount = i;
    }

    public RecipeFinder finder() {
        return finder;
    }

    public static <T> CraftingInput<T> of(int width, int height, List<OptimizedIDItem<T>> stacks) {
        if (width <= 0 || height <= 0) {
            return new CraftingInput<>(0, 0, Collections.emptyList());
        }

        int minCol = width;
        int maxCol = -1;
        int minRow = height;
        int maxRow = -1;

        for (int index = 0; index < width * height; index++) {
            OptimizedIDItem<T> item = stacks.get(index);
            if (!item.isEmpty()) {
                int row = index / width;
                int col = index % width;
                minCol = Math.min(minCol, col);
                maxCol = Math.max(maxCol, col);
                minRow = Math.min(minRow, row);
                maxRow = Math.max(maxRow, row);
            }
        }

        if (maxCol < minCol) {
            return new CraftingInput<>(0, 0, Collections.emptyList());
        }

        int newWidth = maxCol - minCol + 1;
        int newHeight = maxRow - minRow + 1;

        if (newWidth == width && newHeight == height) {
            return new CraftingInput<>(width, height, stacks);
        }

        List<OptimizedIDItem<T>> trimmed = new ArrayList<>(newWidth * newHeight);
        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                int originalIndex = col + row * width;
                trimmed.add(stacks.get(originalIndex));
            }
        }

        return new CraftingInput<>(newWidth, newHeight, trimmed);
    }

    public int ingredientCount() {
        return ingredientCount;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int size() {
        return items.size();
    }

    public OptimizedIDItem<T> getItem(int x, int y) {
        return this.items.get(x + y * width);
    }

    public OptimizedIDItem<T> getItem(int index) {
        return this.items.get(index);
    }
}
