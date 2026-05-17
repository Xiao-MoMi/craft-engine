package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemsProxy;

public final class BukkitItemUtils {
    private BukkitItemUtils() {}

    public static boolean isDebugStick(Item item) {
        return ItemStackProxy.INSTANCE.getItem(item.minecraftItem()) == ItemsProxy.DEBUG_STICK;
    }
}
