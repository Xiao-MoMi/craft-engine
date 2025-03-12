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

package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.util.Key;

public class ItemKeys {
    public static final Key AIR = Key.of("minecraft:air");
    public static final Key STONE = Key.of("minecraft:stone");
    public static final Key TRIDENT = Key.of("minecraft:trident");
    public static final Key SHIELD = Key.of("minecraft:shield");
    public static final Key BOW = Key.of("minecraft:bow");
    public static final Key CROSSBOW = Key.of("minecraft:crossbow");
    public static final Key FISHING_ROD = Key.of("minecraft:fishing_rod");
    public static final Key ELYTRA = Key.of("minecraft:elytra");
    public static final Key GOAT_HORN = Key.of("minecraft:goat_horn");
    public static final Key WOODEN_AXE = Key.of("minecraft:wooden_axe");
    public static final Key STONE_AXE = Key.of("minecraft:stone_axe");
    public static final Key IRON_AXE = Key.of("minecraft:iron_axe");
    public static final Key GOLDEN_AXE = Key.of("minecraft:golden_axe");
    public static final Key DIAMOND_AXE = Key.of("minecraft:diamond_axe");
    public static final Key NETHERITE_AXE = Key.of("minecraft:netherite_axe");
    public static final Key WATER_BUCKET = Key.of("minecraft:water_bucket");
    public static final Key COD_BUCKET = Key.of("minecraft:cod_bucket");
    public static final Key SALMON_BUCKET = Key.of("minecraft:salmon_bucket");
    public static final Key TADPOLE_BUCKET = Key.of("minecraft:tadpole_bucket");
    public static final Key TROPICAL_FISH_BUCKET = Key.of("minecraft:tropical_fish_bucket");
    public static final Key PUFFERFISH_BUCKET = Key.of("minecraft:pufferfish_bucket");
    public static final Key AXOLOTL_BUCKET = Key.of("minecraft:axolotl_bucket");
    public static final Key BUCKET = Key.of("minecraft:bucket");
    public static final Key BONE_MEAL = Key.of("minecraft:bone_meal");
    public static final Key ENCHANTED_BOOK = Key.of("minecraft:enchanted_book");

    public static final Key[] AXES = new Key[] {
            WOODEN_AXE, STONE_AXE, IRON_AXE, GOLDEN_AXE, DIAMOND_AXE, NETHERITE_AXE
    };

    public static final Key[] WATER_BUCKETS = new Key[] {
            WATER_BUCKET, COD_BUCKET, SALMON_BUCKET, TROPICAL_FISH_BUCKET, TADPOLE_BUCKET, PUFFERFISH_BUCKET, AXOLOTL_BUCKET
    };
}
