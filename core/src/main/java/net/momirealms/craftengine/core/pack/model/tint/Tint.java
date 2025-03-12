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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.Key;
import org.incendo.cloud.type.Either;

import java.util.List;
import java.util.function.Supplier;

public interface Tint extends Supplier<JsonObject> {

    Key type();

    default void applyAnyTint(JsonObject json, Either<Integer, List<Integer>> value, String key) {
        if (value.primary().isPresent()) {
            json.addProperty(key, value.primary().get());
        } else {
            List<Integer> list = value.fallback().get();
            if (list.size() != 3) {
                throw new RuntimeException("Invalid tint value list size: " + list.size() + " which is expected to be 3");
            }
            JsonArray array = new JsonArray();
            for (int i : list) {
                array.add(i);
            }
            json.add(key, array);
        }
    }
}
