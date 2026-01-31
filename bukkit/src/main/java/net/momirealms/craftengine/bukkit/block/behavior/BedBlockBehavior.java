package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.block.entity.BedBlockEntity;
import net.momirealms.craftengine.bukkit.block.entity.BukkitBlockEntityTypes;
import net.momirealms.craftengine.bukkit.entity.seat.BukkitSeat;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.EntityBlockBehavior;
import net.momirealms.craftengine.core.block.entity.BlockEntity;
import net.momirealms.craftengine.core.block.entity.BlockEntityType;
import net.momirealms.craftengine.core.block.entity.tick.BlockEntityTicker;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.block.properties.type.BedPart;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.seat.SeatConfig;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.HorizontalDirection;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.*;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.bukkit.inventory.ItemStack;
import org.joml.Vector3f;

import java.util.Map;
import java.util.concurrent.Callable;

public class BedBlockBehavior extends BukkitBlockBehavior implements EntityBlockBehavior {
    public static final BlockBehaviorFactory<BedBlockBehavior> FACTORY = new Factory();
    public final Property<HorizontalDirection> facingProperty;
    public final Property<BedPart> partProperty;
    public final SeatConfig seatConfig;
    public final Vector3f sleepOffset;

    public BedBlockBehavior(
            CustomBlock customBlock,
            Property<HorizontalDirection> facingProperty,
            Property<BedPart> partProperty,
            SeatConfig seatConfig,
            Vector3f sleepOffset) {
        super(customBlock);
        this.facingProperty = facingProperty;
        this.partProperty = partProperty;
        this.seatConfig = seatConfig;
        this.sleepOffset = sleepOffset;
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);
        if (state == null) {
            return superMethod.call();
        }
        BedBlockBehavior behavior = state.behavior().getAs(BedBlockBehavior.class).orElse(null);
        if (behavior == null) {
            return superMethod.call();
        }
        HorizontalDirection direction = state.get(behavior.facingProperty);
        BedPart bedPart = state.get(behavior.partProperty);
        direction = bedPart == BedPart.FOOT ? direction : direction.opposite();
        if (DirectionUtils.toNMSDirection(direction) != args[updateShape$direction]) {
            return superMethod.call();
        }
        Object level = args[updateShape$level];
        Object blockPos = args[updateShape$blockPos];
        ImmutableBlockState neighborState = BlockStateUtils.getOptionalCustomBlockState(args[updateShape$neighborState]).orElse(null);
        if (neighborState == null) {
            MultiHighBlockBehavior.playBreakEffect(state, blockPos, level);
            return MBlocks.AIR$defaultState;
        }
        if (state.owner() != neighborState.owner()) {
            MultiHighBlockBehavior.playBreakEffect(state, blockPos, level);
            return MBlocks.AIR$defaultState;
        }
        BedBlockBehavior neighborBehavior = neighborState.behavior().getAs(BedBlockBehavior.class).orElse(null);
        if (neighborBehavior == null) {
            MultiHighBlockBehavior.playBreakEffect(state, blockPos, level);
            return MBlocks.AIR$defaultState;
        }
        if (state.get(behavior.partProperty) == neighborState.get(neighborBehavior.partProperty)) {
            MultiHighBlockBehavior.playBreakEffect(state, blockPos, level);
            return MBlocks.AIR$defaultState;
        }
        return args[0];
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Object playerWillDestroy(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object player = args[3];
        ImmutableBlockState blockState = BlockStateUtils.getOptionalCustomBlockState(args[2]).orElse(null);
        if (blockState == null || blockState.isEmpty()) {
            return superMethod.call();
        }
        BukkitServerPlayer serverPlayer = BukkitAdaptors.adapt(FastNMS.INSTANCE.method$ServerPlayer$getBukkitEntity(player));
        if (serverPlayer == null) {
            return superMethod.call();
        }
        Item<ItemStack> item = serverPlayer.getItemInHand(InteractionHand.MAIN_HAND);
        if (serverPlayer.canInstabuild() || !BlockStateUtils.isCorrectTool(blockState, item)) {
            preventDropFromHeadPart(args[0], args[1], blockState, player);
        }
        return superMethod.call();
    }

