package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.block.property.type.SingleBlockHalf;
import net.momirealms.craftengine.core.block.property.type.StairsShape;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidsProxy;

import java.util.Optional;

@SuppressWarnings("DuplicatedCode")
public final class StairsBlockBehavior extends WaterloggedBlockBehavior {
    public static final BlockBehaviorFactory<StairsBlockBehavior> FACTORY = new Factory();
    public final Property<Direction> facingProperty;
    public final Property<SingleBlockHalf> halfProperty;
    public final Property<StairsShape> shapeProperty;

    private StairsBlockBehavior(BlockDefinition block,
                                Property<Direction> facing,
                                Property<SingleBlockHalf> half,
                                Property<StairsShape> shape,
                                Property<Boolean> waterloggedProperty) {
        super(block, waterloggedProperty);
        this.facingProperty = facing;
        this.halfProperty = half;
        this.shapeProperty = shape;
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        Direction clickedFace = context.getClickedFace();
        BlockPos clickedPos = context.getClickedPos();
        ImmutableBlockState blockState = state.owner().value().defaultState()
                .with(this.facingProperty, context.getHorizontalDirection())
                .with(this.halfProperty, clickedFace != Direction.DOWN && (clickedFace == Direction.UP || !(context.getClickedLocation().y - clickedPos.y() > 0.5)) ? SingleBlockHalf.BOTTOM : SingleBlockHalf.TOP);
        if (super.waterloggedProperty != null) {
            Object fluidState = BlockGetterProxy.INSTANCE.getFluidState(context.getLevel().minecraftWorld(), LocationUtils.toBlockPos(clickedPos));
            blockState = blockState.with(this.waterloggedProperty, FluidStateProxy.INSTANCE.getType(fluidState) == FluidsProxy.WATER);
        }
        return blockState.with(this.shapeProperty, getStairsShape(blockState, context.getLevel().minecraftWorld(), clickedPos));
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args) {
        Object level = args[updateShape$level];
        Object blockPos = args[updateShape$blockPos];
        Object blockState = args[0];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return blockState;
        ImmutableBlockState customState = optionalCustomState.get();
        if (super.waterloggedProperty != null && customState.get(this.waterloggedProperty)) {
            LevelAccessorProxy.INSTANCE.scheduleTick$1(args[updateShape$level], args[updateShape$blockPos], FluidsProxy.WATER, 5);
        }
        Direction direction = DirectionUtils.fromNMSDirection(VersionHelper.isOrAbove1_21_2 ? args[4] : args[1]);
        StairsShape stairsShape = getStairsShape(customState, level, LocationUtils.fromBlockPos(blockPos));
        return direction.axis().isHorizontal()
                ? customState.with(this.shapeProperty, stairsShape).customBlockState().minecraftState()
                : super.updateShape(thisBlock, args);
    }

    private StairsShape getStairsShape(ImmutableBlockState state, Object level, BlockPos pos) {
        Direction direction = state.get(this.facingProperty);
        Object relativeBlockState1 = BlockGetterProxy.INSTANCE.getBlockState(level, LocationUtils.toBlockPos(pos.relative(direction)));
        Optional<ImmutableBlockState> optionalCustomState1 = BlockStateUtils.getOptionalCustomBlockState(relativeBlockState1);
        if (optionalCustomState1.isPresent()) {
            ImmutableBlockState anotherCustomState = optionalCustomState1.get();
            StairsBlockBehavior stairsBlockBehavior = anotherCustomState.behavior().getFirst(StairsBlockBehavior.class);
            if (stairsBlockBehavior != null && state.get(this.halfProperty) == anotherCustomState.get(stairsBlockBehavior.halfProperty)) {
                Direction direction1 = anotherCustomState.get(stairsBlockBehavior.facingProperty);
                if (direction1.axis() != state.get(this.facingProperty).axis() && canTakeShape(state, level, pos, direction1.opposite())) {
                    if (direction1 == direction.counterClockWise()) {
                        return StairsShape.OUTER_LEFT;
                    }
                    return StairsShape.OUTER_RIGHT;
                }
            }
        }
        Object relativeBlockState2 = BlockGetterProxy.INSTANCE.getBlockState(level, LocationUtils.toBlockPos(pos.relative(direction.opposite())));
        Optional<ImmutableBlockState> optionalCustomState2 = BlockStateUtils.getOptionalCustomBlockState(relativeBlockState2);
        if (optionalCustomState2.isPresent()) {
            ImmutableBlockState anotherCustomState = optionalCustomState2.get();
            StairsBlockBehavior stairsBlockBehavior = anotherCustomState.behavior().getFirst(StairsBlockBehavior.class);
            if (stairsBlockBehavior != null && state.get(this.halfProperty) == anotherCustomState.get(stairsBlockBehavior.halfProperty)) {
                Direction direction2 = anotherCustomState.get(stairsBlockBehavior.facingProperty);
                if (direction2.axis() != state.get(this.facingProperty).axis() && canTakeShape(state, level, pos, direction2)) {
                    if (direction2 == direction.counterClockWise()) {
                        return StairsShape.INNER_LEFT;
                    }
                    return StairsShape.INNER_RIGHT;
                }
            }
        }
        return StairsShape.STRAIGHT;
    }

    private boolean canTakeShape(ImmutableBlockState state, Object level, BlockPos pos, Direction face) {
        Object blockState = BlockGetterProxy.INSTANCE.getBlockState(level, LocationUtils.toBlockPos(pos.relative(face)));
        Optional<ImmutableBlockState> optionalAnotherState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalAnotherState.isEmpty()) {
            return true;
        }
        ImmutableBlockState anotherState = optionalAnotherState.get();
        StairsBlockBehavior anotherBehavior = anotherState.behavior().getFirst(StairsBlockBehavior.class);
        if (anotherBehavior == null) {
            return true;
        }
        return anotherState.get(anotherBehavior.facingProperty) != state.get(this.facingProperty) || anotherState.get(anotherBehavior.halfProperty) != state.get(this.halfProperty);
    }

    private static class Factory implements BlockBehaviorFactory<StairsBlockBehavior> {

        @Override
        public StairsBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new StairsBlockBehavior(
                    block,
                    BlockBehaviorFactory.getProperty(section.path(), block, "facing", Direction.class),
                    BlockBehaviorFactory.getProperty(section.path(), block, "half", SingleBlockHalf.class),
                    BlockBehaviorFactory.getProperty(section.path(), block, "shape", StairsShape.class),
                    BlockBehaviorFactory.getOptionalProperty(block, "waterlogged", Boolean.class)
            );
        }
    }
}
