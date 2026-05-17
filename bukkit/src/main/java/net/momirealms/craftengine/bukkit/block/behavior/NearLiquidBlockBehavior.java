package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.Vec3iProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidsProxy;

import java.util.List;
import java.util.Optional;

public final class NearLiquidBlockBehavior extends AbstractCanSurviveBlockBehavior {
    private static final List<Object> WATER = List.of(FluidsProxy.WATER, FluidsProxy.FLOWING_WATER);
    private static final List<Object> LAVA = List.of(FluidsProxy.LAVA, FluidsProxy.FLOWING_LAVA);
    public static final BlockBehaviorFactory<NearLiquidBlockBehavior> FACTORY = new Factory();
    public final boolean onWater;
    public final boolean onLava;
    public final boolean stackable;
    public final BlockPos[] positions;

    private NearLiquidBlockBehavior(BlockDefinition block,
                                    int delay,
                                    BlockPos[] positions,
                                    boolean stackable,
                                    boolean onWater,
                                    boolean onLava) {
        super(block, delay);
        this.onWater = onWater;
        this.onLava = onLava;
        this.stackable = stackable;
        this.positions = positions;
    }

    private static class Factory implements BlockBehaviorFactory<NearLiquidBlockBehavior> {
        private static final String[] LIQUID_TYPE = new String[] {"liquid_type", "liquid-type"};

        @Override
        public NearLiquidBlockBehavior create(BlockDefinition block, ConfigSection section) {
            List<String> liquidTypes = section.getStringList(LIQUID_TYPE, List.of("water"));
            BlockPos[] positions = section.getList("positions", v -> {
                ConfigValue[] configValues = v.splitValuesRestrict(",", 3);
                return new BlockPos(configValues[0].getAsInt(), configValues[1].getAsInt(), configValues[2].getAsInt());
            }).toArray(BlockPos[]::new);
            return new NearLiquidBlockBehavior(
                    block,
                    section.getInt("delay"),
                    positions,
                    section.getBoolean("stackable"),
                    liquidTypes.contains("water"),
                    liquidTypes.contains("lava")
            );
        }
    }

    @Override
    protected boolean canSurvive(Object thisBlock, Object state, Object level, Object blockPos) {
        int x = Vec3iProxy.INSTANCE.getX(blockPos);
        int y = Vec3iProxy.INSTANCE.getY(blockPos);
        int z = Vec3iProxy.INSTANCE.getZ(blockPos);
        if (this.stackable) {
            Object belowPos = BlockPosProxy.INSTANCE.newInstance(x, y - 1, z);
            Object belowState = BlockGetterProxy.INSTANCE.getBlockState(level, belowPos);
            Optional<ImmutableBlockState> optionalBelowCustomState = BlockStateUtils.getOptionalCustomBlockState(belowState);
            if (optionalBelowCustomState.isPresent() && optionalBelowCustomState.get().owner().value() == super.blockDefinition) {
                return true;
            }
        }
        for (BlockPos pos : positions) {
            Object belowPos = BlockPosProxy.INSTANCE.newInstance(x + pos.x(), y + pos.y(), z + pos.z());
            Object belowState = BlockGetterProxy.INSTANCE.getBlockState(level, belowPos);
            if (mayPlaceOn(belowState, level, belowPos)) {
                return true;
            }
        }
        return false;
    }

    private boolean mayPlaceOn(Object belowState, Object world, Object belowPos) {
        Object fluidState = BlockGetterProxy.INSTANCE.getFluidState(world, belowPos);
        Object fluidStateAbove = BlockGetterProxy.INSTANCE.getFluidState(world, LocationUtils.above(belowPos));
        if (FluidStateProxy.INSTANCE.getType(fluidStateAbove) != FluidsProxy.EMPTY) {
            return false;
        }
        if (this.onWater && (WATER.contains(FluidStateProxy.INSTANCE.getType(fluidState)) || BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getBlock(belowState) == BlocksProxy.ICE)) {
            return true;
        }
        if (this.onLava && LAVA.contains(FluidStateProxy.INSTANCE.getType(fluidState))) {
            return true;
        }
        return false;
    }
}
