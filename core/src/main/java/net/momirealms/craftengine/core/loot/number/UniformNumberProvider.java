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

public class UniformNumberProvider implements NumberProvider {
    public static final Factory FACTORY = new Factory();
    private final NumberProvider min;
    private final NumberProvider max;

    public UniformNumberProvider(NumberProvider min, NumberProvider max) {
        this.min = min;
        this.max = max;
    }

    @Override
    public int getInt(LootContext context) {
        return context.randomSource().nextInt(this.min.getInt(context), this.max.getInt(context));
    }

    @Override
    public float getFloat(LootContext context) {
        return context.randomSource().nextFloat(this.min.getFloat(context), this.max.getFloat(context));
    }

    @Override
    public Key type() {
        return NumberProviders.UNIFORM;
    }

    public static class Factory implements NumberProviderFactory {
        @Override
        public NumberProvider create(Map<String, Object> arguments) {
            Object min = arguments.getOrDefault("min", 1);
            Object max = arguments.getOrDefault("max", 1);
            return new UniformNumberProvider(NumberProviders.fromObject(min), NumberProviders.fromObject(max));
        }
    }
}
