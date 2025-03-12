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

package net.momirealms.craftengine.bukkit.item.behavior;

import net.momirealms.craftengine.core.item.behavior.EmptyItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehaviors;
import net.momirealms.craftengine.core.util.Key;

public class BukkitItemBehaviors extends ItemBehaviors {
    public static final Key EMPTY = Key.from("craftengine:empty");
    public static final Key BLOCK_ITEM = Key.from("craftengine:block_item");
    public static final Key FURNITURE_ITEM = Key.from("craftengine:furniture_item");
    public static final Key AXE_ITEM = Key.from("craftengine:axe_item");
    public static final Key WATER_BUCKET_ITEM = Key.from("craftengine:water_bucket_item");
    public static final Key BUCKET_ITEM = Key.from("craftengine:bucket_item");

    public static void init() {
        register(EMPTY, (pack, path, args, id) -> EmptyItemBehavior.INSTANCE);
        register(BLOCK_ITEM, BlockItemBehavior.FACTORY);
        register(FURNITURE_ITEM, FurnitureItemBehavior.FACTORY);
        register(AXE_ITEM, AxeItemBehavior.FACTORY);
        register(WATER_BUCKET_ITEM, WaterBucketItemBehavior.FACTORY);
        register(BUCKET_ITEM, BucketItemBehavior.FACTORY);
    }
}
