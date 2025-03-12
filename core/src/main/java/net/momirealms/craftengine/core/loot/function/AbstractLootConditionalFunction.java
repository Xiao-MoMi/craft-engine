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
import net.momirealms.craftengine.core.loot.condition.LootCondition;
import net.momirealms.craftengine.core.util.MCUtils;

import java.util.List;
import java.util.function.Predicate;

public abstract class AbstractLootConditionalFunction<T> implements LootFunction<T> {
    protected final List<LootCondition> predicates;
    private final Predicate<LootContext> compositePredicates;

    public AbstractLootConditionalFunction(List<LootCondition> predicates) {
        this.predicates = predicates;
        this.compositePredicates = MCUtils.allOf(predicates);
    }

    @Override
    public Item<T> apply(Item<T> item, LootContext lootContext) {
        return this.compositePredicates.test(lootContext) ? this.applyInternal(item, lootContext) : item;
    }

    protected abstract Item<T> applyInternal(Item<T> item, LootContext context);
}
