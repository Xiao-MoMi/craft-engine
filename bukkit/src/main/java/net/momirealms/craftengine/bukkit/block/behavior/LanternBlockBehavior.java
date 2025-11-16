package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.BooleanProperty;
import net.momirealms.craftengine.core.item.context.BlockPlaceContext;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class LanternBlockBehavior extends AbstractCanSurviveBlockBehavior {
    public static final Factory FACTORY = new Factory();
    private final BooleanProperty hangingProperty;

    public LanternBlockBehavior(CustomBlock block, BooleanProperty hangingProperty, int delay) {
        super(block, delay);
        this.hangingProperty = hangingProperty;
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        Object level = context.getLevel().serverWorld();
        Object blockPos = LocationUtils.toBlockPos(context.getClickedPos());

        boolean hanging = determineHangingState(context, level, blockPos);

        state = state.with(this.hangingProperty, hanging);

        if (super.waterloggedProperty != null) {
            Object fluidState = FastNMS.INSTANCE.method$BlockGetter$getFluidState(level, blockPos);
            state = state.with(super.waterloggedProperty, FastNMS.INSTANCE.method$FluidState$getType(fluidState) == MFluids.WATER);
        }

        return state;
    }

    private boolean determineHangingState(BlockPlaceContext context, Object level, Object blockPos) {
        boolean canHangAbove = canSupportCenter(level, LocationUtils.above(blockPos), Direction.DOWN);
        boolean canStandBelow = canSupportCenter(level, LocationUtils.below(blockPos), Direction.UP);

        Direction clickedFace = context.getClickedFace();

        if (clickedFace == Direction.DOWN) {
            return true;
        } else if (clickedFace == Direction.UP) {
            return false;
        } else {
            return !canStandBelow && canHangAbove;
        }
    }

    @Override
    protected boolean canSurvive(Object thisBlock, Object state, Object world, Object blockPos) {
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(state);
        if (optionalCustomState.isEmpty()) return false;

        ImmutableBlockState customState = optionalCustomState.get();
        boolean hanging = customState.get(this.hangingProperty);

        if (hanging) {
            Object abovePos = LocationUtils.above(blockPos);
            return canSupportCenter(world, abovePos, Direction.DOWN);
        } else {
            Object belowPos = LocationUtils.below(blockPos);
            return canSupportCenter(world, belowPos, Direction.UP);
        }
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object blockState = args[0];
        Object level = args[updateShape$level];
        Object blockPos = args[updateShape$blockPos];

        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) {
            return superMethod.call();
        }

        ImmutableBlockState customState = optionalCustomState.get();

        if (!canSurvive(thisBlock, blockState, level, blockPos)) {
            return MBlocks.AIR$defaultState;
        }

        if (super.waterloggedProperty != null && customState.get(super.waterloggedProperty)) {
            FastNMS.INSTANCE.method$ScheduledTickAccess$scheduleFluidTick(level, blockPos, MFluids.WATER, 5);
        }

        return blockState;
    }

    @Override
    public void neighborChanged(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object blockState = args[0];
        Object level = args[1];
        Object blockPos = args[2];

        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return;

        if (!canSurvive(thisBlock, blockState, level, blockPos)) {
            FastNMS.INSTANCE.method$Level$destroyBlock(level, blockPos, true);
        }
    }

    private boolean canSupportCenter(Object world, Object pos, Direction direction) {
        return FastNMS.INSTANCE.method$Block$canSupportCenter(world, pos, DirectionUtils.toNMSDirection(direction));
    }

    public static class Factory implements BlockBehaviorFactory {
        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            BooleanProperty hangingProperty = (BooleanProperty) ResourceConfigUtils.requireNonNullOrThrow(
                    block.getProperty("hanging"),
                    "warning.config.block.behavior.lantern.missing_hanging"
            );
            int delay = ResourceConfigUtils.getAsInt(arguments.getOrDefault("delay", 0), "delay");
            return new LanternBlockBehavior(block, hangingProperty, delay);
        }
    }
}
