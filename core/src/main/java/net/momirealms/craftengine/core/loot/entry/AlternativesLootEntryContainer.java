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

import net.momirealms.craftengine.core.loot.condition.LootCondition;
import net.momirealms.craftengine.core.loot.condition.LootConditions;
import net.momirealms.craftengine.core.util.Key;

import java.util.*;

public class AlternativesLootEntryContainer<T> extends AbstractCompositeLootEntryContainer<T> {
    public static final Factory<?> FACTORY = new Factory<>();

    protected AlternativesLootEntryContainer(List<LootCondition> conditions, List<LootEntryContainer<T>> children) {
        super(conditions, children);
    }

    @Override
    protected LootEntryContainer<T> compose(List<? extends LootEntryContainer<T>> children) {
        return switch (children.size()) {
            case 0 -> LootEntryContainer.alwaysFalse();
            case 1 -> children.get(0);
            case 2 -> children.get(0).or(children.get(1));
            default -> (context, choiceConsumer) -> {
                for (LootEntryContainer<T> child : children) {
                    if (child.expand(context, choiceConsumer)) {
                        return true;
                    }
                }
                return false;
            };
        };
    }

    @Override
    public Key type() {
        return LootEntryContainers.ALTERNATIVES;
    }

    public static class Factory<A> implements LootEntryContainerFactory<A> {
        @SuppressWarnings("unchecked")
        @Override
        public LootEntryContainer<A> create(Map<String, Object> arguments) {
            List<LootEntryContainer<A>> containers = Optional.ofNullable(arguments.get("children"))
                    .map(it -> (List<LootEntryContainer<A>>) new ArrayList<LootEntryContainer<A>>(LootEntryContainers.fromMapList((List<Map<String, Object>>) it)))
                    .orElse(Collections.emptyList());
            List<LootCondition> conditions = Optional.ofNullable(arguments.get("conditions"))
                    .map(it -> LootConditions.fromMapList((List<Map<String, Object>>) it))
                    .orElse(Collections.emptyList());
            return new AlternativesLootEntryContainer<>(conditions, containers);
        }
    }
}
