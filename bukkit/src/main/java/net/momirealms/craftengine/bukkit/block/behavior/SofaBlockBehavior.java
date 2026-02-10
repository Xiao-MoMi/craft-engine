package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.LevelUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.block.properties.type.SofaShape;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.HorizontalDirection;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class SofaBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<SofaBlockBehavior> FACTORY = new Factory();
    private final Property<HorizontalDirection> facingProperty;
    private final Property<SofaShape> shapeProperty;

    public SofaBlockBehavior(CustomBlock block, Property<HorizontalDirection> facing, Property<SofaShape> shape) {
        super(block);
        this.facingProperty = facing;
        this.shapeProperty = shape;
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        BlockPos clickedPos = context.getClickedPos();
        ImmutableBlockState blockState = state.owner().value().defaultState()
                .with(this.facingProperty, context.getHorizontalDirection().toHorizontalDirection());
        if (super.waterloggedProperty != null) {
            Object fluidState = BlockGetterProxy.INSTANCE.getFluidState(context.getLevel().serverWorld(), LocationUtils.toBlockPos(clickedPos));
            blockState = blockState.with(this.waterloggedProperty, FluidStateProxy.INSTANCE.getType(fluidState) == MFluids.WATER);
        }
        return blockState.with(this.shapeProperty, getSofaShape(blockState, context.getLevel().serverWorld(), clickedPos));
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
            LevelUtils.scheduleFluidTick(args[updateShape$level], args[updateShape$blockPos], MFluids.WATER, 5);
        }
        Direction direction = DirectionUtils.fromNMSDirection(VersionHelper.isOrAbove1_21_2() ? args[4] : args[1]);
        SofaShape sofaShape = getSofaShape(customState, level, LocationUtils.fromBlockPos(blockPos));
        return direction.axis().isHorizontal()
                ? customState.with(this.shapeProperty, sofaShape).customBlockState().literalObject()
                : superMethod.call();
    }

    private SofaShape getSofaShape(ImmutableBlockState state, Object level, BlockPos pos) {
        Direction direction = state.get(this.facingProperty).toDirection();
        Object relativeBlockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, LocationUtils.toBlockPos(pos.relative(direction.opposite())));
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(relativeBlockState);
        if (optionalCustomState.isPresent()) {
            ImmutableBlockState customState = optionalCustomState.get();
            Optional<SofaBlockBehavior> optionalStairsBlockBehavior = customState.behavior().getAs(SofaBlockBehavior.class);
            if (optionalStairsBlockBehavior.isPresent()) {
                SofaBlockBehavior stairsBlockBehavior = optionalStairsBlockBehavior.get();
                Direction direction1 = customState.get(stairsBlockBehavior.facingProperty).toDirection();
                if (direction1.axis() != state.get(this.facingProperty).toDirection().axis() && canTakeShape(state, level, pos, direction1)) {
                    if (direction1 == direction.counterClockWise()) {
                        return SofaShape.INNER_LEFT;
                    }
                    return SofaShape.INNER_RIGHT;
                }
            }
        }
        return SofaShape.STRAIGHT;
    }

    private boolean canTakeShape(ImmutableBlockState state, Object level, BlockPos pos, Direction face) {
        Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, LocationUtils.toBlockPos(pos.relative(face)));
        Optional<ImmutableBlockState> optionalAnotherState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalAnotherState.isEmpty()) {
            return true;
        }
        ImmutableBlockState anotherState = optionalAnotherState.get();
        Optional<SofaBlockBehavior> optionalBehavior = anotherState.behavior().getAs(SofaBlockBehavior.class);
        if (optionalBehavior.isEmpty()) {
            return true;
        }
        SofaBlockBehavior anotherBehavior = optionalBehavior.get();
        return anotherState.get(anotherBehavior.facingProperty) != state.get(this.facingProperty);
    }

    private static class Factory implements BlockBehaviorFactory<SofaBlockBehavior> {

        @Override
        @SuppressWarnings("unchecked")
        public SofaBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Property<HorizontalDirection> facing = (Property<HorizontalDirection>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("facing"), "warning.config.block.behavior.sofa.missing_facing");
            Property<SofaShape> shape = (Property<SofaShape>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("shape"), "warning.config.block.behavior.sofa.missing_shape");
            return new SofaBlockBehavior(block, facing, shape);
        }
    }
}
