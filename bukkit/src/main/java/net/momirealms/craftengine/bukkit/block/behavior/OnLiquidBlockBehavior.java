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

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OnLiquidBlockBehavior extends AbstractCanSurviveBlockBehavior {
    public static final Factory FACTORY = new Factory();
    private final boolean onWater;
    private final boolean onLava;
    private final boolean stackable;

    public OnLiquidBlockBehavior(CustomBlock block, int delay, boolean stackable, boolean onWater, boolean onLava) {
        super(block, delay);
        this.onWater = onWater;
        this.onLava = onLava;
        this.stackable = stackable;
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
            return new OnLiquidBlockBehavior(block, delay, stackable, liquidTypes.contains("water"), liquidTypes.contains("lava"));
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    protected boolean canSurvive(Object thisBlock, Object state, Object world, Object blockPos) {
        int y = FastNMS.INSTANCE.field$Vec3i$y(blockPos);
        int x = FastNMS.INSTANCE.field$Vec3i$x(blockPos);
        int z = FastNMS.INSTANCE.field$Vec3i$z(blockPos);
        Object belowPos = FastNMS.INSTANCE.constructor$BlockPos(x, y - 1, z);
        Object belowState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, belowPos);
        return mayPlaceOn(belowState, world, belowPos);
    }

    protected boolean mayPlaceOn(Object belowState, Object world, Object belowPos) {
        if (this.stackable) {
            Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(belowState);
            if (optionalCustomState.isPresent() && optionalCustomState.get().owner().value() == super.customBlock) {
                return true;
            }
        }
        Object fluidState = FastNMS.INSTANCE.method$BlockGetter$getFluidState(world, belowPos);
        Object fluidStateAbove = FastNMS.INSTANCE.method$BlockGetter$getFluidState(world, LocationUtils.above(belowPos));
        if (FastNMS.INSTANCE.method$FluidState$getType(fluidStateAbove) != MFluids.EMPTY) {
            return false;
        }
        if (this.onWater && (FastNMS.INSTANCE.method$FluidState$getType(fluidState) == MFluids.WATER || FastNMS.INSTANCE.method$BlockState$getBlock(belowState) == MBlocks.ICE)) {
            return true;
        }
        if (this.onLava && FastNMS.INSTANCE.method$FluidState$getType(fluidState) == MFluids.LAVA) {
            return true;
        }
        return false;
    }
}
