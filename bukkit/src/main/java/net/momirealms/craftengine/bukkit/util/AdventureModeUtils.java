package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.CraftWorldProxy;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.inventory.CraftItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.pattern.BlockInWorldProxy;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("DuplicatedCode")
public final class AdventureModeUtils {

    private AdventureModeUtils() {}

    public static boolean canBreak(ItemStack itemStack, Location pos) {
        return canPlace(itemStack, pos, null);
    }

    public static boolean canBreak(ItemStack itemStack, Location pos, Object state) {
        Object blockPos = LocationUtils.toBlockPos(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
        Object blockInWorld = BlockInWorldProxy.INSTANCE.newInstance(CraftWorldProxy.INSTANCE.getWorld(pos.getWorld()), blockPos, false);
        if (state != null) {
            BlockInWorldProxy.INSTANCE.setState(blockInWorld, state);
        }
        return FastNMS.INSTANCE.method$ItemStack$canBreakInAdventureMode(CraftItemStackProxy.INSTANCE.unwrap(itemStack), blockInWorld);
    }

    public static boolean canPlace(Item<?> itemStack, World world, BlockPos pos, Object state) {
        Object blockPos = LocationUtils.toBlockPos(pos);
        Object item = itemStack == null ? ItemStackProxy.EMPTY : itemStack.getLiteralObject();
        Object blockInWorld = BlockInWorldProxy.INSTANCE.newInstance(CraftWorldProxy.INSTANCE.getWorld((org.bukkit.World) world.platformWorld()), blockPos, false);
        if (state != null) {
            BlockInWorldProxy.INSTANCE.setState(blockInWorld, state);
        }
        return FastNMS.INSTANCE.method$ItemStack$canPlaceInAdventureMode(item, blockInWorld);
    }

    public static boolean canPlace(ItemStack itemStack, Location pos, Object state) {
        Object blockPos = LocationUtils.toBlockPos(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
        Object blockInWorld = BlockInWorldProxy.INSTANCE.newInstance(CraftWorldProxy.INSTANCE.getWorld(pos.getWorld()), blockPos, false);
        if (state != null) {
            BlockInWorldProxy.INSTANCE.setState(blockInWorld, state);
        }
        return FastNMS.INSTANCE.method$ItemStack$canPlaceInAdventureMode(CraftItemStackProxy.INSTANCE.unwrap(itemStack), blockInWorld);
    }
}
