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

package net.momirealms.craftengine.core.pack.model.select;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.LegacyModelPredicate;
import net.momirealms.craftengine.core.util.Key;

import java.util.HashMap;
import java.util.Map;

public class TrimMaterialSelectProperty implements SelectProperty, LegacyModelPredicate<String> {
    public static final Factory FACTORY = new Factory();
    private static final Map<String, Float> LEGACY_TRIM_DATA = new HashMap<>();
    static {
        LEGACY_TRIM_DATA.put("minecraft:quartz", 0.1f);
        LEGACY_TRIM_DATA.put("minecraft:iron", 0.2f);
        LEGACY_TRIM_DATA.put("minecraft:netherite", 0.3f);
        LEGACY_TRIM_DATA.put("minecraft:redstone", 0.4f);
        LEGACY_TRIM_DATA.put("minecraft:copper", 0.5f);
        LEGACY_TRIM_DATA.put("minecraft:gold", 0.6f);
        LEGACY_TRIM_DATA.put("minecraft:emerald", 0.7f);
        LEGACY_TRIM_DATA.put("minecraft:diamond", 0.8f);
        LEGACY_TRIM_DATA.put("minecraft:lapis", 0.9f);
        LEGACY_TRIM_DATA.put("minecraft:amethyst", 1.0f);
        // INVALID
        LEGACY_TRIM_DATA.put("minecraft:resin", 0F);
    }

    @Override
    public Key type() {
        return SelectProperties.TRIM_MATERIAL;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
    }

    @Override
    public String legacyPredicateId(Key material) {
        String s = material.toString();
        if (s.contains("helmet") || s.contains("chestplate") || s.contains("leggings") || s.contains("boots")) {
            return "trim";
        }
        return null;
    }

    @Override
    public Number toLegacyValue(String value) {
        Float f = LEGACY_TRIM_DATA.get(value);
        if (f == null) {
            throw new IllegalArgumentException("Invalid trim material '" + value + "'");
        }
        return f;
    }

    public static class Factory implements SelectPropertyFactory {

        @Override
        public SelectProperty create(Map<String, Object> arguments) {
            return new TrimMaterialSelectProperty();
        }
    }
}
