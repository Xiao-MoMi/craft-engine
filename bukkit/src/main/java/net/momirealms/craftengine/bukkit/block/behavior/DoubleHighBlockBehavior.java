package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.block.properties.type.DoubleBlockHalf;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.*;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class DoubleHighBlockBehavior extends AbstractCanSurviveBlockBehavior {
    public static final BlockBehaviorFactory<DoubleHighBlockBehavior> FACTORY = new Factory();
    private final Property<DoubleBlockHalf> halfProperty;

    public DoubleHighBlockBehavior(CustomBlock customBlock, Property<DoubleBlockHalf> halfProperty) {
        super(customBlock, 0);
        this.halfProperty = halfProperty;
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object level = args[updateShape$level];
        Object blockPos = args[updateShape$blockPos];
        Object blockState = args[0];
        ImmutableBlockState customState = BlockStateUtils.getOptionalCustomBlockState(blockState).orElse(null);
        if (customState == null || customState.isEmpty()) return blockState;
        DoubleBlockHalf half = customState.get(this.halfProperty);
        Object direction = args[updateShape$direction];
        if (DirectionUtils.isYAxis(direction) && half == DoubleBlockHalf.LOWER == (direction == CoreReflections.instance$Direction$UP)) {
            ImmutableBlockState neighborState = BlockStateUtils.getOptionalCustomBlockState(args[updateShape$neighborState]).orElse(null);
            if (neighborState == null || neighborState.isEmpty()) return MBlocks.AIR$defaultState;
            DoubleHighBlockBehavior anotherDoorBehavior = neighborState.behavior().getAs(DoubleHighBlockBehavior.class).orElse(null);
            if (anotherDoorBehavior == null) return MBlocks.AIR$defaultState;
            if (neighborState.get(anotherDoorBehavior.halfProperty) != half) {
                return neighborState.with(anotherDoorBehavior.halfProperty, half).customBlockState().literalObject();
            }
            return MBlocks.AIR$defaultState;
        } else if (half == DoubleBlockHalf.LOWER && direction == CoreReflections.instance$Direction$DOWN && !canSurvive(thisBlock, blockState, level, blockPos)) {
            BlockPos pos = LocationUtils.fromBlockPos(blockPos);
            World world = BukkitAdaptors.adapt(FastNMS.INSTANCE.method$Level$getCraftWorld(level));
            WorldPosition position = new WorldPosition(world, Vec3d.atCenterOf(pos));
            world.playBlockSound(position, customState.settings().sounds().breakSound());
            FastNMS.INSTANCE.method$LevelAccessor$levelEvent(level, WorldEvents.BLOCK_BREAK_EFFECT, blockPos, customState.customBlockState().registryId());
            return MBlocks.AIR$defaultState;
        }
        return blockState;
    }

    @Override
    public Object playerWillDestroy(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object level = args[0];
        Object pos = args[1];
        Object state = args[2];
        Object player = args[3];
        ImmutableBlockState blockState = BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
        if (blockState == null || blockState.isEmpty()) {
            return superMethod.call();
        }
        BukkitServerPlayer cePlayer = BukkitAdaptors.adapt(FastNMS.INSTANCE.method$ServerPlayer$getBukkitEntity(player));
        if (cePlayer == null) {
            return superMethod.call();
        }
        Item<ItemStack> item = cePlayer.getItemInHand(InteractionHand.MAIN_HAND);
        if (cePlayer.canInstabuild() || !BlockStateUtils.isCorrectTool(blockState, item)) {
            preventDropFromBottomPart(level, pos, blockState, player);
        }
        return superMethod.call();
    }

    private void preventDropFromBottomPart(Object level, Object pos, ImmutableBlockState state, Object player) {
        if (state.get(this.halfProperty) != DoubleBlockHalf.UPPER) return;
        Object blockPos = FastNMS.INSTANCE.method$BlockPos$relative(pos, CoreReflections.instance$Direction$DOWN);
        Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, blockPos);
        ImmutableBlockState belowState = BlockStateUtils.getOptionalCustomBlockState(blockState).orElse(null);
        if (belowState == null || belowState.isEmpty()) return;
        Optional<DoubleHighBlockBehavior> belowDoubleHighBlockBehavior = belowState.behavior().getAs(DoubleHighBlockBehavior.class);
        if (belowDoubleHighBlockBehavior.isEmpty() || belowState.get(this.halfProperty) != DoubleBlockHalf.LOWER) return;
        FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, MBlocks.AIR$defaultState, UpdateOption.builder().updateSuppressDrops().updateClients().updateNeighbors().build().flags());
        FastNMS.INSTANCE.method$LevelAccessor$levelEvent(level, player, WorldEvents.BLOCK_BREAK_EFFECT, blockPos, belowState.customBlockState().registryId());
    }

    @Override
    protected boolean canSurvive(Object thisBlock, Object state, Object world, Object blockPos) throws Exception {
        ImmutableBlockState customState = BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
        if (customState == null || customState.isEmpty()) return false;
        if (customState.get(this.halfProperty) == DoubleBlockHalf.UPPER) {
            int x = FastNMS.INSTANCE.field$Vec3i$x(blockPos);
            int y = FastNMS.INSTANCE.field$Vec3i$y(blockPos) - 1;
            int z = FastNMS.INSTANCE.field$Vec3i$z(blockPos);
            Object belowPos = FastNMS.INSTANCE.constructor$BlockPos(x, y, z);
            Object belowState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, belowPos);
            Optional<ImmutableBlockState> belowCustomState = BlockStateUtils.getOptionalCustomBlockState(belowState);
            return belowCustomState.filter(immutableBlockState -> immutableBlockState.owner().value() == super.customBlock).isPresent();
        }
        return true;
    }

    @Override
    public void placeMultiState(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object blockState = args[2];
        Object pos = args[1];
        Optional<ImmutableBlockState> immutableBlockState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        immutableBlockState.ifPresent(state -> FastNMS.INSTANCE.method$LevelWriter$setBlock(args[0], LocationUtils.above(pos), state.with(this.halfProperty, DoubleBlockHalf.UPPER).customBlockState().literalObject(), UpdateOption.UPDATE_ALL.flags()));
    }

    @Override
    public boolean hasMultiState(ImmutableBlockState baseState) {
        return baseState.get(this.halfProperty) == DoubleBlockHalf.LOWER;
    }

    @Override
    public boolean canPlaceMultiState(WorldAccessor accessor, BlockPos pos, ImmutableBlockState state) {
        if (pos.y() >= accessor.worldHeight().getMaxBuildHeight() - 1) {
            return false;
        }
        return accessor.getBlockState(pos.above()).isAir();
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        World world  = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (pos.y() < context.getLevel().worldHeight().getMaxBuildHeight() - 1 && world.getBlock(pos.above()).canBeReplaced(context)) {
            return state.with(this.halfProperty, DoubleBlockHalf.LOWER);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static class Factory implements BlockBehaviorFactory<DoubleHighBlockBehavior> {

        @Override
        public DoubleHighBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Property<DoubleBlockHalf> half = (Property<DoubleBlockHalf>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("half"), "warning.config.block.behavior.double_high.missing_half");
            return new DoubleHighBlockBehavior(block, half);
        }
    }
}