    private void preventDropFromHeadPart(Object level, Object pos, ImmutableBlockState state, Object player) {
        BedBlockBehavior behavior = state.behavior().getAs(BedBlockBehavior.class).orElse(null);
        if (behavior == null) {
            return;
        }
        BedPart bedPart = state.get(behavior.partProperty);
        if (bedPart == BedPart.HEAD) {
            return;
        }
        HorizontalDirection direction = state.get(behavior.facingProperty);
        pos = FastNMS.INSTANCE.method$BlockPos$offset(pos, direction.stepX(), 0, direction.stepZ());
        Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, pos);
        ImmutableBlockState headState = BlockStateUtils.getOptionalCustomBlockState(blockState).orElse(null);
        if (headState == null || headState.isEmpty()) {
            return;
        }
        BedBlockBehavior headBehavior = headState.behavior().getAs(BedBlockBehavior.class).orElse(null);
        if (headBehavior == null) {
            return;
        }
        if (state.owner() != headState.owner() || headState.get(headBehavior.partProperty) != BedPart.HEAD) {
            return;
        }
        Object emptyState = FastNMS.INSTANCE.method$FluidState$getType(FastNMS.INSTANCE.field$BlockBehaviour$BlockStateBase$fluidState(blockState)) == MFluids.WATER
                ? MBlocks.WATER$defaultState
                : MBlocks.AIR$defaultState;
        FastNMS.INSTANCE.method$LevelWriter$setBlock(level, pos, emptyState, UpdateOption.builder().updateSuppressDrops().updateClients().updateNeighbors().build().flags());
        FastNMS.INSTANCE.method$LevelAccessor$levelEvent(level, player, WorldEvents.BLOCK_BREAK_EFFECT, pos, headState.customBlockState().registryId());
    }

    @Override
    public void placeMultiState(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object level = args[0];
        Object pos = args[1];
        Object blockState = args[2];
        ImmutableBlockState state = BlockStateUtils.getOptionalCustomBlockState(blockState).orElse(null);
        if (state == null) {
            return;
        }
        BedBlockBehavior behavior = state.behavior().getAs(BedBlockBehavior.class).orElse(null);
        if (behavior == null) {
            return;
        }
        HorizontalDirection direction = state.get(behavior.facingProperty);
        FastNMS.INSTANCE.method$LevelWriter$setBlock(
                level,
                FastNMS.INSTANCE.method$BlockPos$offset(pos, direction.stepX(), 0, direction.stepZ()),
                state.with(behavior.partProperty, BedPart.HEAD).customBlockState().literalObject(),
                UpdateOption.UPDATE_ALL.flags()
        );
    }

    @Override
    public boolean hasMultiState(ImmutableBlockState baseState) {
        return true;
    }

    @Override
    public boolean canPlaceMultiState(WorldAccessor accessor, BlockPos pos, ImmutableBlockState state) {
        if (!accessor.getBlockState(pos).isAir()) return false;
        BedBlockBehavior behavior = state.behavior().getAs(BedBlockBehavior.class).orElse(null);
        if (behavior == null) {
            return false;
        }
        BedPart bedPart = state.get(behavior.partProperty);
        HorizontalDirection direction = state.get(behavior.facingProperty);
        if (bedPart == BedPart.FOOT) {
            direction = direction.opposite();
        }
        return accessor.getBlockState(pos.offset(direction.stepX(), 0, direction.stepZ())).isAir();
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        World world  = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BedBlockBehavior behavior = state.behavior().getAs(BedBlockBehavior.class).orElse(null);
        if (behavior == null) {
            return null;
        }
        if (!world.getBlock(pos).canBeReplaced(context)) {
            return null;
        }
        BedPart bedPart = state.get(behavior.partProperty);
        HorizontalDirection direction = state.get(behavior.facingProperty);
        if (bedPart == BedPart.FOOT) {
            direction = direction.opposite();
        }
        if (!world.getBlock(pos.offset(direction.stepX(), 0, direction.stepZ())).canBeReplaced(context)) {
            return null;
        }
        return state.with(behavior.facingProperty, context.getHorizontalDirection().toHorizontalDirection())
                .with(behavior.partProperty, BedPart.FOOT);
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> blockEntityType(ImmutableBlockState state) {
        return EntityBlockBehavior.blockEntityTypeHelper(BukkitBlockEntityTypes.BED);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, ImmutableBlockState state) {
        BedBlockBehavior behavior = state.behavior().getAs(BedBlockBehavior.class).orElseThrow();
        if (state.get(behavior.partProperty) == BedPart.HEAD) {
            return new BedBlockEntity.Controller(pos, state);
        } else {
            return new BedBlockEntity.Requestor(pos, state);
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> createAsyncBlockEntityTicker(CEWorld level, ImmutableBlockState state, BlockEntityType<T> blockEntityType) {
        BedBlockBehavior behavior = state.behavior().getAs(BedBlockBehavior.class).orElse(null);
        if (behavior == null) {
            return null;
        }
        if (state.get(behavior.partProperty) == BedPart.FOOT) {
            return null;
        }
        return EntityBlockBehavior.createTickerHelper(BedBlockEntity.Controller::tick);
    }

    @Override
    public InteractionResult useOnBlock(UseOnContext context, ImmutableBlockState state) {
        World world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BedBlockBehavior behavior = state.behavior().getAs(BedBlockBehavior.class).orElse(null);
        if (behavior == null) {
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        if (state.get(behavior.partProperty) == BedPart.HEAD) {
            HorizontalDirection direction = state.get(behavior.facingProperty).opposite();
            ImmutableBlockState otherState = world.getBlock(pos.x + direction.stepX(), pos.y, pos.z + direction.stepZ()).customBlockState();
            if (otherState == null || otherState.owner() != state.owner()) {
                return InteractionResult.SUCCESS_AND_CANCEL;
            }
            BedBlockBehavior otherBehavior = otherState.behavior().getAs(BedBlockBehavior.class).orElse(null);
            if (otherBehavior == null || otherState.get(otherBehavior.partProperty) == BedPart.HEAD) {
                return InteractionResult.SUCCESS_AND_CANCEL;
            }
        } else {
            HorizontalDirection direction = state.get(behavior.facingProperty);
            ImmutableBlockState otherState = world.getBlock(pos.x + direction.stepX(), pos.y, pos.z + direction.stepZ()).customBlockState();
            if (otherState == null || otherState.owner() != state.owner()) {
                return InteractionResult.SUCCESS_AND_CANCEL;
            }
            BedBlockBehavior otherBehavior = otherState.behavior().getAs(BedBlockBehavior.class).orElse(null);
            if (otherBehavior == null || otherState.get(otherBehavior.partProperty) == BedPart.FOOT) {
                return InteractionResult.SUCCESS_AND_CANCEL;
            }
        }
        BlockEntity blockEntity = world.storageWorld().getBlockEntityAtIfLoaded(pos);
        if (blockEntity instanceof BedBlockEntity bed
                && bed.occupier() == null
                && bed.seat() instanceof BukkitSeat<?> seat
                && !seat.isOccupied()
                && context.getPlayer() instanceof BukkitServerPlayer player
                && !player.isSecondaryUseActive()) {
            player.setBedBlockEntity(bed);
            bed.setOccupier(player);
        }
        return InteractionResult.SUCCESS_AND_CANCEL;
    }

    private static class Factory implements BlockBehaviorFactory<BedBlockBehavior> {

        @SuppressWarnings("unchecked")
        @Override
        public BedBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            if (!VersionHelper.isOrAbove1_20_2()) {
                throw new UnsupportedOperationException("bed_block requires at least 1.20.2");
            }
            Property<HorizontalDirection> facingProperty = (Property<HorizontalDirection>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("facing"), "warning.config.block.behavior.bed.missing_facing");
            Property<BedPart> partProperty = (Property<BedPart>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("part"), "warning.config.block.behavior.bed.missing_part");
            SeatConfig seatConfig;
            SeatConfig[] seats = SeatConfig.fromObj(arguments.get("seat"));
            if (seats.length == 0) {
                seatConfig = new SeatConfig(new Vector3f(0, 0, 0), 0, true);
            } else {
                seatConfig = seats[0];
            }
            Vector3f sleepOffset = ResourceConfigUtils.getAsVector3f(arguments.get("sleep-offset"), "sleep-offset");
            return new BedBlockBehavior(block, facingProperty, partProperty, seatConfig, sleepOffset);
        }
    }
}
