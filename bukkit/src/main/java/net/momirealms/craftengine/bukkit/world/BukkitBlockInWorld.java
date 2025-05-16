package net.momirealms.craftengine.bukkit.world;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.item.behavior.BlockItemBehavior;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.context.BlockPlaceContext;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockInWorld;
import net.momirealms.craftengine.core.world.World;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class BukkitBlockInWorld implements BlockInWorld {
    private final Block block;

    public BukkitBlockInWorld(Block block) {
        this.block = block;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean canBeReplaced(BlockPlaceContext context) {
        ImmutableBlockState customState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockDataToId(this.block.getBlockData()));
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
        return this.block.isReplaceable();
    }

    @Override
    public boolean isWaterSource(BlockPlaceContext blockPlaceContext) {
        try {
            Location location = this.block.getLocation();
            Object serverLevel = FastNMS.INSTANCE.field$CraftWorld$ServerLevel(this.block.getWorld());
            Object fluidData = Reflections.method$Level$getFluidState.invoke(serverLevel, LocationUtils.toBlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
            if (fluidData == null) return false;
            return Reflections.method$FluidState$getType.invoke(fluidData) == Reflections.instance$Fluids$WATER;
        } catch (ReflectiveOperationException e) {
            CraftEngine.instance().logger().warn("Failed to check if water source is available", e);
            return false;
        }
    }

    @Override
    public int x() {
        return this.block.getX();
    }

    @Override
    public int y() {
        return this.block.getY();
    }

    @Override
    public int z() {
        return this.block.getZ();
    }

    @Override
    public World world() {
        return new BukkitWorld(this.block.getWorld());
    }

    @Override
    public ImmutableBlockState customBlockState() {
        return CraftEngineBlocks.getCustomBlockState(this.block);
    }

    @Override
    public CustomBlock customBlock() {
        ImmutableBlockState state = CraftEngineBlocks.getCustomBlockState(this.block);
        if (state != null) {
            return state.owner().value();
        }
        return null;
    }

    public Block block() {
        return this.block;
    }
}
