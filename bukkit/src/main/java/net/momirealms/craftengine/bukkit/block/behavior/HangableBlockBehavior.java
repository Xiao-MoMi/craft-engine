package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.PathFindingBlock;
import net.momirealms.craftengine.core.block.property.BooleanProperty;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidsProxy;

public final class HangableBlockBehavior extends BukkitBlockBehavior implements PathFindingBlock {
    public static final BlockBehaviorFactory<HangableBlockBehavior> FACTORY = new Factory();
    public final Property<Boolean> hangingProperty;
    public final Property<Boolean> waterloggedProperty;

    private HangableBlockBehavior(BlockDefinition blockDefinition, Property<Boolean> hangingProperty, Property<Boolean> waterloggedProperty) {
        super(blockDefinition);
        this.hangingProperty = hangingProperty;
        this.waterloggedProperty = waterloggedProperty;
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        Object world = context.getLevel().minecraftWorld();
        Object blockPos = LocationUtils.toBlockPos(context.getClickedPos());
        Object fluidType = FluidStateProxy.INSTANCE.getType(BlockGetterProxy.INSTANCE.getFluidState(world, blockPos));
        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.axis() != Direction.Axis.Y) continue;
            ImmutableBlockState blockState = state.with(this.hangingProperty, direction == Direction.UP);
            if (!BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.canSurvive(blockState.customBlockState().minecraftState(), world, blockPos)) continue;
            return this.waterloggedProperty != null ? blockState.with(this.waterloggedProperty, fluidType == FluidsProxy.WATER) : blockState;
        }
        return state;
    }

    @Override
    public boolean canSurvive(Object thisBlock, Object[] args) {
        Object state = args[0];
        Object world = args[1];
        Object blockPos = args[2];
        ImmutableBlockState blockState = BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
        if (blockState == null) return false;
        BooleanProperty hangingProperty = (BooleanProperty) blockState.owner().value().getProperty("hanging");
        if (hangingProperty == null) return false;
        Boolean hanging = blockState.get(hangingProperty);
        Object relativePos = BlockPosProxy.INSTANCE.relative(blockPos, hanging ? DirectionProxy.UP : DirectionProxy.DOWN);
        return BlockProxy.INSTANCE.canSupportCenter(world, relativePos, hanging ? DirectionProxy.DOWN : DirectionProxy.UP);
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args) {
        ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);
        if (state == null) return BlocksProxy.AIR$defaultState;
        if (this.waterloggedProperty != null && state.get(this.waterloggedProperty)) {
            LevelAccessorProxy.INSTANCE.scheduleTick$1(args[updateShape$level], args[updateShape$blockPos], FluidsProxy.WATER, 5);
        }
        if ((state.get(this.hangingProperty) ? DirectionProxy.UP : DirectionProxy.DOWN) == args[updateShape$direction]
                && !BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.canSurvive(args[0], args[updateShape$level], args[updateShape$blockPos])) {
            return BlocksProxy.AIR$defaultState;
        }
        return super.updateShape(thisBlock, args);
    }

    @Override
    public boolean isPathFindable(Object thisBlock, Object[] args) {
        return false;
    }

    private static class Factory implements BlockBehaviorFactory<HangableBlockBehavior> {

        @Override
        public HangableBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new HangableBlockBehavior(
                    block,
                    BlockBehaviorFactory.getProperty(section.path(), block, "hanging", Boolean.class),
                    BlockBehaviorFactory.getOptionalProperty(block, "waterlogged", Boolean.class)
            );
        }
    }
}
