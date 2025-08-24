package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public final class InventoryUtils {

    private InventoryUtils() {}

    public static Player getPlayerFromInventoryEvent(InventoryEvent event) {
        if (VersionHelper.isOrAbove1_21()) {
            return (Player) event.getView().getPlayer();
        } else {
            return LegacyInventoryUtils.getPlayerFromInventoryEvent(event);
        }
    }

    public static int getSuitableHotBarSlot(PlayerInventory inventory) {
        int selectedSlot = inventory.getHeldItemSlot();
        int i;
        int j;
        for (j = 0; j < 9; ++j) {
            i = (selectedSlot + j) % 9;
            if (ItemStackUtils.isEmpty(inventory.getItem(i))) {
                return i;
            }
        }
        for (j = 0; j < 9; ++j) {
            i = (selectedSlot + j) % 9;
            ItemStack item = inventory.getItem(i);
            if (ItemStackUtils.isEmpty(item) || item.getEnchantments().isEmpty()) {
                return i;
            }
        }
        return selectedSlot;
    }

    public static int findMatchingItemSlot(PlayerInventory inventory, ItemStack itemStack) {
        ItemStack[] items = inventory.getStorageContents();
        for (int i = 0; i < items.length; ++i) {
            ItemStack stack = items[i];
            if (ItemStackUtils.isEmpty(stack)) continue;
            if (stack.isSimilar(itemStack)) {
                return i;
            }
        }
        return -1;
    }
}
