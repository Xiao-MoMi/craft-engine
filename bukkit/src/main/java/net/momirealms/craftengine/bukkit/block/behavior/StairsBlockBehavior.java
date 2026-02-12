package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.block.properties.type.SingleBlockHalf;
import net.momirealms.craftengine.core.block.properties.type.StairsShape;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.HorizontalDirection;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

@SuppressWarnings("DuplicatedCode")
public class StairsBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<StairsBlockBehavior> FACTORY = new Factory();
    private final Property<HorizontalDirection> facingProperty;
    private final Property<SingleBlockHalf> halfProperty;
    private final Property<StairsShape> shapeProperty;

    public StairsBlockBehavior(CustomBlock block, Property<HorizontalDirection> facing, Property<SingleBlockHalf> half, Property<StairsShape> shape) {
        super(block);
        this.facingProperty = facing;
        this.halfProperty = half;
        this.shapeProperty = shape;
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        Direction clickedFace = context.getClickedFace();
        BlockPos clickedPos = context.getClickedPos();
        ImmutableBlockState blockState = state.owner().value().defaultState()
                .with(this.facingProperty, context.getHorizontalDirection().toHorizontalDirection())
                .with(this.halfProperty, clickedFace != Direction.DOWN && (clickedFace == Direction.UP || !(context.getClickedLocation().y - clickedPos.y() > 0.5)) ? SingleBlockHalf.BOTTOM : SingleBlockHalf.TOP);
        if (super.waterloggedProperty != null) {
            Object fluidState = FastNMS.INSTANCE.method$BlockGetter$getFluidState(context.getLevel().serverWorld(), LocationUtils.toBlockPos(clickedPos));
            blockState = blockState.with(this.waterloggedProperty, FastNMS.INSTANCE.method$FluidState$getType(fluidState) == MFluids.WATER);
        }
        return blockState.with(this.shapeProperty, getStairsShape(blockState, context.getLevel().serverWorld(), clickedPos));
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object level = args[updateShape$level];
        Object blockPos = args[updateShape$blockPos];
        Object blockState = args[0];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return blockState;
        ImmutableBlockState customState = optionalCustomState.get();
        if (super.waterloggedProperty != null && customState.get(this.waterloggedProperty)) {
            FastNMS.INSTANCE.method$ScheduledTickAccess$scheduleFluidTick(args[updateShape$level], args[updateShape$blockPos], MFluids.WATER, 5);
        }
        Direction direction = DirectionUtils.fromNMSDirection(VersionHelper.isOrAbove1_21_2() ? args[4] : args[1]);
        StairsShape stairsShape = getStairsShape(customState, level, LocationUtils.fromBlockPos(blockPos));
        return direction.axis().isHorizontal()
                ? customState.with(this.shapeProperty, stairsShape).customBlockState().literalObject()
                : superMethod.call();
    }

    private StairsShape getStairsShape(ImmutableBlockState state, Object level, BlockPos pos) {
        Direction direction = state.get(this.facingProperty).toDirection();
        Object relativeBlockState1 = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, LocationUtils.toBlockPos(pos.relative(direction)));
        Optional<ImmutableBlockState> optionalCustomState1 = BlockStateUtils.getOptionalCustomBlockState(relativeBlockState1);
        if (optionalCustomState1.isPresent()) {
            ImmutableBlockState customState1 = optionalCustomState1.get();
            Optional<StairsBlockBehavior> optionalStairsBlockBehavior = customState1.behavior().getAs(StairsBlockBehavior.class);
            if (optionalStairsBlockBehavior.isPresent()) {
                StairsBlockBehavior stairsBlockBehavior = optionalStairsBlockBehavior.get();
                if (state.get(this.halfProperty) == customState1.get(stairsBlockBehavior.halfProperty)) {
                    Direction direction1 = customState1.get(stairsBlockBehavior.facingProperty).toDirection();
                    if (direction1.axis() != state.get(this.facingProperty).toDirection().axis() && canTakeShape(state, level, pos, direction1.opposite())) {
                        if (direction1 == direction.counterClockWise()) {
                            return StairsShape.OUTER_LEFT;
                        }
                        return StairsShape.OUTER_RIGHT;
                    }
                }
            }
        }
        Object relativeBlockState2 = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, LocationUtils.toBlockPos(pos.relative(direction.opposite())));
        Optional<ImmutableBlockState> optionalCustomState2 = BlockStateUtils.getOptionalCustomBlockState(relativeBlockState2);
        if (optionalCustomState2.isPresent()) {
            ImmutableBlockState customState2 = optionalCustomState2.get();
            Optional<StairsBlockBehavior> optionalStairsBlockBehavior = customState2.behavior().getAs(StairsBlockBehavior.class);
            if (optionalStairsBlockBehavior.isPresent()) {
                StairsBlockBehavior stairsBlockBehavior = optionalStairsBlockBehavior.get();
                if (state.get(this.halfProperty) == customState2.get(stairsBlockBehavior.halfProperty)) {
                    Direction direction2 = customState2.get(stairsBlockBehavior.facingProperty).toDirection();
                    if (direction2.axis() != state.get(this.facingProperty).toDirection().axis() && canTakeShape(state, level, pos, direction2)) {
                        if (direction2 == direction.counterClockWise()) {
                            return StairsShape.INNER_LEFT;
                        }
                        return StairsShape.INNER_RIGHT;
                    }
                }
            }
        }
        return StairsShape.STRAIGHT;
    }

    private boolean canTakeShape(ImmutableBlockState state, Object level, BlockPos pos, Direction face) {
        Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, LocationUtils.toBlockPos(pos.relative(face)));
        Optional<ImmutableBlockState> optionalAnotherState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalAnotherState.isEmpty()) {
            return true;
        }
        ImmutableBlockState anotherState = optionalAnotherState.get();
        Optional<StairsBlockBehavior> optionalBehavior = anotherState.behavior().getAs(StairsBlockBehavior.class);
        if (optionalBehavior.isEmpty()) {
            return true;
        }
        StairsBlockBehavior anotherBehavior = optionalBehavior.get();
        return anotherState.get(anotherBehavior.facingProperty) != state.get(this.facingProperty) || anotherState.get(anotherBehavior.halfProperty) != state.get(this.halfProperty);
    }

    private static class Factory implements BlockBehaviorFactory<StairsBlockBehavior> {

        @Override
        @SuppressWarnings("unchecked")
        public StairsBlockBehavior  create(CustomBlock block, Map<String, Object> arguments) {
            Property<HorizontalDirection> facing = (Property<HorizontalDirection>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("facing"), "warning.config.block.behavior.stairs.missing_facing");
            Property<SingleBlockHalf> half = (Property<SingleBlockHalf>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("half"), "warning.config.block.behavior.stairs.missing_half");
            Property<StairsShape> shape = (Property<StairsShape>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("shape"), "warning.config.block.behavior.stairs.missing_shape");
            return new StairsBlockBehavior(block, facing, half, shape);
        }
    }
}
