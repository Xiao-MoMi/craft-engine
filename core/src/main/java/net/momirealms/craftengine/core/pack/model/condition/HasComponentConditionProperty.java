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

package net.momirealms.craftengine.core.pack.model.condition;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;
import java.util.Objects;

public class HasComponentConditionProperty implements ConditionProperty {
    public static final Factory FACTORY = new Factory();
    private final String component;
    private final boolean ignoreDefault;

    public HasComponentConditionProperty(String component, boolean ignoreDefault) {
        this.component = component;
        this.ignoreDefault = ignoreDefault;
    }

    @Override
    public Key type() {
        return ConditionProperties.HAS_COMPONENT;
    }

    @Override
    public void accept(JsonObject jsonObject) {
        jsonObject.addProperty("property", type().toString());
        jsonObject.addProperty("component", component);
        if (ignoreDefault) {
            jsonObject.addProperty("ignore_default", true);
        }
    }

    public static class Factory implements ConditionPropertyFactory {

        @Override
        public ConditionProperty create(Map<String, Object> arguments) {
            boolean ignoreDefault = (boolean) arguments.getOrDefault("ignore-default", false);
            String component = Objects.requireNonNull(arguments.get("component"), "component").toString();
            return new HasComponentConditionProperty(component, ignoreDefault);
        }
    }
}
