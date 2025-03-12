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

package net.momirealms.craftengine.core.pack.model.tint;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import org.incendo.cloud.type.Either;

import java.util.List;
import java.util.Map;

public class SimpleDefaultTint implements Tint {
    public static final Factory FACTORY = new Factory();
    private final Either<Integer, List<Integer>> value;
    private final Key type;

    public SimpleDefaultTint(Either<Integer, List<Integer>> value, Key type) {
        this.value = value;
        this.type = type;
    }

    @Override
    public Key type() {
        return type;
    }

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type().toString());
        applyAnyTint(json, value, "default");
        return json;
    }

    public static class Factory implements TintFactory {

        @Override
        public Tint create(Map<String, Object> arguments) {
            Object value = arguments.get("default");
            Key type = Key.of(arguments.get("type").toString());
            return new SimpleDefaultTint(parseTintValue(value), type);
        }
    }
}
