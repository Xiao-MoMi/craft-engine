package net.momirealms.craftengine.bukkit.world;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.api.CraftEngineBlocks;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.*;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.ExistingBlock;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SnowLayerBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.StateHolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class BukkitExistingBlock implements ExistingBlock {
    private final Block block;

    public BukkitExistingBlock(Block block) {
        this.block = block;
    }

    @Override
    public boolean canBeReplaced(BlockPlaceContext context) {
        Object state = BlockStateUtils.getBlockState(this.block);
        ImmutableBlockState customState = BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
        if (customState != null && !customState.isEmpty()) {
            return customState.behavior().canBeReplaced(context, customState);
        }
        if (BlockStateUtils.getBlockOwner(state) == MBlocks.SNOW) {
            return (Integer) StateHolderProxy.INSTANCE.getValue(state, SnowLayerBlockProxy.INSTANCE.getLayersProperty()) == 1;
        }
        return BlockStateUtils.isReplaceable(state);
    }

    @Override
    public boolean isWaterSource(BlockPlaceContext blockPlaceContext) {
        Location location = this.block.getLocation();
        Object serverLevel = FastNMS.INSTANCE.field$CraftWorld$ServerLevel(this.block.getWorld());
        Object fluidData = BlockGetterProxy.INSTANCE.getFluidState(serverLevel, LocationUtils.toBlockPos(location.getBlockX(), location.getBlockY(), location.getBlockZ()));
        if (fluidData == null) return false;
        return FluidStateProxy.INSTANCE.getType(fluidData) == MFluids.WATER;
    }

    @Override
    public @NotNull StatePropertyAccessor createStatePropertyAccessor() {
        return FastNMS.INSTANCE.createStatePropertyAccessor(BlockStateUtils.getBlockState(this.block));
    }

    @Override
    public boolean isCustom() {
        return CraftEngineBlocks.isCustomBlock(this.block);
    }

    @Override
    public @NotNull BlockStateWrapper blockState() {
        Object blockState = BlockStateUtils.getBlockState(this.block);
        return BlockRegistryMirror.byId(BlockStateUtils.blockStateToId(blockState));
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
    public Key id() {
        Object blockState = BlockStateUtils.getBlockState(this.block);
        Optional<ImmutableBlockState> optionalCustomBlockState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomBlockState.isPresent()) {
            return optionalCustomBlockState.get().owner().value().id();
        }
        return BlockStateUtils.getBlockOwnerIdFromState(blockState);
    }

    @Override
    public World world() {
        return BukkitAdaptors.adapt(this.block.getWorld());
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

    @Override
    public boolean is(Key tag) {
        Object state = FastNMS.INSTANCE.method$BlockGetter$getBlockState(FastNMS.INSTANCE.field$CraftWorld$ServerLevel(block.getWorld()), LocationUtils.toBlockPos(block.getX(), block.getY(), block.getZ()));
        return BlockStateUtils.isTag(state, tag);
    }

    public Block block() {
        return this.block;
    }
}
