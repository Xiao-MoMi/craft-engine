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

package net.momirealms.craftengine.core.loot.number;

import net.momirealms.craftengine.core.loot.LootContext;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class FixedNumberProvider implements NumberProvider {
    public static final Factory FACTORY = new Factory();
    private final float value;

    public FixedNumberProvider(float value) {
        this.value = value;
    }

    @Override
    public float getFloat(LootContext context) {
        return this.value;
    }

    @Override
    public Key type() {
        return NumberProviders.FIXED;
    }

    public static class Factory implements NumberProviderFactory {
        @Override
        public NumberProvider create(Map<String, Object> arguments) {
            Number value = (Number) arguments.get("value");
            return new FixedNumberProvider(value.floatValue());
        }
    }
}
