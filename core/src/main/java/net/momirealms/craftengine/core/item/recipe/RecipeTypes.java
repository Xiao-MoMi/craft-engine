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

package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.registry.BuiltInRegistries;
import net.momirealms.craftengine.core.registry.Holder;
import net.momirealms.craftengine.core.registry.Registries;
import net.momirealms.craftengine.core.registry.WritableRegistry;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceKey;

import java.util.Map;

public class RecipeTypes {
    public static final Key SHAPED = Key.of("minecraft:shaped");
    public static final Key SHAPELESS = Key.of("minecraft:shapeless");
    public static final Key SMELTING = Key.of("minecraft:smelting");
    public static final Key BLASTING = Key.of("minecraft:blasting");
    public static final Key SMOKING = Key.of("minecraft:smoking");
    public static final Key CAMPFIRE_COOKING = Key.of("minecraft:campfire_cooking");
    public static final Key STONE_CUTTING = Key.of("minecraft:stone_cutting");

    static {
        register(SHAPED, CustomShapedRecipe.FACTORY);
        register(SHAPELESS, CustomShapelessRecipe.FACTORY);
        register(SMELTING, CustomSmeltingRecipe.FACTORY);
        register(SMOKING, CustomSmokingRecipe.FACTORY);
        register(BLASTING, CustomBlastingRecipe.FACTORY);
        register(CAMPFIRE_COOKING, CustomCampfireRecipe.FACTORY);
        register(STONE_CUTTING, CustomStoneCuttingRecipe.FACTORY);
    }

    public static <T> void register(Key key, RecipeFactory<T> factory) {
        Holder.Reference<RecipeFactory<?>> holder = ((WritableRegistry<RecipeFactory<?>>) BuiltInRegistries.RECIPE_FACTORY)
                .registerForHolder(new ResourceKey<>(Registries.RECIPE_FACTORY.location(), key));
        holder.bindValue(factory);
    }

    @SuppressWarnings("unchecked")
    public static <T> Recipe<T> fromMap(Key id, Map<String, Object> map) {
        String type = (String) map.get("type");
        if (type == null) {
            throw new NullPointerException("recipe type cannot be null");
        }
        Key key = Key.withDefaultNamespace(type, "minecraft");
        RecipeFactory<T> factory = (RecipeFactory<T>) BuiltInRegistries.RECIPE_FACTORY.getValue(key);
        if (factory == null) {
            throw new IllegalArgumentException("Unknown recipe type: " + type);
        }
        return factory.create(id, map);
    }
}
