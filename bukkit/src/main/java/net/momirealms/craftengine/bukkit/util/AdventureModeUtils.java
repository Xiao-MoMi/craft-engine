package net.momirealms.craftengine.bukkit.util;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;
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
        Object blockInWorld = FastNMS.INSTANCE.constructor$BlockInWorld(FastNMS.INSTANCE.field$CraftWorld$ServerLevel(pos.getWorld()), blockPos, false);
        if (state != null) {
            BlockInWorldProxy.INSTANCE.setState(blockInWorld, state);
        }
        return FastNMS.INSTANCE.method$ItemStack$canBreakInAdventureMode(FastNMS.INSTANCE.field$CraftItemStack$handle(itemStack), blockInWorld);
    }

    public static boolean canPlace(Item<?> itemStack, World world, BlockPos pos, Object state) {
        Object blockPos = LocationUtils.toBlockPos(pos);
        Object item = itemStack == null ? ItemStackProxy.EMPTY : itemStack.getLiteralObject();
        Object blockInWorld = FastNMS.INSTANCE.constructor$BlockInWorld(FastNMS.INSTANCE.field$CraftWorld$ServerLevel((org.bukkit.World) world.platformWorld()), blockPos, false);
        if (state != null) {
            BlockInWorldProxy.INSTANCE.setState(blockInWorld, state);
        }
        return FastNMS.INSTANCE.method$ItemStack$canPlaceInAdventureMode(item, blockInWorld);
    }

    public static boolean canPlace(ItemStack itemStack, Location pos, Object state) {
        Object blockPos = LocationUtils.toBlockPos(pos.getBlockX(), pos.getBlockY(), pos.getBlockZ());
        Object blockInWorld = FastNMS.INSTANCE.constructor$BlockInWorld(FastNMS.INSTANCE.field$CraftWorld$ServerLevel(pos.getWorld()), blockPos, false);
        if (state != null) {
            BlockInWorldProxy.INSTANCE.setState(blockInWorld, state);
        }
        return FastNMS.INSTANCE.method$ItemStack$canPlaceInAdventureMode(FastNMS.INSTANCE.field$CraftItemStack$handle(itemStack), blockInWorld);
    }
}
