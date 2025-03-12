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
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.Map;

public class CustomModelDataRangeDispatchProperty implements RangeDispatchProperty, LegacyModelPredicate<Float> {
    public static final Factory FACTORY = new Factory();
    private final int index;

    public CustomModelDataRangeDispatchProperty(int index) {
        this.index = index;
    }

    @Override
    public Key type() {
        return RangeDispatchProperties.CUSTOM_MODEL_DATA;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        jsonObject.addProperty("index", index);
    }

    @Override
    public String legacyPredicateId(Key material) {
        return "custom_model_data";
    }

    @Override
    public Number toLegacyValue(Float value) {
        return value.intValue();
    }

    public static class Factory implements RangeDispatchPropertyFactory {

        @Override
        public RangeDispatchProperty create(Map<String, Object> arguments) {
            int index = MiscUtils.getAsInt(arguments.getOrDefault("index", 0));
            return new CustomModelDataRangeDispatchProperty(index);
        }
    }
}
