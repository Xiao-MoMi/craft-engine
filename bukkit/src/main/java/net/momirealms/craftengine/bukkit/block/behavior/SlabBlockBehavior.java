package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.PathFindingBlock;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.block.property.type.SlabType;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemDefinition;
import net.momirealms.craftengine.core.item.behavior.BlockItem;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.ItemUtils;
import net.momirealms.craftengine.core.util.MutableBoolean;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.pathfinder.PathComputationTypeProxy;

import java.util.Optional;

public final class SlabBlockBehavior extends WaterloggedBlockBehavior implements PathFindingBlock {
    public static final BlockBehaviorFactory<SlabBlockBehavior> FACTORY = new Factory();
    public final Property<SlabType> typeProperty;

    private SlabBlockBehavior(BlockDefinition block,
                              Property<SlabType> typeProperty,
                              Property<Boolean> waterloggedProperty) {
        super(block, waterloggedProperty);
        this.typeProperty = typeProperty;
    }

    @Override
    public boolean canBeReplaced(BlockPlaceContext context, ImmutableBlockState state) {
        SlabType type = state.get(this.typeProperty);
        Item item = context.getItem();
        if (type == SlabType.DOUBLE || ItemUtils.isEmpty(item)) return false;
        Optional<ItemDefinition> itemInHand = item.getDefinition();
        if (itemInHand.isEmpty()) return false;
        ItemDefinition itemDefinition = itemInHand.get();

        MutableBoolean sameId = new MutableBoolean(false);
        itemDefinition.behavior().let(BlockItem.class, b -> {
            if (b.block().equals(super.blockDefinition.id())) {
                sameId.set(true);
            }
        });

        if (!sameId.booleanValue()) return false;
        if (!context.replacingClickedBlock()) return true;
        boolean upper = context.getClickedLocation().y - (double) context.getClickedPos().y() > (double) 0.5F;
        Direction clickedFace = context.getClickedFace();
        return type == SlabType.BOTTOM ?
                clickedFace == Direction.UP || (upper && clickedFace.axis().isHorizontal()) :
                clickedFace == Direction.DOWN || (!upper && clickedFace.axis().isHorizontal());
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        BlockPos clickedPos = context.getClickedPos();
        ImmutableBlockState blockState = context.getLevel().getBlock(clickedPos).customBlockState();
        if (blockState != null && blockState.owner().value() == super.blockDefinition) {
            if (this.waterloggedProperty != null)
                blockState = blockState.with(this.waterloggedProperty, false);
            return blockState.with(this.typeProperty, SlabType.DOUBLE);
        } else {
            Object fluidState = BlockGetterProxy.INSTANCE.getFluidState(context.getLevel().minecraftWorld(), LocationUtils.toBlockPos(clickedPos));
            if (this.waterloggedProperty != null)
                state = state.with(this.waterloggedProperty, FluidStateProxy.INSTANCE.getType(fluidState) == FluidsProxy.WATER);
            Direction clickedFace = context.getClickedFace();
            return clickedFace == Direction.DOWN || clickedFace != Direction.UP && context.getClickedLocation().y - (double) clickedPos.y() > (double) 0.5F ? state.with(this.typeProperty, SlabType.TOP) : state.with(this.typeProperty, SlabType.BOTTOM);
        }
    }

    @Override
    public boolean placeLiquid(Object thisBlock, Object[] args) {
        Object blockState = args[2];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        return optionalCustomState.filter(state -> state.get(this.typeProperty) != SlabType.DOUBLE && super.placeLiquid(thisBlock, args)).isPresent();
    }

    @Override
    public boolean canPlaceLiquid(Object thisBlock, Object[] args) {
        Object blockState = VersionHelper.isOrAbove1_20_2 ? args[3] : args[2];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        return optionalCustomState.filter(state -> state.get(this.typeProperty) != SlabType.DOUBLE && super.canPlaceLiquid(thisBlock, args)).isPresent();
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args) {
        Object blockState = args[0];
        if (this.waterloggedProperty == null) return blockState;
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return blockState;
        if (optionalCustomState.get().get(this.waterloggedProperty)) {
            LevelAccessorProxy.INSTANCE.scheduleTick$1(VersionHelper.isOrAbove1_21_2 ? args[2] : args[3], VersionHelper.isOrAbove1_21_2 ? args[3] : args[4], FluidsProxy.WATER, 5);
        }
        return blockState;
    }

    @Override
    public boolean isPathFindable(Object thisBlock, Object[] args) {
        Object type = VersionHelper.isOrAbove1_20_5 ? args[1] : args[3];
        Object blockState = args[0];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return false;
        if (type == PathComputationTypeProxy.WATER) {
            return this.waterloggedProperty != null && optionalCustomState.get().get(this.typeProperty) != SlabType.DOUBLE && optionalCustomState.get().get(this.waterloggedProperty);
        }
        return false;
    }

    private static class Factory implements BlockBehaviorFactory<SlabBlockBehavior> {

        @Override
        public SlabBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new SlabBlockBehavior(
                    block,
                    BlockBehaviorFactory.getProperty(section.path(), block, "type", SlabType.class),
                    BlockBehaviorFactory.getOptionalProperty(block, "waterlogged", Boolean.class)
            );
        }
    }
}
