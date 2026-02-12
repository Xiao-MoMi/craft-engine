package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
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
import net.momirealms.craftengine.core.block.behavior.IsPathFindableBlockBehavior;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.block.properties.type.DoorHinge;
import net.momirealms.craftengine.core.block.properties.type.DoubleBlockHalf;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.HorizontalDirection;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.*;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.bukkit.Bukkit;
import org.bukkit.GameEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

@SuppressWarnings("DuplicatedCode")
public class DoorBlockBehavior extends AbstractCanSurviveBlockBehavior implements IsPathFindableBlockBehavior {
    public static final BlockBehaviorFactory<DoorBlockBehavior> FACTORY = new Factory();
    private final Property<DoubleBlockHalf> halfProperty;
    private final Property<HorizontalDirection> facingProperty;
    private final Property<DoorHinge> hingeProperty;
    private final Property<Boolean> poweredProperty;
    private final Property<Boolean> openProperty;
    private final boolean canOpenWithHand;
    private final boolean canOpenByWindCharge;
    private final SoundData openSound;
    private final SoundData closeSound;

    public DoorBlockBehavior(CustomBlock block,
                             Property<DoubleBlockHalf> halfProperty,
                             Property<HorizontalDirection> facingProperty,
                             Property<DoorHinge> hingeProperty,
                             Property<Boolean> poweredProperty,
                             Property<Boolean> openProperty,
                             boolean canOpenWithHand,
                             boolean canOpenByWindCharge,
                             SoundData openSound,
                             SoundData closeSound) {
        super(block, 0);
        this.halfProperty = halfProperty;
        this.facingProperty = facingProperty;
        this.hingeProperty = hingeProperty;
        this.poweredProperty = poweredProperty;
        this.openProperty = openProperty;
        this.canOpenWithHand = canOpenWithHand;
        this.canOpenByWindCharge = canOpenByWindCharge;
        this.openSound = openSound;
        this.closeSound = closeSound;
    }

