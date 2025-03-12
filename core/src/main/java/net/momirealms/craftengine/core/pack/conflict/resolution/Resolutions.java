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

package net.momirealms.craftengine.core.pack.conflict.resolution;

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public class Resolutions {
    public static final Key RETAIN_MATCHING = Key.of("craftengine:retain_matching");
    public static final Key MERGE_JSON = Key.of("craftengine:merge_json");
    public static final Key CONDITIONAL = Key.of("craftengine:conditional");

    static {
        register(RETAIN_MATCHING, RetainMatchingResolution.FACTORY);
        register(MERGE_JSON, MergeJsonResolution.FACTORY);
        register(CONDITIONAL, ConditionalResolution.FACTORY);
    }

    public static void register(Key key, ResolutionFactory factory) {
        Holder.Reference<ResolutionFactory> holder = ((WritableRegistry<ResolutionFactory>) BuiltInRegistries.RESOLUTION_FACTORY).registerForHolder(new ResourceKey<>(Registries.RESOLUTION_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    public static Resolution fromMap(Map<String, Object> map) {
        String type = (String) map.getOrDefault("type", "empty");
        if (type == null) {
            throw new NullPointerException("path matcher type cannot be null");
        }
        Key key = Key.withDefaultNamespace(type, "craftengine");
        ResolutionFactory factory = BuiltInRegistries.RESOLUTION_FACTORY.getValue(key);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown matcher type: " + type);
        }
        return factory.create(map);
    }
}
