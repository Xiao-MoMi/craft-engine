package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.block.state.properties.SlabType;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.behavior.BlockBoundItemBehavior;
import net.momirealms.craftengine.core.item.behavior.ItemBehavior;
import net.momirealms.craftengine.core.item.context.BlockPlaceContext;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class SlabBlockBehavior extends BukkitBlockBehavior {
    public static final Factory FACTORY = new Factory();
    private final Property<SlabType> typeProperty;

    public SlabBlockBehavior(CustomBlock block, Property<SlabType> typeProperty) {
        super(block);
        this.typeProperty = typeProperty;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean canBeReplaced(BlockPlaceContext context, ImmutableBlockState state) {
        SlabType type = state.get(this.typeProperty);
        Item<ItemStack> item = (Item<ItemStack>) context.getItem();
        if (type == SlabType.DOUBLE || item == null) return false;
        Optional<CustomItem<ItemStack>> itemInHand = item.getCustomItem();
        if (itemInHand.isEmpty()) return false;
        CustomItem<ItemStack> customItem = itemInHand.get();
        Key blockId = null;
        for (ItemBehavior itemBehavior : customItem.behaviors()) {
            if (itemBehavior instanceof BlockBoundItemBehavior behavior) {
                blockId = behavior.block();
            }
        }
        if (blockId == null || !blockId.equals(super.customBlock.id())) return false;
        if (!context.replacingClickedOnBlock()) return true;
        boolean upper = context.getClickLocation().y - (double) context.getClickedPos().y() > (double) 0.5F;
        Direction clickedFace = context.getClickedFace();
        return type == SlabType.BOTTOM ?
                clickedFace == Direction.UP || (upper && clickedFace.axis().isHorizontal()) :
                clickedFace == Direction.DOWN || (!upper && clickedFace.axis().isHorizontal());
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        BlockPos clickedPos = context.getClickedPos();
        ImmutableBlockState blockState = context.getLevel().getBlockAt(clickedPos).customBlockState();
        if (blockState != null && blockState.owner().value() == super.customBlock) {
            if (super.waterloggedProperty != null)
                blockState = blockState.with(super.waterloggedProperty, false);
            return blockState.with(this.typeProperty, SlabType.DOUBLE);
        } else {
            Object fluidState = FastNMS.INSTANCE.method$Level$getFluidState(context.getLevel().serverWorld(), LocationUtils.toBlockPos(clickedPos));
            if (super.waterloggedProperty != null)
                state = state.with(super.waterloggedProperty, FastNMS.INSTANCE.method$FluidState$getType(fluidState) == MFluids.WATER);
            Direction clickedFace = context.getClickedFace();
            return clickedFace == Direction.DOWN || clickedFace != Direction.UP && context.getClickLocation().y - (double) clickedPos.y() > (double) 0.5F ? state.with(this.typeProperty, SlabType.TOP) : state.with(this.typeProperty, SlabType.BOTTOM);
        }
    }

    @Override
    public boolean placeLiquid(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object blockState = args[2];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        return optionalCustomState.filter(state -> state.get(this.typeProperty) != SlabType.DOUBLE && super.placeLiquid(thisBlock, args, superMethod)).isPresent();
    }

    @Override
    public boolean canPlaceLiquid(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object blockState = VersionHelper.isOrAbove1_20_2() ? args[3] : args[2];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        return optionalCustomState.filter(state -> state.get(this.typeProperty) != SlabType.DOUBLE && super.canPlaceLiquid(thisBlock, args, superMethod)).isPresent();
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object blockState = args[0];
        if (super.waterloggedProperty == null) return blockState;
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return blockState;
        if (optionalCustomState.get().get(super.waterloggedProperty)) {
            FastNMS.INSTANCE.method$LevelAccessor$scheduleFluidTick(VersionHelper.isOrAbove1_21_2() ? args[2] : args[3], VersionHelper.isOrAbove1_21_2() ? args[3] : args[4], MFluids.WATER, 5);
        }
        return blockState;
    }

    @Override
    public boolean isPathFindable(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object type = VersionHelper.isOrAbove1_20_5() ? args[1] : args[3];
        Object blockState = args[0];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return false;
        if (type == CoreReflections.instance$PathComputationType$WATER) {
            return super.waterloggedProperty != null && optionalCustomState.get().get(this.typeProperty) != SlabType.DOUBLE && optionalCustomState.get().get(super.waterloggedProperty);
        }
        return false;
    }

    public static class Factory implements BlockBehaviorFactory {

        @SuppressWarnings("unchecked")
        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Property<SlabType> type = (Property<SlabType>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("type"), "warning.config.block.behavior.slab.missing_type");
            return new SlabBlockBehavior(block, type);
        }
    }
}
