package net.momirealms.craftengine.bukkit.util;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class LegacyInventoryUtils {

    public static Inventory getTopInventory(Player player) {
        return player.getOpenInventory().getTopInventory();
    }
}
