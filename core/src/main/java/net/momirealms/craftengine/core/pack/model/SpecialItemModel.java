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
import net.momirealms.craftengine.core.pack.model.special.SpecialModel;
import net.momirealms.craftengine.core.pack.model.special.SpecialModels;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SpecialItemModel implements ItemModel {
    public static final Factory FACTORY = new Factory();
    private final SpecialModel specialModel;
    private final String base;

    public SpecialItemModel(SpecialModel specialModel, String base) {
        this.specialModel = specialModel;
        this.base = base;
    }

    public SpecialModel specialModel() {
        return specialModel;
    }

    public String base() {
        return base;
    }

    @Override
    public JsonObject get() {
        JsonObject json = new JsonObject();
        json.addProperty("type", type().toString());
        json.add("model", specialModel.get());
        json.addProperty("base", base);
        return json;
    }

    @Override
    public Key type() {
        return ItemModels.SPECIAL;
    }

    @Override
    public List<ModelGeneration> modelsToGenerate() {
        return List.of();
    }

    public static class Factory implements ItemModelFactory {

        @Override
        public ItemModel create(Map<String, Object> arguments) {
            String base = Objects.requireNonNull(arguments.get("base")).toString();
            Map<String, Object> model = MiscUtils.castToMap(arguments.get("model"), false);
            return new SpecialItemModel(SpecialModels.fromMap(model), base);
        }
    }
}
