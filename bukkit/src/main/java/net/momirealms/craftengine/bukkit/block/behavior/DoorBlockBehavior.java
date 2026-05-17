package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LevelUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.PathFindingBlock;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.block.property.type.DoorHinge;
import net.momirealms.craftengine.core.block.property.type.DoubleBlockHalf;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.*;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.block.CraftBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.core.AxisProxy;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.core.Vec3iProxy;
import net.momirealms.craftengine.proxy.minecraft.server.level.ServerPlayerProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.*;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.SupportTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.pathfinder.PathComputationTypeProxy;
import org.bukkit.Bukkit;
import org.bukkit.GameEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Door;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.Optional;

import static net.momirealms.craftengine.core.block.UpdateFlags.*;

@SuppressWarnings("DuplicatedCode")
public final class DoorBlockBehavior extends AbstractCanSurviveBlockBehavior
        implements PathFindingBlock {
    public static final BlockBehaviorFactory<DoorBlockBehavior> FACTORY = new Factory();
    public final Property<DoubleBlockHalf> halfProperty;
    public final Property<Direction> facingProperty;
    public final Property<DoorHinge> hingeProperty;
    public final Property<Boolean> poweredProperty;
    public final Property<Boolean> openProperty;
    public final boolean canOpenWithHand;
    public final boolean canOpenByWindCharge;
    public final SoundData openSound;
    public final SoundData closeSound;

    private DoorBlockBehavior(BlockDefinition block,
                              Property<DoubleBlockHalf> halfProperty,
                              Property<Direction> facingProperty,
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
    public Object updateShape(Object thisBlock, Object[] args) {
        Object level = args[updateShape$level];
        Object blockPos = args[updateShape$blockPos];
        Object blockState = args[0];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) {
            return blockState;
        }
        ImmutableBlockState customState = optionalCustomState.get();
        DoubleBlockHalf half = customState.get(this.halfProperty);
        Object direction = VersionHelper.isOrAbove1_21_2 ? args[4] : args[1];
        if (DirectionProxy.INSTANCE.getAxis(direction) == AxisProxy.Y && half == DoubleBlockHalf.LOWER == (direction == DirectionProxy.UP)) {
            Optional<ImmutableBlockState> optionalNeighborState = BlockStateUtils.getOptionalCustomBlockState(args[updateShape$neighborState]);
            if (optionalNeighborState.isEmpty()) {
                return BlocksProxy.AIR$defaultState;
            }
            ImmutableBlockState neighborState = optionalNeighborState.get();
            DoorBlockBehavior anotherDoorBehavior = neighborState.behavior().getFirst(DoorBlockBehavior.class);
            if (anotherDoorBehavior == null) {
                return BlocksProxy.AIR$defaultState;
            }
            if (neighborState.get(anotherDoorBehavior.halfProperty) != half) {
                return neighborState.with(anotherDoorBehavior.halfProperty, half).customBlockState().minecraftState();
            }
            return BlocksProxy.AIR$defaultState;
        } else {
            if (half == DoubleBlockHalf.LOWER && direction == DirectionProxy.DOWN
                    && !canSurvive(thisBlock, blockState, level, blockPos)) {
                MultiHighBlockBehavior.playBreakEffect(customState, blockPos, level);
                return BlocksProxy.AIR$defaultState;
            }
            return blockState;
        }
    }

    @Override
    public Object playerWillDestroy(Object thisBlock, Object[] args) {
        Object level = args[0];
        Object pos = args[1];
        Object state = args[2];
        Object player = args[3];
        ImmutableBlockState blockState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(state));
        if (blockState == null || blockState.isEmpty()) {
            return state;
        }
        org.bukkit.entity.Player bukkitPlayer = ServerPlayerProxy.INSTANCE.getBukkitEntity(player);
        BukkitServerPlayer cePlayer = BukkitAdaptor.adapt(bukkitPlayer);
        if (cePlayer == null) {
            return state;
        }
        Item item = cePlayer.getItemInHand(InteractionHand.MAIN_HAND);
        if (cePlayer.canInstabuild() || !BlockStateUtils.isCorrectTool(blockState, item)) {
            preventDropFromBottomPart(level, pos, blockState, player);
        }
        return state;
    }

    private void preventDropFromBottomPart(Object level, Object pos, ImmutableBlockState state, Object player) {
        DoubleBlockHalf half = state.get(this.halfProperty);
        if (half == DoubleBlockHalf.UPPER) {
            Object blockPos = BlockPosProxy.INSTANCE.relative(pos, DirectionProxy.DOWN);
            Object blockState = BlockGetterProxy.INSTANCE.getBlockState(level, blockPos);
            ImmutableBlockState belowState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(blockState));
            if (belowState == null || belowState.isEmpty()) return;
            DoorBlockBehavior belowDoorBehavior = belowState.behavior().getFirst(DoorBlockBehavior.class);
            if (belowDoorBehavior == null || belowState.get(belowDoorBehavior.halfProperty) != DoubleBlockHalf.LOWER) return;
            LevelWriterProxy.INSTANCE.setBlock(level, blockPos, BlocksProxy.AIR$defaultState, UPDATE_NEIGHBORS | UPDATE_CLIENTS | UPDATE_SUPPRESS_DROPS);
            LevelUtils.levelEvent(level, player, WorldEvents.BLOCK_BREAK_EFFECT, blockPos, belowState.customBlockState().registryId());
        }
    }

    @Override
    public void preExplosionHit(Object thisBlock, Object[] args) {
        if (this.canOpenByWindCharge && ExplosionProxy.INSTANCE.canTriggerBlocks(args[3])) {
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
    public void placeMultiState(Object thisBlock, Object[] args) {
        Object blockState = args[2];
        Object pos = args[1];
        Optional<ImmutableBlockState> immutableBlockState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        immutableBlockState.ifPresent(state -> LevelWriterProxy.INSTANCE.setBlock(args[0], LocationUtils.above(pos), state.with(this.halfProperty, DoubleBlockHalf.UPPER).customBlockState().minecraftState(), UpdateFlags.UPDATE_ALL));
    }

    @Override
    public boolean hasMultiState(ImmutableBlockState baseState) {
        return baseState.get(this.halfProperty) == DoubleBlockHalf.LOWER;
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        World world  = context.getLevel();
        Object level = world.minecraftWorld();
        BlockPos pos = context.getClickedPos();
        if (pos.y() < world.worldHeight().getMaxBuildHeight() - 1 && world.getBlock(pos.above()).canBeReplaced(context)) {
            boolean hasSignal = SignalGetterProxy.INSTANCE.hasNeighborSignal(level, LocationUtils.toBlockPos(pos)) || SignalGetterProxy.INSTANCE.hasNeighborSignal(level, LocationUtils.toBlockPos(pos.above()));
            return state.with(this.poweredProperty, hasSignal)
                    .with(this.facingProperty, context.getHorizontalDirection())
                    .with(this.openProperty, hasSignal)
                    .with(this.halfProperty, DoubleBlockHalf.LOWER)
                    .with(this.hingeProperty, getHinge(context));
        }
        return null;
    }

    private DoorHinge getHinge(BlockPlaceContext context) {
        Object serverLevel = context.getLevel().minecraftWorld();
        BlockPos clickedPos = context.getClickedPos();
        Direction horizontalDirection = context.getHorizontalDirection();
        BlockPos blockPos = clickedPos.above();

        Direction counterClockWise = horizontalDirection.counterClockWise();
        Object blockPos1 = LocationUtils.toBlockPos(clickedPos.relative(counterClockWise));
        Object blockState1 = BlockGetterProxy.INSTANCE.getBlockState(serverLevel, blockPos1);
        Object blockPos2 = LocationUtils.toBlockPos(blockPos.relative(counterClockWise));
        Object blockState2 = BlockGetterProxy.INSTANCE.getBlockState(serverLevel, blockPos2);

        Direction clockWise = horizontalDirection.clockWise();
        Object blockPos3 = LocationUtils.toBlockPos(clickedPos.relative(clockWise));
        Object blockState3 = BlockGetterProxy.INSTANCE.getBlockState(serverLevel, blockPos3);
        Object blockPos4 = LocationUtils.toBlockPos(blockPos.relative(clockWise));
        Object blockState4 = BlockGetterProxy.INSTANCE.getBlockState(serverLevel, blockPos4);

        int i = (BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isCollisionShapeFullBlock(blockState1, serverLevel, blockPos1) ? -1 : 0) +
                (BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isCollisionShapeFullBlock(blockState2, serverLevel, blockPos2) ? -1 : 0) +
                (BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isCollisionShapeFullBlock(blockState3, serverLevel, blockPos3) ? 1 : 0) +
                (BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isCollisionShapeFullBlock(blockState4, serverLevel, blockPos4) ? 1 : 0);

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
            DoorBlockBehavior doorBlockBehavior = optionalCustomState.get().behavior().getFirst(DoorBlockBehavior.class);
            return doorBlockBehavior != null && optionalCustomState.get().get(doorBlockBehavior.halfProperty) == DoubleBlockHalf.LOWER;
        }
    }

    public void setOpen(@Nullable Player player, Object serverLevel, ImmutableBlockState state, BlockPos pos, boolean isOpen) {
        if (isOpen(state) != isOpen) {
            org.bukkit.World world = LevelProxy.INSTANCE.getWorld(serverLevel);
            LevelWriterProxy.INSTANCE.setBlock(serverLevel, LocationUtils.toBlockPos(pos), state.with(this.openProperty, isOpen).customBlockState().minecraftState(), UPDATE_CLIENTS | UPDATE_IMMEDIATE);
            world.sendGameEvent(player == null ? null : (org.bukkit.entity.Player) player.platformPlayer(), isOpen ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, new Vector(pos.x(), pos.y(), pos.z()));
            SoundData soundData = isOpen ? this.openSound : this.closeSound;
            if (soundData != null) {
                BukkitAdaptor.adapt(world).playBlockSound(
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
        setOpen(player, world.minecraftWorld(), state, pos, !state.get(this.openProperty));
        return InteractionResult.SUCCESS_AND_CANCEL;
    }

    @Override
    public boolean isPathFindable(Object thisBlock, Object[] args) {
        Object type = VersionHelper.isOrAbove1_20_5 ? args[1] : args[3];
        Object blockState = args[0];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return false;
        if (type == PathComputationTypeProxy.LAND || type == PathComputationTypeProxy.AIR) {
            return optionalCustomState.get().get(this.openProperty);
        }
        return false;
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void neighborChanged(Object thisBlock, Object[] args) {
        Object blockPos = args[2];
        Object level = args[1];
        Object blockState = args[0];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return;
        ImmutableBlockState customState = optionalCustomState.get();
        Object anotherHalfPos = customState.get(this.halfProperty) == DoubleBlockHalf.LOWER ? LocationUtils.above(blockPos) : LocationUtils.below(blockPos);
        Block bukkitBlock = CraftBlockProxy.INSTANCE.at(level, blockPos);
        Block anotherBukkitBlock = CraftBlockProxy.INSTANCE.at(level, anotherHalfPos);
        int power = Math.max(bukkitBlock.getBlockPower(), anotherBukkitBlock.getBlockPower());
        int oldPower = customState.get(this.poweredProperty) ? 15 : 0;
        if (oldPower == 0 ^ power == 0) {
            BlockRedstoneEvent event = new BlockRedstoneEvent(bukkitBlock, oldPower, power);
            Bukkit.getPluginManager().callEvent(event);
            boolean flag = event.getNewCurrent() > 0;
            if (flag != customState.get(this.openProperty)) {
                org.bukkit.World world = LevelProxy.INSTANCE.getWorld(level);
                world.sendGameEvent(null, flag ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, new Vector(bukkitBlock.getX(), bukkitBlock.getY(), bukkitBlock.getZ()));
                SoundData soundData = flag ? this.openSound : this.closeSound;
                if (soundData != null) {
                    BukkitAdaptor.adapt(world).playBlockSound(
                            new Vec3d(Vec3iProxy.INSTANCE.getX(blockPos) + 0.5, Vec3iProxy.INSTANCE.getY(blockPos) + 0.5, Vec3iProxy.INSTANCE.getZ(blockPos) + 0.5),
                            soundData
                    );
                }
            }
            LevelWriterProxy.INSTANCE.setBlock(level, blockPos, customState.with(this.poweredProperty, flag).with(this.openProperty, flag).customBlockState().minecraftState(), UpdateFlags.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean canSurvive(Object thisBlock, Object state, Object level, Object blockPos) {
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(state);
        if (optionalCustomState.isEmpty()) return false;
        int x = Vec3iProxy.INSTANCE.getX(blockPos);
        int y = Vec3iProxy.INSTANCE.getY(blockPos) - 1;
        int z = Vec3iProxy.INSTANCE.getZ(blockPos);
        Object belowPos = BlockPosProxy.INSTANCE.newInstance(x, y, z);
        Object belowState = BlockGetterProxy.INSTANCE.getBlockState(level, belowPos);
        if (optionalCustomState.get().get(this.halfProperty) == DoubleBlockHalf.UPPER) {
            Optional<ImmutableBlockState> belowCustomState = BlockStateUtils.getOptionalCustomBlockState(belowState);
            return belowCustomState.filter(immutableBlockState -> immutableBlockState.owner().value() == super.blockDefinition).isPresent();
        } else {
            return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isFaceSturdy(
                    belowState, level, belowPos, DirectionProxy.UP,
                    SupportTypeProxy.FULL
            );
        }
    }

    private static class Factory implements BlockBehaviorFactory<DoorBlockBehavior> {
        private static final String[] CAN_OPEN_WITH_HAND = new String[] {"can_open_with_hand", "can-open-with-hand"};
        private static final String[] CAN_OPEN_BY_WIND_CHARGE = new String[] {"can_open_by_wind_charge", "can-open-by-wind-charge"};

        @Override
        public DoorBlockBehavior create(BlockDefinition block, ConfigSection section) {
            ConfigSection soundSection = section.getSection("sounds");
            SoundData openSound = null;
            SoundData closeSound = null;
            if (soundSection != null) {
                openSound = soundSection.getValue("open", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.RANGED_0_9_1));
                closeSound = soundSection.getValue("close", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.RANGED_0_9_1));
            }
            return new DoorBlockBehavior(
                    block,
                    BlockBehaviorFactory.getProperty(section.path(), block, "half", DoubleBlockHalf.class),
                    BlockBehaviorFactory.getProperty(section.path(), block, "facing", Direction.class),
                    BlockBehaviorFactory.getProperty(section.path(), block, "hinge", DoorHinge.class),
                    BlockBehaviorFactory.getProperty(section.path(), block, "powered", Boolean.class),
                    BlockBehaviorFactory.getProperty(section.path(), block, "open", Boolean.class),
                    section.getBoolean(CAN_OPEN_WITH_HAND, true),
                    section.getBoolean(CAN_OPEN_BY_WIND_CHARGE, true),
                    openSound,
                    closeSound
            );
        }
    }
}
