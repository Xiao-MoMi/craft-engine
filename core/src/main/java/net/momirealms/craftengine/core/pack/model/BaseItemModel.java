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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.model.generator.ModelGeneration;
import net.momirealms.craftengine.core.pack.model.tint.Tint;
import net.momirealms.craftengine.core.pack.model.tint.Tints;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class BaseItemModel implements ItemModel {
    public static final Factory FACTORY = new Factory();
    private final String path;
    private final List<Tint> tints;
    private final ModelGeneration modelGeneration;

    public BaseItemModel(String path, List<Tint> tints, @Nullable ModelGeneration modelGeneration) {
        this.path = path;
        this.tints = tints;
        this.modelGeneration = modelGeneration;
    }

    public List<Tint> tints() {
        return tints;
    }

    public String path() {
        return path;
    }

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type().toString());
        json.addProperty("model", path);
        if (!tints.isEmpty()) {
            JsonArray array = new JsonArray();
            for (Tint tint : tints) {
                array.add(tint.get());
            }
            json.add("tints", array);
        }
        return json;
    }

    @Override
    public Key type() {
        return ItemModels.MODEL;
    }

    @Override
    public List<ModelGeneration> modelsToGenerate() {
        if (this.modelGeneration == null) {
            return List.of();
        } else {
            return List.of(this.modelGeneration);
        }
    }

    public static class Factory implements ItemModelFactory {

        @SuppressWarnings("unchecked")
        @Override
        public ItemModel create(Map<String, Object> arguments) {
            String modelPath = Objects.requireNonNull(arguments.get("path"), "path").toString();
            Map<String, Object> generation = MiscUtils.castToMap(arguments.get("generation"), true);
            ModelGeneration modelGeneration = null;
            if (generation != null) {
                modelGeneration = new ModelGeneration(Key.of(modelPath), generation);
            }
            if (arguments.containsKey("tints")) {
                List<Tint> tints = new ArrayList<>();
                List<Map<String, Object>> tintList = (List<Map<String, Object>>) arguments.get("tints");
                for (Map<String, Object> tint : tintList) {
                    tints.add(Tints.fromMap(tint));
                }
                return new BaseItemModel(modelPath, tints, modelGeneration);
            } else {
                return new BaseItemModel(modelPath, List.of(), modelGeneration);
            }
        }
    }
}
