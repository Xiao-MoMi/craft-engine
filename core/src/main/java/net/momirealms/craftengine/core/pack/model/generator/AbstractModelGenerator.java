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

package net.momirealms.craftengine.core.pack.model.generator;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractModelGenerator implements ModelGenerator {
    protected final CraftEngine plugin;
    protected final Map<Key, ModelGeneration> modelsToGenerate = new HashMap<>();

    public AbstractModelGenerator(CraftEngine plugin) {
        this.plugin = plugin;
    }

    @Override
    public Collection<ModelGeneration> modelsToGenerate() {
        return this.modelsToGenerate.values();
    }

    @Override
    public void clearModelsToGenerate() {
        this.modelsToGenerate.clear();
    }

    @Override
    public void prepareModelGeneration(ModelGeneration model) {
        ModelGeneration generation = this.modelsToGenerate.get(model.path());
        if (generation != null) {
            if (generation.equals(model)) {
                return;
            }
            this.plugin.logger().severe("Two or more configurations attempt to generate different json models with the same path: " + model.path());
            return;
        }
        this.modelsToGenerate.put(model.path(), model);
    }
}
