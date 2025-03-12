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
import java.util.Optional;

public class SurvivesExplosionCondition implements LootCondition {
    public static final Factory FACTORY = new Factory();
    private static final SurvivesExplosionCondition INSTANCE = new SurvivesExplosionCondition();

    @Override
    public Key type() {
        return LootConditions.SURVIVES_EXPLOSION;
    }

    @Override
    public boolean test(LootContext lootContext) {
        Optional<Float> radius = lootContext.getOptionalParameter(LootParameters.EXPLOSION_RADIUS);
        if (radius.isPresent()) {
            float f = 1f / radius.get();
            return lootContext.randomSource().nextFloat() < f;
        }
        return true;
    }

    public static class Factory implements LootConditionFactory {
        @Override
        public LootCondition create(Map<String, Object> arguments) {
            return INSTANCE;
        }
    }
}
