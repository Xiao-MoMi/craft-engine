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

package net.momirealms.craftengine.core.pack.model;

import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.generator.ModelGeneration;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;
import java.util.Map;

public class EmptyItemModel implements ItemModel {
    public static final Factory FACTORY = new Factory();
    private static final EmptyItemModel INSTANCE = new EmptyItemModel();

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type().toString());
        return json;
    }

    @Override
    public Key type() {
        return ItemModels.EMPTY;
    }

    @Override
    public List<ModelGeneration> modelsToGenerate() {
        return List.of();
    }

    public static class Factory implements ItemModelFactory {

        @Override
        public ItemModel create(Map<String, Object> arguments) {
            return INSTANCE;
        }
    }
}
