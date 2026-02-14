package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemsProxy;
import org.bukkit.inventory.ItemStack;

public final class BukkitItemUtils {

    private BukkitItemUtils() {}

    public static boolean isDebugStick(Item<ItemStack> item) {
        return ItemStackProxy.INSTANCE.getItem(item.getLiteralObject()) == ItemsProxy.DEBUG_STICK;
    }
}
