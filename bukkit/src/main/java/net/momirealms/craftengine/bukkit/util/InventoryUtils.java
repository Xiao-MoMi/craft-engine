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
