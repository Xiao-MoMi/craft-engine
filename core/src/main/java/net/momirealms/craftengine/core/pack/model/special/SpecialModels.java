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

package net.momirealms.craftengine.core.pack.model.special;

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public class SpecialModels {
    public static final Key BANNER = Key.of("minecraft:banner");
    public static final Key BED = Key.of("minecraft:bed");
    public static final Key CONDUIT = Key.of("minecraft:conduit");
    public static final Key CHEST = Key.of("minecraft:chest");
    public static final Key DECORATED_POT = Key.of("minecraft:decorated_pot");
    public static final Key HANGING_SIGN = Key.of("minecraft:hanging_sign");
    public static final Key HEAD = Key.of("minecraft:head");
    public static final Key SHIELD = Key.of("minecraft:shield");
    public static final Key SHULKER_BOX = Key.of("minecraft:shulker_box");
    public static final Key STANDING_SIGN = Key.of("minecraft:standing_sign");
    public static final Key TRIDENT = Key.of("minecraft:trident");

    static {
        register(TRIDENT, SimpleSpecialModel.FACTORY);
        register(DECORATED_POT, SimpleSpecialModel.FACTORY);
        register(CONDUIT, SimpleSpecialModel.FACTORY);
        register(SHIELD, SimpleSpecialModel.FACTORY);
        register(HANGING_SIGN, SignSpecialModel.FACTORY);
        register(STANDING_SIGN, SignSpecialModel.FACTORY);
        register(CHEST, ChestSpecialModel.FACTORY);
        register(BANNER, BannerSpecialModel.FACTORY);
        register(BED, BedSpecialModel.FACTORY);
        register(HEAD, HeadSpecialModel.FACTORY);
    }

    public static void register(Key key, SpecialModelFactory factory) {
        Holder.Reference<SpecialModelFactory> holder = ((WritableRegistry<SpecialModelFactory>) BuiltInRegistries.SPECIAL_MODEL_FACTORY)
                .registerForHolder(new ResourceKey<>(Registries.SPECIAL_MODEL_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    public static SpecialModel fromMap(Map<String, Object> map) {
        String type = (String) map.get("type");
        if (type == null) {
            throw new NullPointerException("special model type cannot be null");
        }
        Key key = Key.withDefaultNamespace(type, "minecraft");
        SpecialModelFactory factory = BuiltInRegistries.SPECIAL_MODEL_FACTORY.getValue(key);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown special model type: " + type);
        }
        return factory.create(map);
    }
}
