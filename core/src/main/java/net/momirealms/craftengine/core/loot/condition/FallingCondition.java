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
import net.momirealms.craftengine.core.loot.parameter.LootParameters;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class FallingCondition implements LootCondition {
    public static final Factory FACTORY = new Factory();
    public static final FallingCondition INSTANCE = new FallingCondition();

    @Override
    public Key type() {
        return LootConditions.FALLING_BLOCK;
    }

    @Override
    public boolean test(LootContext lootContext) {
        return lootContext.getOptionalParameter(LootParameters.FALLING_BLOCK).orElse(false);
    }

    public static class Factory implements LootConditionFactory {
        @Override
        public LootCondition create(Map<String, Object> arguments) {
            return INSTANCE;
        }
    }
}
