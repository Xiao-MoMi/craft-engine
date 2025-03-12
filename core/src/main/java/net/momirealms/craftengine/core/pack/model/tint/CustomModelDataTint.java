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
import net.momirealms.craftengine.core.util.MiscUtils;
import org.incendo.cloud.type.Either;

import java.util.List;
import java.util.Map;

public class CustomModelDataTint implements Tint {
    public static final Factory FACTORY = new Factory();
    private final Either<Integer, List<Integer>> value;
    private final int index;

    public CustomModelDataTint(Either<Integer, List<Integer>> value, int index) {
        this.index = index;
        this.value = value;
    }

    @Override
    public Key type() {
        return Tints.CUSTOM_MODEL_DATA;
    }

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type().toString());
        if (index != 0)
            json.addProperty("index", index);
        applyAnyTint(json, value, "default");
        return json;
    }

    public static class Factory implements TintFactory {

        @Override
        public Tint create(Map<String, Object> arguments) {
            Object value = arguments.get("default");
            int index = MiscUtils.getAsInt(arguments.getOrDefault("index", 0));
            return new CustomModelDataTint(parseTintValue(value), index);
        }
    }
}
