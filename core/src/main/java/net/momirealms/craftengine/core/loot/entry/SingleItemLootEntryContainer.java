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

package net.momirealms.craftengine.core.loot.entry;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.condition.LootCondition;
import net.momirealms.craftengine.core.loot.condition.LootConditions;
import net.momirealms.craftengine.core.loot.function.LootFunction;
import net.momirealms.craftengine.core.loot.function.LootFunctions;
import net.momirealms.craftengine.core.loot.parameter.LootParameters;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;

import java.util.*;
import java.util.function.Consumer;

public class SingleItemLootEntryContainer<T> extends AbstractSingleLootEntryContainer<T> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final Key item;

    protected SingleItemLootEntryContainer(Key item, List<LootCondition> conditions, List<LootFunction<T>> lootFunctions, int weight, int quality) {
        super(conditions, lootFunctions, weight, quality);
        this.item = item;
    }

    @Override
    public Key type() {
        return LootEntryContainers.ITEM;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void createItem(Consumer<Item<T>> lootConsumer, LootContext context) {
        Item<T> tItem = (Item<T>) CraftEngine.instance().itemManager().createWrappedItem(this.item, context.getOptionalParameter(LootParameters.PLAYER).orElse(null));
        if (tItem != null) {
            lootConsumer.accept(tItem);
        } else {
            CraftEngine.instance().logger().warn("Failed to create item: " + this.item);
        }
    }

    public static class Factory<A> implements LootEntryContainerFactory<A> {
        @SuppressWarnings("unchecked")
        @Override
        public LootEntryContainer<A> create(Map<String, Object> arguments) {
            Key item = Key.from((String) arguments.get("item"));
            int weight = (int) arguments.getOrDefault("weight", 1);
            int quality = (int) arguments.getOrDefault("quality", 0);
            List<LootCondition> conditions = Optional.ofNullable(arguments.get("conditions"))
                    .map(it -> LootConditions.fromMapList((List<Map<String, Object>>) it))
                    .orElse(Collections.emptyList());
            List<LootFunction<A>> functions = Optional.ofNullable(arguments.get("functions"))
                    .map(it -> (List<LootFunction<A>>) new ArrayList<LootFunction<A>>(LootFunctions.fromMapList((List<Map<String, Object>>) it)))
                    .orElse(Collections.emptyList());
            return new SingleItemLootEntryContainer<>(item, conditions, functions, weight, quality);
        }
    }
}
