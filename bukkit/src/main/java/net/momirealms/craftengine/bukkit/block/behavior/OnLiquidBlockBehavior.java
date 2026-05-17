package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.Vec3iProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidsProxy;

import java.util.List;
import java.util.Optional;

public final class OnLiquidBlockBehavior extends AbstractCanSurviveBlockBehavior {
    public static final BlockBehaviorFactory<OnLiquidBlockBehavior> FACTORY = new Factory();
    public final boolean onWater;
    public final boolean onLava;
    public final boolean stackable;

    private OnLiquidBlockBehavior(BlockDefinition block, int delay, boolean stackable, boolean onWater, boolean onLava) {
        super(block, delay);
        this.onWater = onWater;
        this.onLava = onLava;
        this.stackable = stackable;
    }

    private static class Factory implements BlockBehaviorFactory<OnLiquidBlockBehavior> {
        private static final String[] LIQUID_TYPE = new String[] {"liquid_type", "liquid-type"};

        @Override
        public OnLiquidBlockBehavior create(BlockDefinition block, ConfigSection section) {
            List<String> liquidTypes = section.getStringList(LIQUID_TYPE, List.of("water"));
            return new OnLiquidBlockBehavior(
                    block,
                    section.getInt("delay"),
                    section.getBoolean("stackable"),
                    liquidTypes.contains("water"),
                    liquidTypes.contains("lava")
            );
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    protected boolean canSurvive(Object thisBlock, Object state, Object level, Object blockPos) {
        int x = Vec3iProxy.INSTANCE.getX(blockPos);
        int y = Vec3iProxy.INSTANCE.getY(blockPos);
        int z = Vec3iProxy.INSTANCE.getZ(blockPos);
        Object belowPos = BlockPosProxy.INSTANCE.newInstance(x, y - 1, z);
        Object belowState = BlockGetterProxy.INSTANCE.getBlockState(level, belowPos);
        return mayPlaceOn(belowState, level, belowPos);
    }

    private boolean mayPlaceOn(Object belowState, Object world, Object belowPos) {
        if (this.stackable) {
            Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(belowState);
            if (optionalCustomState.isPresent() && optionalCustomState.get().owner().value() == super.blockDefinition) {
                return true;
            }
        }
        Object fluidState = BlockGetterProxy.INSTANCE.getFluidState(world, belowPos);
        Object fluidStateAbove = BlockGetterProxy.INSTANCE.getFluidState(world, LocationUtils.above(belowPos));
        if (FluidStateProxy.INSTANCE.getType(fluidStateAbove) != FluidsProxy.EMPTY) {
            return false;
        }
        if (this.onWater && (FluidStateProxy.INSTANCE.getType(fluidState) == FluidsProxy.WATER || BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getBlock(belowState) == BlocksProxy.ICE)) {
            return true;
        }
        if (this.onLava && FluidStateProxy.INSTANCE.getType(fluidState) == FluidsProxy.LAVA) {
            return true;
        }
        return false;
    }
}
