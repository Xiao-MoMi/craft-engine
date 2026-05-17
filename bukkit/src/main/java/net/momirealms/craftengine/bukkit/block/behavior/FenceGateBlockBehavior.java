package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.InteractUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.PathFindingBlock;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.block.CraftBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.core.Vec3iProxy;
import net.momirealms.craftengine.proxy.minecraft.tags.BlockTagsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.*;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.RedstoneWireBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.pathfinder.PathComputationTypeProxy;
import org.bukkit.Bukkit;
import org.bukkit.GameEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

@SuppressWarnings("DuplicatedCode")
public final class FenceGateBlockBehavior extends BukkitBlockBehavior implements PathFindingBlock {
    public static final BlockBehaviorFactory<FenceGateBlockBehavior> FACTORY = new Factory();
    public final Property<Direction> facingProperty;
    public final Property<Boolean> inWallProperty;
    public final Property<Boolean> openProperty;
    public final Property<Boolean> poweredProperty;
    public final boolean canOpenWithHand;
    public final boolean canOpenByWindCharge;
    public final SoundData openSound;
    public final SoundData closeSound;

    private FenceGateBlockBehavior(BlockDefinition blockDefinition,
                                   Property<Direction> facing,
                                   Property<Boolean> inWall,
                                   Property<Boolean> open,
                                   Property<Boolean> powered,
                                   boolean canOpenWithHand,
                                   boolean canOpenByWindCharge,
                                   SoundData openSound,
                                   SoundData closeSound) {
        super(blockDefinition);
        this.facingProperty = facing;
        this.inWallProperty = inWall;
        this.openProperty = open;
        this.poweredProperty = powered;
        this.canOpenWithHand = canOpenWithHand;
        this.canOpenByWindCharge = canOpenByWindCharge;
        this.openSound = openSound;
        this.closeSound = closeSound;
    }

    public boolean isOpen(ImmutableBlockState state) {
        if (state == null || state.isEmpty() || !state.contains(this.openProperty)) return false;
        return state.get(this.openProperty);
    }

    public boolean isWall(Object state) {
        if (state == null) return false;
        return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$1(state, BlockTagsProxy.WALLS);
    }

    private Object getBlockState(Object level, BlockPos blockPos) {
        return BlockGetterProxy.INSTANCE.getBlockState(level, LocationUtils.toBlockPos(blockPos));
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args) {
        Object blockState = args[0];
        Direction direction = DirectionUtils.fromNMSDirection(args[updateShape$direction]);
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return blockState;
        ImmutableBlockState customState = optionalCustomState.get();
        if (customState.get(this.facingProperty).clockWise().axis() != direction.axis()) {
            return super.updateShape(thisBlock, args);
        }
        Object neighborState = args[updateShape$neighborState];
        Object level = args[updateShape$level];
        BlockPos blockPos = LocationUtils.fromBlockPos(VersionHelper.isOrAbove1_21_2 ? args[3] : args[4]);
        Object relativeState = getBlockState(level, blockPos.relative(direction.opposite()));
        boolean neighborStateIsWall = this.isWall(neighborState);
        boolean relativeStateIsWall = this.isWall(relativeState);
        boolean flag = neighborStateIsWall || relativeStateIsWall;
        // TODO: 连接原版方块
        if (neighborStateIsWall) {
        }
        if (relativeStateIsWall) {
        }
        return customState.with(this.inWallProperty, flag).customBlockState().minecraftState();
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        Object level = context.getLevel().minecraftWorld();
        BlockPos clickedPos = context.getClickedPos();
        boolean hasNeighborSignal = SignalGetterProxy.INSTANCE.hasNeighborSignal(level, LocationUtils.toBlockPos(clickedPos));
        Direction horizontalDirection = context.getHorizontalDirection();
        Direction.Axis axis = horizontalDirection.axis();
        boolean flag = axis == Direction.Axis.Z && (this.isWall(getBlockState(level, clickedPos.relative(Direction.WEST))))
                || this.isWall(getBlockState(level, clickedPos.relative(Direction.EAST)))
                || axis == Direction.Axis.X && (this.isWall(getBlockState(level, clickedPos.relative(Direction.NORTH)))
                || this.isWall(getBlockState(level, clickedPos.relative(Direction.SOUTH))));
        // TODO: 连接原版方块
        return state.owner().value().defaultState()
                .with(this.facingProperty, horizontalDirection)
                .with(this.openProperty, hasNeighborSignal)
                .with(this.poweredProperty, hasNeighborSignal)
                .with(this.inWallProperty, flag);
    }

