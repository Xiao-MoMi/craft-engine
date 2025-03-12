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

package net.momirealms.craftengine.core.loot.function;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.util.Key;

import java.util.function.BiFunction;
import java.util.function.Consumer;

public interface LootFunction<T> extends BiFunction<Item<T>, LootContext, Item<T>> {

    Key type();

    static <T> Consumer<Item<T>> decorate(BiFunction<Item<T>, LootContext, Item<T>> itemApplier, Consumer<Item<T>> lootConsumer, LootContext context) {
        return item -> lootConsumer.accept(itemApplier.apply(item, context));
    }
}
