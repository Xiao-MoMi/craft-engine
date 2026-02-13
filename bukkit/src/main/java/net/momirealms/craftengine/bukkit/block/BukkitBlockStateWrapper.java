package net.momirealms.craftengine.bukkit.block;


import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.BlockTags;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.AbstractBlockStateWrapper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.WorldAccessor;
import net.momirealms.craftengine.proxy.minecraft.core.RegistryProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.RegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;

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
        return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$1(super.blockState, BlockTags.getOrCreate(tag));
    }

    @Override
    public boolean isAir() {
        return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isAir(super.blockState);
    }

    @Override
    public String getAsString() {
        return BlockStateUtils.fromBlockData(super.blockState).getAsString();
    }

    @Override
    public Key fluidState() {
        Object fluid = FluidStateProxy.INSTANCE.getType(BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getFluidState(super.blockState));
        return KeyUtils.identifierToKey(RegistryProxy.INSTANCE.getKey(RegistriesProxy.FLUID, fluid));
    }

    @Override
    public boolean replaceable() {
        return BlockStateUtils.isReplaceable(super.blockState);
    }

    @Override
    public boolean canSurvive(WorldAccessor world, BlockPos pos) {
        return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.canSurvive(super.blockState, world.literalObject(), LocationUtils.toBlockPos(pos));
    }
}