    @Override
    public InteractionResult useWithoutItem(UseOnContext context, ImmutableBlockState state) {
        if (!this.canOpenWithHand) {
            return InteractionResult.PASS;
        }
        playerToggle(context, state);
        return InteractionResult.SUCCESS_AND_CANCEL;
    }

    private void playerToggle(UseOnContext context, ImmutableBlockState state) {
        Player player = context.getPlayer();
        if (player == null) return;
        BlockPos pos = context.getClickedPos();
        Location location = new Location((org.bukkit.World) player.world().platformWorld(), pos.x, pos.y, pos.z);
        if (!BukkitCraftEngine.instance().antiGriefProvider().test((org.bukkit.entity.Player) player.platformPlayer(), Flag.OPEN_DOOR, location)) {
            return;
        }
        this.toggle(state, context.getLevel(), pos, player);
        if (!InteractUtils.isInteractable((org.bukkit.entity.Player) player.platformPlayer(), BlockStateUtils.fromBlockData(state.visualBlockState().minecraftState()), context.getHitResult(), (Item) context.getItem())) {
            player.swingHand(context.getHand());
        }
    }

    @Override
    public boolean isPathFindable(Object thisBlock, Object[] args) {
        Object type = VersionHelper.isOrAbove1_20_5 ? args[1] : args[3];
        Object blockState = args[0];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return false;
        if (type == PathComputationTypeProxy.LAND || type == PathComputationTypeProxy.AIR) {
            return isOpen(optionalCustomState.get());
        }
        return false;
    }

