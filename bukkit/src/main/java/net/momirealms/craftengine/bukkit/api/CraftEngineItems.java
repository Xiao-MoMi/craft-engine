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
