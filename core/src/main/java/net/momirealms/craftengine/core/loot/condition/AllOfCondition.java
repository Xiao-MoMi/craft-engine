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

package net.momirealms.craftengine.core.loot.condition;

import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Map;

public class AllOfCondition implements LootCondition {
    public static final Factory FACTORY = new Factory();
    private final List<LootCondition> conditions;

    public AllOfCondition(List<LootCondition> conditions) {
        this.conditions = conditions;
    }

    @Override
    public Key type() {
        return LootConditions.ALL_OF;
    }

    @Override
    public boolean test(LootContext lootContext) {
        for (LootCondition condition : conditions) {
            if (!condition.test(lootContext)) {
                return false;
            }
        }
        return true;
    }

    public static class Factory implements LootConditionFactory {
        @SuppressWarnings("unchecked")
        @Override
        public LootCondition create(Map<String, Object> arguments) {
            List<Map<String, Object>> terms = (List<Map<String, Object>>) arguments.get("terms");
            return new AllOfCondition(LootConditions.fromMapList(terms));
        }
    }
}
