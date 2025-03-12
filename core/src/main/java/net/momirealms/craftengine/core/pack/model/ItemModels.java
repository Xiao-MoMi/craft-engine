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

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public class ItemModels {
    public static final Key EMPTY = Key.of("minecraft:empty");
    public static final Key MODEL = Key.of("minecraft:model");
    public static final Key COMPOSITE = Key.of("minecraft:composite");
    public static final Key CONDITION = Key.of("minecraft:condition");
    public static final Key RANGE_DISPATCH = Key.of("minecraft:range_dispatch");
    public static final Key SELECT = Key.of("minecraft:select");
    public static final Key SPECIAL = Key.of("minecraft:special");

    static {
        register(EMPTY, EmptyItemModel.FACTORY);
        register(COMPOSITE, CompositeItemModel.FACTORY);
        register(MODEL, BaseItemModel.FACTORY);
        register(CONDITION, ConditionItemModel.FACTORY);
        register(SPECIAL, SpecialItemModel.FACTORY);
        register(RANGE_DISPATCH, RangeDispatchItemModel.FACTORY);
        register(SELECT, SelectItemModel.FACTORY);
    }

    public static void register(Key key, ItemModelFactory factory) {
        Holder.Reference<ItemModelFactory> holder = ((WritableRegistry<ItemModelFactory>) BuiltInRegistries.ITEM_MODEL_FACTORY)
                .registerForHolder(new ResourceKey<>(Registries.ITEM_MODEL_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    public static ItemModel fromMap(Map<String, Object> map) {
        String type = (String) map.getOrDefault("type", "minecraft:model");
        Key key = Key.withDefaultNamespace(type, "minecraft");
        ItemModelFactory factory = BuiltInRegistries.ITEM_MODEL_FACTORY.getValue(key);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown model type: " + type);
        }
        return factory.create(map);
    }
}
