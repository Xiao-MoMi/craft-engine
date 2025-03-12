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

package net.momirealms.craftengine.core.pack.model.rangedisptach;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.LegacyModelPredicate;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class DamageRangeDispatchProperty implements RangeDispatchProperty, LegacyModelPredicate<Float> {
    public static final Factory FACTORY = new Factory();
    private final boolean normalize;

    public DamageRangeDispatchProperty(boolean normalize) {
        this.normalize = normalize;
    }

    @Override
    public Key type() {
        return RangeDispatchProperties.DAMAGE;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        if (!normalize) {
            jsonObject.addProperty("normalize", false);
        }
    }

    @Override
    public String legacyPredicateId(Key material) {
        if (this.normalize) return "damage";
        throw new RuntimeException("Enable 'normalize' option if you want to use 'damage' on 1.21.3 and below");
    }

    @Override
    public Number toLegacyValue(Float value) {
        if (this.normalize) return value;
        throw new RuntimeException("Enable 'normalize' option if you want to use 'damage' on 1.21.3 and below");
    }

    public static class Factory implements RangeDispatchPropertyFactory {

        @Override
        public RangeDispatchProperty create(Map<String, Object> arguments) {
            boolean normalize = (boolean) arguments.getOrDefault("normalize", true);
            return new DamageRangeDispatchProperty(normalize);
        }
    }
}