    public boolean isOpen(ImmutableBlockState state) {
        return state.get(this.openProperty);
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object level = args[updateShape$level];
        Object blockPos = args[updateShape$blockPos];
        Object blockState = args[0];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) {
            return blockState;
        }
        ImmutableBlockState customState = optionalCustomState.get();
        DoubleBlockHalf half = customState.get(this.halfProperty);
        Object direction = VersionHelper.isOrAbove1_21_2() ? args[4] : args[1];
        if (DirectionUtils.isYAxis(direction) && half == DoubleBlockHalf.LOWER == (direction == CoreReflections.instance$Direction$UP)) {
            Optional<ImmutableBlockState> optionalNeighborState = BlockStateUtils.getOptionalCustomBlockState(args[updateShape$neighborState]);
            if (optionalNeighborState.isEmpty()) {
                return MBlocks.AIR$defaultState;
            }
            ImmutableBlockState neighborState = optionalNeighborState.get();
            Optional<DoorBlockBehavior> anotherDoorBehavior = neighborState.behavior().getAs(DoorBlockBehavior.class);
            if (anotherDoorBehavior.isEmpty()) {
                return MBlocks.AIR$defaultState;
            }
            if (neighborState.get(anotherDoorBehavior.get().halfProperty) != half) {
                return neighborState.with(anotherDoorBehavior.get().halfProperty, half).customBlockState().literalObject();
            }
            return MBlocks.AIR$defaultState;
        } else {
            if (half == DoubleBlockHalf.LOWER && direction == CoreReflections.instance$Direction$DOWN
                    && !canSurvive(thisBlock, blockState, level, blockPos)) {
                MultiHighBlockBehavior.playBreakEffect(customState, blockPos, level);
                return MBlocks.AIR$defaultState;
            }
            return blockState;
        }
    }

    @Override
    public Object playerWillDestroy(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object level = args[0];
        Object pos = args[1];
        Object state = args[2];
        Object player = args[3];
        ImmutableBlockState blockState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(state));
        if (blockState == null || blockState.isEmpty()) return superMethod.call();
        org.bukkit.entity.Player bukkitPlayer = FastNMS.INSTANCE.method$ServerPlayer$getBukkitEntity(player);
        BukkitServerPlayer cePlayer = BukkitCraftEngine.instance().adapt(bukkitPlayer);
        Item<ItemStack> item = cePlayer.getItemInHand(InteractionHand.MAIN_HAND);
        if (cePlayer.canInstabuild() || !BlockStateUtils.isCorrectTool(blockState, item)) {
            preventDropFromBottomPart(level, pos, blockState, player);
        }
        return superMethod.call();
    }

    private void preventDropFromBottomPart(Object level, Object pos, ImmutableBlockState state, Object player) {
        DoubleBlockHalf half = state.get(this.halfProperty);
        if (half == DoubleBlockHalf.UPPER) {
            Object blockPos = FastNMS.INSTANCE.method$BlockPos$relative(pos, CoreReflections.instance$Direction$DOWN);
            Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, blockPos);
            ImmutableBlockState belowState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(blockState));
            if (belowState == null || belowState.isEmpty()) return;
            Optional<DoorBlockBehavior> belowDoorBehavior = belowState.behavior().getAs(DoorBlockBehavior.class);
            if (belowDoorBehavior.isEmpty() || belowState.get(this.halfProperty) != DoubleBlockHalf.LOWER) return;
            FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, MBlocks.AIR$defaultState, UpdateOption.builder().updateSuppressDrops().updateClients().updateNeighbors().build().flags());
            FastNMS.INSTANCE.method$LevelAccessor$levelEvent(level, player, WorldEvents.BLOCK_BREAK_EFFECT, blockPos, belowState.customBlockState().registryId());
        }
    }

    @Override
    public void onExplosionHit(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        if (this.canOpenByWindCharge && FastNMS.INSTANCE.method$Explosion$canTriggerBlocks(args[3])) {
            Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(args[0]);
            if (optionalCustomState.isEmpty()) return;
            ImmutableBlockState state = optionalCustomState.get();
            if (state.get(this.poweredProperty)) return;
            if (state.get(this.halfProperty) == DoubleBlockHalf.LOWER) {
                this.setOpen(null, args[1], state, LocationUtils.fromBlockPos(args[2]), !this.isOpen(state));
            }
        }
    }

    @Override
    public boolean canPlaceMultiState(WorldAccessor accessor, BlockPos pos, ImmutableBlockState state) {
        if (pos.y() >= accessor.worldHeight().getMaxBuildHeight() - 1) {
            return false;
        }
        return accessor.getBlockState(pos.above()).isAir();
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
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        World world  = context.getLevel();
        Object level = world.serverWorld();
        BlockPos pos = context.getClickedPos();
        if (pos.y() < world.worldHeight().getMaxBuildHeight() - 1 && world.getBlock(pos.above()).canBeReplaced(context)) {
            boolean hasSignal = FastNMS.INSTANCE.method$SignalGetter$hasNeighborSignal(level, LocationUtils.toBlockPos(pos)) || FastNMS.INSTANCE.method$SignalGetter$hasNeighborSignal(level, LocationUtils.toBlockPos(pos.above()));
            return state.with(this.poweredProperty, hasSignal)
                    .with(this.facingProperty, context.getHorizontalDirection().toHorizontalDirection())
                    .with(this.openProperty, hasSignal)
                    .with(this.halfProperty, DoubleBlockHalf.LOWER)
                    .with(this.hingeProperty, getHinge(context));
        }
        return null;
    }

    private DoorHinge getHinge(BlockPlaceContext context) {
        Object serverLevel = context.getLevel().serverWorld();
        BlockPos clickedPos = context.getClickedPos();
        Direction horizontalDirection = context.getHorizontalDirection();
        BlockPos blockPos = clickedPos.above();

        Direction counterClockWise = horizontalDirection.counterClockWise();
        Object blockPos1 = LocationUtils.toBlockPos(clickedPos.relative(counterClockWise));
        Object blockState1 = FastNMS.INSTANCE.method$BlockGetter$getBlockState(serverLevel, blockPos1);
        Object blockPos2 = LocationUtils.toBlockPos(blockPos.relative(counterClockWise));
        Object blockState2 = FastNMS.INSTANCE.method$BlockGetter$getBlockState(serverLevel, blockPos2);

        Direction clockWise = horizontalDirection.clockWise();
        Object blockPos3 = LocationUtils.toBlockPos(clickedPos.relative(clockWise));
        Object blockState3 = FastNMS.INSTANCE.method$BlockGetter$getBlockState(serverLevel, blockPos3);
        Object blockPos4 = LocationUtils.toBlockPos(blockPos.relative(clockWise));
        Object blockState4 = FastNMS.INSTANCE.method$BlockGetter$getBlockState(serverLevel, blockPos4);

        int i = (FastNMS.INSTANCE.method$BlockStateBase$isCollisionShapeFullBlock(blockState1, serverLevel, blockPos1) ? -1 : 0) +
                (FastNMS.INSTANCE.method$BlockStateBase$isCollisionShapeFullBlock(blockState2, serverLevel, blockPos2) ? -1 : 0) +
                (FastNMS.INSTANCE.method$BlockStateBase$isCollisionShapeFullBlock(blockState3, serverLevel, blockPos3) ? 1 : 0) +
                (FastNMS.INSTANCE.method$BlockStateBase$isCollisionShapeFullBlock(blockState4, serverLevel, blockPos4) ? 1 : 0);

        boolean anotherDoor1 = isAnotherDoor(blockState1);
        boolean anotherDoor2 = isAnotherDoor(blockState3);
        if ((!anotherDoor1 || anotherDoor2) && i <= 0) {
            if ((!anotherDoor2 || anotherDoor1) && i == 0) {
                int stepX = horizontalDirection.stepX();
                int stepZ = horizontalDirection.stepZ();
                Vec3d clickLocation = context.getClickedLocation();
                double d = clickLocation.x - (double) clickedPos.x();
                double d1 = clickLocation.z - (double) clickedPos.z();
                return stepX < 0 && d1 < (double) 0.5F || stepX > 0 && d1 > (double) 0.5F || stepZ < 0 && d > (double) 0.5F || stepZ > 0 && d < (double) 0.5F ? DoorHinge.RIGHT : DoorHinge.LEFT;
            } else {
                return DoorHinge.LEFT;
            }
        } else {
            return DoorHinge.RIGHT;
        }
    }

    private boolean isAnotherDoor(Object blockState) {
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) {
            BlockData blockData = BlockStateUtils.fromBlockData(blockState);
            return blockData instanceof Door door && door.getHalf() == Bisected.Half.BOTTOM;
        } else {
            Optional<DoorBlockBehavior> optional = optionalCustomState.get().behavior().getAs(DoorBlockBehavior.class);
            return optional.isPresent() && optionalCustomState.get().get(optional.get().halfProperty) == DoubleBlockHalf.LOWER;
        }
    }

    public void setOpen(@Nullable Player player, Object serverLevel, ImmutableBlockState state, BlockPos pos, boolean isOpen) {
        if (isOpen(state) != isOpen) {
            org.bukkit.World world = FastNMS.INSTANCE.method$Level$getCraftWorld(serverLevel);
            FastNMS.INSTANCE.method$LevelWriter$setBlock(serverLevel, LocationUtils.toBlockPos(pos), state.with(this.openProperty, isOpen).customBlockState().literalObject(), UpdateOption.builder().updateImmediate().updateClients().build().flags());
            world.sendGameEvent(player == null ? null : (org.bukkit.entity.Player) player.platformPlayer(), isOpen ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, new Vector(pos.x(), pos.y(), pos.z()));
            SoundData soundData = isOpen ? this.openSound : this.closeSound;
            if (soundData != null) {
                BukkitAdaptors.adapt(world).playBlockSound(
                        new Vec3d(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5),
                        soundData
                );
            }
        }
    }

    @Override
    public InteractionResult useWithoutItem(UseOnContext context, ImmutableBlockState state) {
        if (!this.canOpenWithHand) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        World world = context.getLevel();
        if (player != null) {
            Location location = new Location((org.bukkit.World) world.platformWorld(), pos.x, pos.y, pos.z);
            if (!BukkitCraftEngine.instance().antiGriefProvider().test((org.bukkit.entity.Player) player.platformPlayer(), Flag.OPEN_DOOR, location)) {
                return InteractionResult.SUCCESS_AND_CANCEL;
            }
        }
        setOpen(player, world.serverWorld(), state, pos, !state.get(this.openProperty));
        return InteractionResult.SUCCESS_AND_CANCEL;
    }

    @Override
    public boolean isPathFindable(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object type = VersionHelper.isOrAbove1_20_5() ? args[1] : args[3];
        Object blockState = args[0];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return false;
        if (type == CoreReflections.instance$PathComputationType$LAND || type == CoreReflections.instance$PathComputationType$AIR) {
            return optionalCustomState.get().get(this.openProperty);
        }
        return false;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void neighborChanged(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object blockPos = args[2];
        Object level = args[1];
        Object blockState = args[0];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return;
        ImmutableBlockState customState = optionalCustomState.get();
        Object anotherHalfPos = customState.get(this.halfProperty) == DoubleBlockHalf.LOWER ? LocationUtils.above(blockPos) : LocationUtils.below(blockPos);
        Block bukkitBlock = FastNMS.INSTANCE.method$CraftBlock$at(level, blockPos);
        Block anotherBukkitBlock = FastNMS.INSTANCE.method$CraftBlock$at(level, anotherHalfPos);
        int power = Math.max(bukkitBlock.getBlockPower(), anotherBukkitBlock.getBlockPower());
        int oldPower = customState.get(this.poweredProperty) ? 15 : 0;
        if (oldPower == 0 ^ power == 0) {
            BlockRedstoneEvent event = new BlockRedstoneEvent(bukkitBlock, oldPower, power);
            Bukkit.getPluginManager().callEvent(event);
            boolean flag = event.getNewCurrent() > 0;
            if (flag != customState.get(this.openProperty)) {
                org.bukkit.World world = FastNMS.INSTANCE.method$Level$getCraftWorld(level);
                world.sendGameEvent(null, flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, new Vector(bukkitBlock.getX(), bukkitBlock.getY(), bukkitBlock.getZ()));
                SoundData soundData = flag ? this.openSound : this.closeSound;
                if (soundData != null) {
                    BukkitAdaptors.adapt(world).playBlockSound(
                            new Vec3d(FastNMS.INSTANCE.field$Vec3i$x(blockPos) + 0.5, FastNMS.INSTANCE.field$Vec3i$y(blockPos) + 0.5, FastNMS.INSTANCE.field$Vec3i$z(blockPos) + 0.5),
                            soundData
                    );
                }
            }
            FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, customState.with(this.poweredProperty, flag).with(this.openProperty, flag).customBlockState().literalObject(), UpdateOption.Flags.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean canSurvive(Object thisBlock, Object state, Object world, Object blockPos) throws Exception {
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(state);
        if (optionalCustomState.isEmpty()) return false;
        int x = FastNMS.INSTANCE.field$Vec3i$x(blockPos);
        int y = FastNMS.INSTANCE.field$Vec3i$y(blockPos) - 1;
        int z = FastNMS.INSTANCE.field$Vec3i$z(blockPos);
        Object belowPos = FastNMS.INSTANCE.constructor$BlockPos(x, y, z);
        Object belowState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, belowPos);
        if (optionalCustomState.get().get(this.halfProperty) == DoubleBlockHalf.UPPER) {
            Optional<ImmutableBlockState> belowCustomState = BlockStateUtils.getOptionalCustomBlockState(belowState);
            return belowCustomState.filter(immutableBlockState -> immutableBlockState.owner().value() == super.customBlock).isPresent();
        } else {
            return FastNMS.INSTANCE.method$BlockStateBase$isFaceSturdy(
                    belowState, world, belowPos, CoreReflections.instance$Direction$UP,
                    CoreReflections.instance$SupportType$FULL
            );
        }
    }

    @SuppressWarnings("unchecked")
    private static class Factory implements BlockBehaviorFactory<DoorBlockBehavior> {
        @Override
        public DoorBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Property<DoubleBlockHalf> half = (Property<DoubleBlockHalf>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("half"), "warning.config.block.behavior.door.missing_half");
            Property<HorizontalDirection> facing = (Property<HorizontalDirection>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("facing"), "warning.config.block.behavior.door.missing_facing");
            Property<DoorHinge> hinge = (Property<DoorHinge>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("hinge"), "warning.config.block.behavior.door.missing_hinge");
            Property<Boolean> open = (Property<Boolean>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("open"), "warning.config.block.behavior.door.missing_open");
            Property<Boolean> powered = (Property<Boolean>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("powered"), "warning.config.block.behavior.door.missing_powered");
            boolean canOpenWithHand = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("can-open-with-hand", true), "can-open-with-hand");
            boolean canOpenByWindCharge = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("can-open-by-wind-charge", true), "can-open-by-wind-charge");
            Map<String, Object> sounds = (Map<String, Object>) arguments.get("sounds");
            SoundData openSound = null;
            SoundData closeSound = null;
            if (sounds != null) {
                openSound = Optional.ofNullable(sounds.get("open")).map(obj -> SoundData.create(obj, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.ranged(0.9f, 1f))).orElse(null);
                closeSound = Optional.ofNullable(sounds.get("close")).map(obj -> SoundData.create(obj, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.ranged(0.9f, 1f))).orElse(null);
            }
            return new DoorBlockBehavior(block, half, facing, hinge, powered, open, canOpenWithHand, canOpenByWindCharge, openSound, closeSound);
        }
    }
}
