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

package net.momirealms.craftengine.bukkit.api;

import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.util.ItemUtils;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CraftEngineItems {

    /**
     * Gets a custom item by ID
     *
     * @param id id
     * @return the custom item
     */
    @Nullable
    public static CustomItem<ItemStack> byId(@NotNull Key id) {
        return BukkitItemManager.instance().getCustomItem(id).orElse(null);
    }

    /**
     * Gets a custom item by existing item stack
     *
     * @param itemStack item stack
     * @return the custom item
     */
    @Nullable
    public static CustomItem<ItemStack> byItemStack(@NotNull ItemStack itemStack) {
        if (ItemUtils.isEmpty(itemStack)) return null;
        return BukkitItemManager.instance().wrap(itemStack).getCustomItem().orElse(null);
    }

    /**
     * Checks if an item is a custom one
     *
     * @param itemStack item stack
     * @return true if it's a custom item
     */
    public static boolean isCustomItem(@NotNull ItemStack itemStack) {
        if (ItemUtils.isEmpty(itemStack)) return false;
        return BukkitItemManager.instance().wrap(itemStack).isCustomItem();
    }

    /**
     * Gets custom item id from item stack
     *
     * @param itemStack item stack
     * @return the custom id, null if it's not a custom one
     */
    @Nullable
    public static Key getCustomItemId(@NotNull ItemStack itemStack) {
        if (ItemUtils.isEmpty(itemStack)) return null;
        return BukkitItemManager.instance().wrap(itemStack).customId().orElse(null);
    }
}
