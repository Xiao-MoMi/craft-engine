package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LevelUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.IsPathFindableBlockBehavior;
import net.momirealms.craftengine.core.block.properties.BooleanProperty;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;

import java.util.Map;
import java.util.concurrent.Callable;

public class HangableBlockBehavior extends BukkitBlockBehavior implements IsPathFindableBlockBehavior {
    public static final BlockBehaviorFactory<HangableBlockBehavior> FACTORY = new Factory();
    private final BooleanProperty hangingProperty;

    public HangableBlockBehavior(CustomBlock customBlock, BooleanProperty hangingProperty) {
        super(customBlock);
        this.hangingProperty = hangingProperty;
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        Object world = context.getLevel().serverWorld();
        Object blockPos = LocationUtils.toBlockPos(context.getClickedPos());
        Object fluidType = FluidStateProxy.INSTANCE.getType(BlockGetterProxy.INSTANCE.getFluidState(world, blockPos));
        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.axis() != Direction.Axis.Y) continue;
            ImmutableBlockState blockState = state.with(this.hangingProperty, direction == Direction.UP);
            if (!BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.canSurvive(blockState.customBlockState().literalObject(), world, blockPos)) continue;
            return super.waterloggedProperty != null ? blockState.with(super.waterloggedProperty, fluidType == MFluids.WATER) : blockState;
        }
        return state;
    }

    @Override
    public boolean canSurvive(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object state = args[0];
        Object world = args[1];
        Object blockPos = args[2];
        ImmutableBlockState blockState = BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
        if (blockState == null) return false;
        BooleanProperty hangingProperty = (BooleanProperty) blockState.owner().value().getProperty("hanging");
        if (hangingProperty == null) return false;
        Boolean hanging = blockState.get(hangingProperty);
        Object relativePos = FastNMS.INSTANCE.method$BlockPos$relative(blockPos, hanging ? DirectionProxy.UP : DirectionProxy.DOWN);
        return BlockProxy.INSTANCE.canSupportCenter(world, relativePos, hanging ? DirectionProxy.DOWN : DirectionProxy.UP);
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);
        if (state == null) return MBlocks.AIR$defaultState;
        if (super.waterloggedProperty != null && state.get(super.waterloggedProperty)) {
            LevelUtils.scheduleFluidTick(args[updateShape$level], args[updateShape$blockPos], MFluids.WATER, 5);
        }
        if ((state.get(this.hangingProperty) ? DirectionProxy.UP : DirectionProxy.DOWN) == args[updateShape$direction]
                && !BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.canSurvive(args[0], args[updateShape$level], args[updateShape$blockPos])) {
            return MBlocks.AIR$defaultState;
        }
        return superMethod.call();
    }

    @Override
    public boolean isPathFindable(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        return false;
    }

    private static class Factory implements BlockBehaviorFactory<HangableBlockBehavior> {

        @Override
        public HangableBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            BooleanProperty hanging = (BooleanProperty) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("hanging"), "warning.config.block.behavior.hangable.missing_hanging");
            return new HangableBlockBehavior(block, hanging);
        }
    }
}
