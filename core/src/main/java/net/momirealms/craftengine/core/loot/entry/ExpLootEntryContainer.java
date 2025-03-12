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

import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.loot.condition.LootCondition;
import net.momirealms.craftengine.core.loot.condition.LootConditions;
import net.momirealms.craftengine.core.loot.number.NumberProvider;
import net.momirealms.craftengine.core.loot.number.NumberProviders;
import net.momirealms.craftengine.core.loot.parameter.LootParameters;
import net.momirealms.craftengine.core.util.Key;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class ExpLootEntryContainer<T> extends AbstractLootEntryContainer<T> {
    public static final Factory<?> FACTORY = new Factory<>();
    private final NumberProvider value;

    protected ExpLootEntryContainer(NumberProvider value, List<LootCondition> conditions) {
        super(conditions);
        this.value = value;
    }

    @Override
    public Key type() {
        return LootEntryContainers.EXP;
    }

    @Override
    public boolean expand(LootContext context, Consumer<LootEntry<T>> choiceConsumer) {
        if (super.test(context)) {
            context.getOptionalParameter(LootParameters.WORLD)
                    .ifPresent(it -> context.getOptionalParameter(LootParameters.LOCATION).ifPresent(loc -> it.dropExp(loc.toCenter(), value.getInt(context))));
            return true;
        } else {
            return false;
        }
    }

    public static class Factory<A> implements LootEntryContainerFactory<A> {
        @SuppressWarnings("unchecked")
        @Override
        public LootEntryContainer<A> create(Map<String, Object> arguments) {
            Object value = arguments.get("count");
            if (value == null) {
                throw new IllegalArgumentException("count can not be null");
            }
            List<LootCondition> conditions = Optional.ofNullable(arguments.get("conditions"))
                    .map(it -> LootConditions.fromMapList((List<Map<String, Object>>) it))
                    .orElse(Collections.emptyList());
            return new ExpLootEntryContainer<>(NumberProviders.fromObject(value), conditions);
        }
    }
}
