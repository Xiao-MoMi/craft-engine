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
import net.momirealms.craftengine.core.loot.condition.LootConditions;
import net.momirealms.craftengine.core.loot.number.NumberProvider;
import net.momirealms.craftengine.core.loot.number.NumberProviders;
import net.momirealms.craftengine.core.util.Key;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SetCountFunction<T> extends AbstractLootConditionalFunction<T> {
    public static final Factory<?> FACTORY = new Factory<>();

    private final NumberProvider value;
    private final boolean add;

    public SetCountFunction(List<LootCondition> conditions, NumberProvider value, boolean add) {
        super(conditions);
        this.value = value;
        this.add = add;
    }

    @Override
    public Key type() {
        return LootFunctions.SET_COUNT;
    }

    @Override
    protected Item<T> applyInternal(Item<T> item, LootContext context) {
        int amount = this.add ? item.count() : 0;
        item.count(amount + this.value.getInt(context));
        return item;
    }

    public static class Factory<A> implements LootFunctionFactory<A> {
        @SuppressWarnings("unchecked")
        @Override
        public LootFunction<A> create(Map<String, Object> arguments) {
            Object value = arguments.get("count");
            boolean add = (boolean) arguments.getOrDefault("add", true);
            List<LootCondition> conditions = Optional.ofNullable(arguments.get("conditions"))
                    .map(it -> LootConditions.fromMapList((List<Map<String, Object>>) it))
                    .orElse(Collections.emptyList());
            return new SetCountFunction<>(conditions, NumberProviders.fromObject(value), add);
        }
    }
}
