package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.BlockPos;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NearLiquidBlockBehavior extends AbstractCanSurviveBlockBehavior {
    private static final List<Object> WATER = List.of(MFluids.WATER, MFluids.FLOWING_WATER);
    private static final List<Object> LAVA = List.of(MFluids.LAVA, MFluids.FLOWING_LAVA);
    public static final Factory FACTORY = new Factory();
    private final boolean onWater;
    private final boolean onLava;
    private final boolean stackable;
    private final BlockPos[] positions;

    public NearLiquidBlockBehavior(CustomBlock block, int delay, BlockPos[] positions, boolean stackable, boolean onWater, boolean onLava) {
        super(block, delay);
        this.onWater = onWater;
        this.onLava = onLava;
        this.stackable = stackable;
        this.positions = positions;
    }

    public boolean onWater() {
        return this.onWater;
    }

    public boolean onLava() {
        return this.onLava;
    }

    public static class Factory implements BlockBehaviorFactory {
        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            List<String> liquidTypes = MiscUtils.getAsStringList(arguments.getOrDefault("liquid-type", List.of("water")));
            boolean stackable = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("stackable", false), "stackable");
            int delay = ResourceConfigUtils.getAsInt(arguments.getOrDefault("delay", 0), "delay");
            List<String> positionsToCheck = MiscUtils.getAsStringList(arguments.getOrDefault("positions", List.of()));
            if (positionsToCheck.isEmpty()) {
                return new NearLiquidBlockBehavior(block, delay, new BlockPos[]{new BlockPos(0,-1,0)}, stackable, liquidTypes.contains("water"), liquidTypes.contains("lava"));
            } else {
                BlockPos[] pos = new BlockPos[positionsToCheck.size()];
                for (int i = 0; i < pos.length; i++) {
                    String[] split = positionsToCheck.get(i).split(",");
                    pos[i] = new BlockPos(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2]));
                }
                return new NearLiquidBlockBehavior(block, delay, pos, stackable, liquidTypes.contains("water"), liquidTypes.contains("lava"));
            }
        }
    }

    @Override
    protected boolean canSurvive(Object thisBlock, Object state, Object world, Object blockPos) {
        int y = FastNMS.INSTANCE.field$Vec3i$y(blockPos);
        int x = FastNMS.INSTANCE.field$Vec3i$x(blockPos);
        int z = FastNMS.INSTANCE.field$Vec3i$z(blockPos);
        if (this.stackable) {
            Object belowPos = FastNMS.INSTANCE.constructor$BlockPos(x, y - 1, z);
            Object belowState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, belowPos);
            Optional<ImmutableBlockState> optionalBelowCustomState = BlockStateUtils.getOptionalCustomBlockState(belowState);
            if (optionalBelowCustomState.isPresent() && optionalBelowCustomState.get().owner().value() == super.customBlock) {
                return true;
            }
        }
        for (BlockPos pos : positions) {
            Object belowPos = FastNMS.INSTANCE.constructor$BlockPos(x + pos.x(), y + pos.y(), z + pos.z());
            Object belowState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, belowPos);
            if (mayPlaceOn(belowState, world, belowPos)) {
                return true;
            }
        }
        return false;
    }

    protected boolean mayPlaceOn(Object belowState, Object world, Object belowPos) {
        Object fluidState = FastNMS.INSTANCE.method$BlockGetter$getFluidState(world, belowPos);
        Object fluidStateAbove = FastNMS.INSTANCE.method$BlockGetter$getFluidState(world, LocationUtils.above(belowPos));
        if (FastNMS.INSTANCE.method$FluidState$getType(fluidStateAbove) != MFluids.EMPTY) {
            return false;
        }
        if (this.onWater && (WATER.contains(FastNMS.INSTANCE.method$FluidState$getType(fluidState)) || FastNMS.INSTANCE.method$BlockState$getBlock(belowState) == MBlocks.ICE)) {
            return true;
        }
        if (this.onLava && LAVA.contains(FastNMS.INSTANCE.method$FluidState$getType(fluidState))) {
            return true;
        }
        return false;
    }
}
