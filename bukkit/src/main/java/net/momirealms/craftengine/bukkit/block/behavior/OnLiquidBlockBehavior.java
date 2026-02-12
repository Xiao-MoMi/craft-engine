package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.Vec3iProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class OnLiquidBlockBehavior extends AbstractCanSurviveBlockBehavior {
    public static final BlockBehaviorFactory<OnLiquidBlockBehavior> FACTORY = new Factory();
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

    private static class Factory implements BlockBehaviorFactory<OnLiquidBlockBehavior> {
        @Override
        public OnLiquidBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            List<String> liquidTypes = MiscUtils.getAsStringList(arguments.getOrDefault("liquid-type", List.of("water")));
            boolean stackable = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("stackable", false), "stackable");
            int delay = ResourceConfigUtils.getAsInt(arguments.getOrDefault("delay", 0), "delay");
            return new OnLiquidBlockBehavior(block, delay, stackable, liquidTypes.contains("water"), liquidTypes.contains("lava"));
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    protected boolean canSurvive(Object thisBlock, Object state, Object world, Object blockPos) {
        int x = Vec3iProxy.INSTANCE.getX(blockPos);
        int y = Vec3iProxy.INSTANCE.getY(blockPos);
        int z = Vec3iProxy.INSTANCE.getZ(blockPos);
        Object belowPos = BlockPosProxy.INSTANCE.newInstance(x, y - 1, z);
        Object belowState = BlockGetterProxy.INSTANCE.getBlockState(world, belowPos);
        return mayPlaceOn(belowState, world, belowPos);
    }

    protected boolean mayPlaceOn(Object belowState, Object world, Object belowPos) {
        if (this.stackable) {
            Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(belowState);
            if (optionalCustomState.isPresent() && optionalCustomState.get().owner().value() == super.customBlock) {
                return true;
            }
        }
        Object fluidState = BlockGetterProxy.INSTANCE.getFluidState(world, belowPos);
        Object fluidStateAbove = BlockGetterProxy.INSTANCE.getFluidState(world, LocationUtils.above(belowPos));
        if (FluidStateProxy.INSTANCE.getType(fluidStateAbove) != MFluids.EMPTY) {
            return false;
        }
        if (this.onWater && (FluidStateProxy.INSTANCE.getType(fluidState) == MFluids.WATER || BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getBlock(belowState) == MBlocks.ICE)) {
            return true;
        }
        if (this.onLava && FluidStateProxy.INSTANCE.getType(fluidState) == MFluids.LAVA) {
            return true;
        }
        return false;
    }
}
