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

import java.util.*;
import java.util.function.Predicate;

public class Ingredient<T> implements Predicate<OptimizedIDItem<T>>, StackedContents.IngredientInfo<Holder<Key>> {
    private final List<Holder<Key>> items;

    public Ingredient(List<Holder<Key>> items) {
        this.items = items;
    }

    public static <T> boolean isInstance(Optional<Ingredient<T>> optionalIngredient, OptimizedIDItem<T> stack) {
        return optionalIngredient.map((ingredient) -> ingredient.test(stack))
                .orElseGet(stack::isEmpty);
    }

    public static <T> Ingredient<T> of(List<Holder<Key>> items) {
        return new Ingredient<>(items);
    }

    public static <T> Ingredient<T> of(Set<Holder<Key>> items) {
        return new Ingredient<>(new ArrayList<>(items));
    }

    @Override
    public boolean test(OptimizedIDItem<T> optimizedIDItem) {
        for (Holder<Key> item : this.items()) {
            if (optimizedIDItem.is(item)) {
                return true;
            }
        }
        return false;
    }

    public List<Holder<Key>> items() {
        return this.items;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ");
        for (Holder<Key> item : this.items()) {
            joiner.add(item.toString());
        }
        return "Ingredient: [" + joiner + "]";
    }

    public boolean isEmpty() {
        return this.items().isEmpty();
    }

    @Override
    public boolean acceptsItem(Holder<Key> entry) {
        return this.items.contains(entry);
    }
}


