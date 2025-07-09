package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.bukkit.CraftBukkitReflections;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;

public class ItemUtils {

    private ItemUtils() {}

    @Contract("null -> true")
    public static boolean isEmpty(final ItemStack item) {
        if (item == null) return true;
        if (item.getType() == Material.AIR) return true;
        return item.getAmount() == 0;
    }

    public static boolean hasCustomItem(ItemStack[] stack) {
        for (ItemStack itemStack : stack) {
            if (!ItemUtils.isEmpty(itemStack)) {
                if (BukkitItemManager.instance().wrap(itemStack).customId().isPresent()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isCustomItem(ItemStack stack) {
        if (!ItemUtils.isEmpty(stack)) {
            return BukkitItemManager.instance().wrap(stack).customId().isPresent();
        }
        return false;
    }

    public static ItemStack ensureCraftItemStack(ItemStack itemStack) {
        if (CraftBukkitReflections.clazz$CraftItemStack.isInstance(itemStack)) {
            return itemStack;
        } else {
            return FastNMS.INSTANCE.method$CraftItemStack$asCraftCopy(itemStack);
        }
    }
}
