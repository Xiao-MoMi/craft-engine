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

package net.momirealms.craftengine.bukkit.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class InventoryUtils {

    private InventoryUtils() {}

    public static int getSuitableHotBarSlot(PlayerInventory inventory) {
        int selectedSlot = inventory.getHeldItemSlot();
        int i;
        int j;
        for (j = 0; j < 9; ++j) {
            i = (selectedSlot + j) % 9;
            if (ItemUtils.isEmpty(inventory.getItem(i))) {
                return i;
            }
        }
        for (j = 0; j < 9; ++j) {
            i = (selectedSlot + j) % 9;
            ItemStack item = inventory.getItem(i);
            if (ItemUtils.isEmpty(item) || item.getEnchantments().isEmpty()) {
                return i;
            }
        }
        return selectedSlot;
    }

    public static int findMatchingItemSlot(PlayerInventory inventory, ItemStack itemStack) {
        ItemStack[] items = inventory.getStorageContents();
        for (int i = 0; i < items.length; ++i) {
            ItemStack stack = items[i];
            if (ItemUtils.isEmpty(stack)) continue;
            if (stack.isSimilar(itemStack)) {
                return i;
            }
        }
        return -1;
    }
}
