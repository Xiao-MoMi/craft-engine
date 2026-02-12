package net.momirealms.craftengine.bukkit.block;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MRegistries;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.BlockTags;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.AbstractBlockStateWrapper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.WorldAccessor;

public abstract class BukkitBlockStateWrapper extends AbstractBlockStateWrapper {

    protected BukkitBlockStateWrapper(Object blockState, int registryId) {
        super(blockState, registryId);
    }

    @Override
    public Key ownerId() {
        return BlockStateUtils.getBlockOwnerIdFromState(super.blockState);
    }

    @Override
    public boolean hasTag(Key tag) {
        return FastNMS.INSTANCE.method$BlockStateBase$is(super.blockState, BlockTags.getOrCreate(tag));
    }

    @Override
    public boolean isAir() {
        return FastNMS.INSTANCE.method$BlockStateBase$isAir(super.blockState);
    }

    @Override
    public String getAsString() {
        return BlockStateUtils.fromBlockData(super.blockState).getAsString();
    }

    @Override
    public Key fluidState() {
        Object fluid = FastNMS.INSTANCE.method$FluidState$getType(FastNMS.INSTANCE.field$BlockBehaviour$BlockStateBase$fluidState(super.blockState));
        return KeyUtils.resourceLocationToKey(FastNMS.INSTANCE.method$Registry$getKey(MRegistries.FLUID, fluid));
    }

    @Override
    public boolean replaceable() {
        return BlockStateUtils.isReplaceable(super.blockState);
    }

    @Override
    public boolean canSurvive(WorldAccessor world, BlockPos pos) {
        return FastNMS.INSTANCE.method$BlockStateBase$canSurvive(super.blockState, world.literalObject(), LocationUtils.toBlockPos(pos));
    }
}
