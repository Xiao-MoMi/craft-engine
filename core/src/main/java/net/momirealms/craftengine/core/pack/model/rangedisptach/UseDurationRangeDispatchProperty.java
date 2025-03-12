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
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.pack.model.LegacyModelPredicate;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public class UseDurationRangeDispatchProperty implements RangeDispatchProperty, LegacyModelPredicate<Float> {
    public static final Factory FACTORY = new Factory();
    private final boolean remaining;

    public UseDurationRangeDispatchProperty(boolean remaining) {
        this.remaining = remaining;
    }

    @Override
    public Key type() {
        return RangeDispatchProperties.USE_DURATION;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        if (remaining) {
            jsonObject.addProperty("remaining", true);
        }
    }

    @Override
    public String legacyPredicateId(Key material) {
        if (material.equals(ItemKeys.BOW)) return "pull";
        return null;
    }

    @Override
    public Number toLegacyValue(Float value) {
        return value;
    }

    public static class Factory implements RangeDispatchPropertyFactory {

        @Override
        public RangeDispatchProperty create(Map<String, Object> arguments) {
            boolean remaining = (boolean) arguments.getOrDefault("remaining", false);
            return new UseDurationRangeDispatchProperty(remaining);
        }
    }
}