    @Override
    public void preExplosionHit(Object thisBlock, Object[] args) {
        if (this.canOpenByWindCharge && ExplosionProxy.INSTANCE.canTriggerBlocks(args[3])) {
            Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(args[0]);
            if (optionalCustomState.isEmpty()) return;
            this.toggle(optionalCustomState.get(), BukkitAdaptor.adapt(LevelProxy.INSTANCE.getWorld(args[1])), LocationUtils.fromBlockPos(args[2]), null);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void neighborChanged(Object thisBlock, Object[] args) {
        Object blockState = args[0];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return;
        Object level = args[1];
        Object blockPos = args[2];
        boolean hasSignal = SignalGetterProxy.INSTANCE.hasNeighborSignal(level, blockPos);
        ImmutableBlockState customState = optionalCustomState.get();
        if (hasSignal == customState.get(this.poweredProperty)) return;

        Block bblock = CraftBlockProxy.INSTANCE.at(level, blockPos);
        int power = bblock.getBlockPower();
        int oldPower = isOpen(customState) ? 15 : 0;
        Object neighborBlock = args[3];

        if (oldPower == 0 ^ power == 0 || BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isSignalSource(BlockProxy.INSTANCE.getDefaultBlockState(neighborBlock))) {
            BlockRedstoneEvent event = new BlockRedstoneEvent(bblock, oldPower, power);
            Bukkit.getPluginManager().callEvent(event);
            hasSignal = event.getNewCurrent() > 0;
        }

        World world = BukkitAdaptor.adapt(LevelProxy.INSTANCE.getWorld(level));
        boolean changed = isOpen(customState) != hasSignal;
        if (hasSignal && changed) {
            Object abovePos = LocationUtils.above(blockPos);
            Object aboveBlockState = BlockGetterProxy.INSTANCE.getBlockState(level, abovePos);
            if (RedstoneWireBlockProxy.CLASS.isInstance(BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getBlock(aboveBlockState))) {
                LevelWriterProxy.INSTANCE.setBlock(level, abovePos, BlocksProxy.AIR$defaultState, UpdateFlags.UPDATE_ALL);
                world.dropItemNaturally(
                        new Vec3d(Vec3iProxy.INSTANCE.getX(abovePos) + 0.5, Vec3iProxy.INSTANCE.getY(abovePos) + 0.5, Vec3iProxy.INSTANCE.getZ(abovePos) + 0.5),
                        BukkitItemManager.instance().createWrappedItem(ItemKeys.REDSTONE, null)
                );
                if (BlockGetterProxy.INSTANCE.getBlockState(level, blockPos) != blockPos) {
                    return;
                }
            }
        }

        if (changed) {
            customState = customState.with(this.openProperty, hasSignal);
            LevelProxy.INSTANCE.getWorld(level).sendGameEvent(null,
                    hasSignal ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE,
                    new Vector(Vec3iProxy.INSTANCE.getX(blockPos), Vec3iProxy.INSTANCE.getY(blockPos), Vec3iProxy.INSTANCE.getZ(blockPos))
            );
            this.playSound(LocationUtils.fromBlockPos(blockPos), world, hasSignal);
        }

        LevelWriterProxy.INSTANCE.setBlock(level, blockPos, customState.with(this.poweredProperty, hasSignal).customBlockState().minecraftState(), UpdateFlags.UPDATE_CLIENTS);
    }

    private void toggle(ImmutableBlockState state, World world, BlockPos pos, @Nullable Player player) {
        ImmutableBlockState newState;
        if (state.get(this.openProperty)) {
            newState = state.with(this.openProperty, false);
        } else {
            ImmutableBlockState blockState = state;
            if (player != null) {
                Direction direction = player.getDirection();
                if (state.get(this.facingProperty) == direction.opposite()) {
                    blockState = blockState.with(this.facingProperty, direction);
                }
            }
            newState = blockState.with(this.openProperty, true);
        }
        LevelWriterProxy.INSTANCE.setBlock(world.minecraftWorld(), LocationUtils.toBlockPos(pos), newState.customBlockState().minecraftState(), UpdateFlags.UPDATE_ALL);
        boolean open = isOpen(newState);
        ((org.bukkit.World) world.platformWorld()).sendGameEvent(
                player != null ? (org.bukkit.entity.Player) player.platformPlayer() : null,
                open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE,
                new Vector(pos.x(), pos.y(), pos.z())
        );
        this.playSound(pos, world, open);
    }

    private void playSound(BlockPos pos, World world, boolean open) {
        if (open) {
            if (this.openSound != null) {
                world.playBlockSound(new Vec3d(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5), this.openSound);
            }
        } else {
            if (this.closeSound != null) {
                world.playBlockSound(new Vec3d(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5), this.closeSound);
            }
        }
    }

    public static boolean connectsToDirection(BlockStateWrapper state, Direction direction) {
        Optional<ImmutableBlockState> optionalCustomBlockState = BlockStateUtils.getOptionalCustomBlockState(state.minecraftState());
        if (optionalCustomBlockState.isEmpty()) return false;
        ImmutableBlockState customState = optionalCustomBlockState.get();
        FenceGateBlockBehavior fence = customState.behavior().getFirst(FenceGateBlockBehavior.class);
        if (fence == null) return false;
        Direction facing = customState.get(fence.facingProperty);
        return facing.axis() == direction.clockWise().axis();
    }

    private static class Factory implements BlockBehaviorFactory<FenceGateBlockBehavior> {
        private static final String[] CAN_OPEN_WITH_HAND = new String[] {"can_open_with_hand", "can-open-with-hand"};
        private static final String[] CAN_OPEN_BY_WIND_CHARGE = new String[] {"can_open_by_wind_charge", "can-open-by-wind-charge"};

        @Override
        public FenceGateBlockBehavior create(BlockDefinition block, ConfigSection section) {
            ConfigSection soundSection = section.getSection("sounds");
            SoundData openSound = null;
            SoundData closeSound = null;
            if (soundSection != null) {
                openSound = soundSection.getValue("open", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.RANGED_0_9_1));
                closeSound = soundSection.getValue("close", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.RANGED_0_9_1));
            }
            return new FenceGateBlockBehavior(
                    block,
                    BlockBehaviorFactory.getProperty(section.path(), block, "facing", Direction.class),
                    BlockBehaviorFactory.getProperty(section.path(), block, "in_wall", Boolean.class),
                    BlockBehaviorFactory.getProperty(section.path(), block, "open", Boolean.class),
                    BlockBehaviorFactory.getProperty(section.path(), block, "powered", Boolean.class),
                    section.getBoolean(CAN_OPEN_WITH_HAND, true),
                    section.getBoolean(CAN_OPEN_BY_WIND_CHARGE, true),
                    openSound,
                    closeSound
            );
        }
    }
}
