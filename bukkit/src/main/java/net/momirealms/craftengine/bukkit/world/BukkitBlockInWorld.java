package net.momirealms.craftengine.bukkit.world;

import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.item.context.BlockPlaceContext;
import net.momirealms.craftengine.core.world.BlockInWorld;
import net.momirealms.craftengine.core.world.World;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class BukkitBlockInWorld implements BlockInWorld {
    private final Block block;

    public BukkitBlockInWorld(Block block) {
        this.block = block;
    }

    @Override
    public boolean canBeReplaced(BlockPlaceContext context) {
        ImmutableBlockState customState = CraftEngineBlocks.getCustomBlockState(this.block);
        if (customState != null && !customState.isEmpty()) {
            return customState.behavior().canBeReplaced(context, customState);
        }
        return this.block.isReplaceable();
    }

    @Override
    public boolean isWaterSource(BlockPlaceContext blockPlaceContext) {
        Location location = this.block.getLocation();
        Object serverLevel = FastNMS.INSTANCE.field$CraftWorld$ServerLevel(this.block.getWorld());
        Object fluidData = FastNMS.INSTANCE.method$Level$getFluidState(serverLevel, LocationUtils.toBlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
        if (fluidData == null) return false;
        return FastNMS.INSTANCE.method$FluidState$getType(fluidData) == MFluids.WATER;
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
