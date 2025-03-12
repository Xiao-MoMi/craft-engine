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

public class OptimizedIDItem<T> {
    private final T rawItem;
    private final Holder<Key> idHolder;

    public OptimizedIDItem(Holder<Key> idHolder, T rawItem) {
        this.idHolder = idHolder;
        this.rawItem = rawItem;
    }

    public Holder<Key> id() {
        return idHolder;
    }

    public T rawItem() {
        return rawItem;
    }

    public boolean is(Holder<Key> id) {
        return idHolder == id;
    }

    public boolean isEmpty() {
        return idHolder == null;
    }

    @Override
    public String toString() {
        return "OptimizedIDItem{" +
                "idHolder=" + idHolder +
                '}';
    }
}
