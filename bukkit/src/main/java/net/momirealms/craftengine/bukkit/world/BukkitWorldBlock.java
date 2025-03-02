package net.momirealms.craftengine.bukkit.world;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.item.behavior.BlockItemBehavior;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.context.BlockPlaceContext;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.WorldBlock;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class BukkitWorldBlock implements WorldBlock {
    private final Block block;

    public BukkitWorldBlock(Block block) {
        this.block = block;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean canBeReplaced(BlockPlaceContext context) {
        ImmutableBlockState customState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockDataToId(block.getBlockData()));
        if (customState != null && !customState.isEmpty()) {
            Key clickedBlockId = customState.owner().value().id();
            Item<ItemStack> item = (Item<ItemStack>) context.getPlayer().getItemInHand(context.getHand());
            Optional<CustomItem<ItemStack>> customItem = BukkitItemManager.instance().getCustomItem(item.id());
            if (customItem.isPresent()) {
                CustomItem<ItemStack> custom = customItem.get();
                if (custom.behaviors() instanceof BlockItemBehavior blockItemBehavior) {
                    Key blockId = blockItemBehavior.blockId();
                    if (blockId.equals(clickedBlockId)) {
                        return false;
                    }
                }
            }
        }
        return block.isReplaceable();
    }

    @Override
    public boolean isWaterSource(BlockPlaceContext blockPlaceContext) {
        try {
            Location location = block.getLocation();
            Object serverLevel = Reflections.field$CraftWorld$ServerLevel.get(block.getWorld());
            Object fluidData = Reflections.method$Level$getFluidState.invoke(serverLevel, LocationUtils.toBlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
            if (fluidData == null) return false;
            return (boolean) Reflections.method$FluidState$isSource.invoke(fluidData);
        } catch (ReflectiveOperationException e) {
            CraftEngine.instance().logger().warn("Failed to check if water source is available", e);
            return false;
        }
    }

    public Block block() {
        return block;
    }
}
