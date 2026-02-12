package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MTagKeys;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.InteractUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.IsPathFindableBlockBehavior;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.HorizontalDirection;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import org.bukkit.Bukkit;
import org.bukkit.GameEvent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

@SuppressWarnings("DuplicatedCode")
public class FenceGateBlockBehavior extends BukkitBlockBehavior implements IsPathFindableBlockBehavior {
    public static final BlockBehaviorFactory<FenceGateBlockBehavior> FACTORY = new Factory();
    private final Property<HorizontalDirection> facingProperty;
    private final Property<Boolean> inWallProperty;
    private final Property<Boolean> openProperty;
    private final Property<Boolean> poweredProperty;
    private final boolean canOpenWithHand;
    private final boolean canOpenByWindCharge;
    private final SoundData openSound;
    private final SoundData closeSound;

    public FenceGateBlockBehavior(
            CustomBlock customBlock,
            Property<HorizontalDirection> facing,
            Property<Boolean> inWall,
            Property<Boolean> open,
            Property<Boolean> powered,
            boolean canOpenWithHand,
            boolean canOpenByWindCharge,
            SoundData openSound,
            SoundData closeSound
    ) {
        super(customBlock);
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
        return FastNMS.INSTANCE.method$BlockStateBase$is(state, MTagKeys.Block$WALLS);
    }

    private Object getBlockState(Object level, BlockPos blockPos) {
        return FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, LocationUtils.toBlockPos(blockPos));
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object blockState = args[0];
        Direction direction = DirectionUtils.fromNMSDirection(args[updateShape$direction]);
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return blockState;
        ImmutableBlockState customState = optionalCustomState.get();
        if (customState.get(this.facingProperty).toDirection().clockWise().axis() != direction.axis()) {
            return superMethod.call();
        }
        Object neighborState = args[updateShape$neighborState];
        Object level = args[updateShape$level];
        BlockPos blockPos = LocationUtils.fromBlockPos(VersionHelper.isOrAbove1_21_2() ? args[3] : args[4]);
        Object relativeState = getBlockState(level, blockPos.relative(direction.opposite()));
        boolean neighborStateIsWall = this.isWall(neighborState);
        boolean relativeStateIsWall = this.isWall(relativeState);
        boolean flag = neighborStateIsWall || relativeStateIsWall;
        // TODO: 连接原版方块
        if (neighborStateIsWall) {
        }
        if (relativeStateIsWall) {
        }
        return customState.with(this.inWallProperty, flag).customBlockState().literalObject();
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        Object level = context.getLevel().serverWorld();
        BlockPos clickedPos = context.getClickedPos();
        boolean hasNeighborSignal = FastNMS.INSTANCE.method$SignalGetter$hasNeighborSignal(level, LocationUtils.toBlockPos(clickedPos));
        Direction horizontalDirection = context.getHorizontalDirection();
        Direction.Axis axis = horizontalDirection.axis();
        boolean flag = axis == Direction.Axis.Z && (this.isWall(getBlockState(level, clickedPos.relative(Direction.WEST))))
                || this.isWall(getBlockState(level, clickedPos.relative(Direction.EAST)))
                || axis == Direction.Axis.X && (this.isWall(getBlockState(level, clickedPos.relative(Direction.NORTH)))
                || this.isWall(getBlockState(level, clickedPos.relative(Direction.SOUTH))));
        // TODO: 连接原版方块
        return state.owner().value().defaultState()
                .with(this.facingProperty, horizontalDirection.toHorizontalDirection())
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

    @SuppressWarnings("unchecked")
    private void playerToggle(UseOnContext context, ImmutableBlockState state) {
        Player player = context.getPlayer();
        if (player == null) return;
        BlockPos pos = context.getClickedPos();
        Location location = new Location((org.bukkit.World) player.world().platformWorld(), pos.x, pos.y, pos.z);
        if (!BukkitCraftEngine.instance().antiGriefProvider().test((org.bukkit.entity.Player) player.platformPlayer(), Flag.OPEN_DOOR, location)) {
            return;
        }
        this.toggle(state, context.getLevel(), pos, player);
        if (!InteractUtils.isInteractable((org.bukkit.entity.Player) player.platformPlayer(), BlockStateUtils.fromBlockData(state.visualBlockState().literalObject()), context.getHitResult(), (Item<ItemStack>) context.getItem())) {
            player.swingHand(context.getHand());
        }
    }

    @Override
    public boolean isPathFindable(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object type = VersionHelper.isOrAbove1_20_5() ? args[1] : args[3];
        Object blockState = args[0];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return false;
        if (type == CoreReflections.instance$PathComputationType$LAND || type == CoreReflections.instance$PathComputationType$AIR) {
            return isOpen(optionalCustomState.get());
        }
        return false;
    }

    @Override
    public void onExplosionHit(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        if (this.canOpenByWindCharge && FastNMS.INSTANCE.method$Explosion$canTriggerBlocks(args[3])) {
            Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(args[0]);
            if (optionalCustomState.isEmpty()) return;
            this.toggle(optionalCustomState.get(), BukkitAdaptors.adapt(FastNMS.INSTANCE.method$Level$getCraftWorld(args[1])), LocationUtils.fromBlockPos(args[2]), null);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void neighborChanged(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object blockState = args[0];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) return;
        Object level = args[1];
        Object blockPos = args[2];
        boolean hasSignal = FastNMS.INSTANCE.method$SignalGetter$hasNeighborSignal(level, blockPos);
        ImmutableBlockState customState = optionalCustomState.get();
        if (hasSignal == customState.get(this.poweredProperty)) return;

        Block bblock = FastNMS.INSTANCE.method$CraftBlock$at(level, blockPos);
        int power = bblock.getBlockPower();
        int oldPower = isOpen(customState) ? 15 : 0;
        Object neighborBlock = args[3];

        if (oldPower == 0 ^ power == 0 || FastNMS.INSTANCE.method$BlockStateBase$isSignalSource(FastNMS.INSTANCE.method$Block$defaultState(neighborBlock))) {
            BlockRedstoneEvent event = new BlockRedstoneEvent(bblock, oldPower, power);
            Bukkit.getPluginManager().callEvent(event);
            hasSignal = event.getNewCurrent() > 0;
        }

        World world = BukkitAdaptors.adapt(FastNMS.INSTANCE.method$Level$getCraftWorld(level));
        boolean changed = isOpen(customState) != hasSignal;
        if (hasSignal && changed) {
            Object abovePos = LocationUtils.above(blockPos);
            Object aboveBlockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, abovePos);
            if (CoreReflections.clazz$RedStoneWireBlock.isInstance(FastNMS.INSTANCE.method$BlockState$getBlock(aboveBlockState))) {
                FastNMS.INSTANCE.method$LevelWriter$setBlock(level, abovePos, MBlocks.AIR$defaultState, UpdateOption.UPDATE_ALL.flags());
                world.dropItemNaturally(
                        new Vec3d(FastNMS.INSTANCE.field$Vec3i$x(abovePos) + 0.5, FastNMS.INSTANCE.field$Vec3i$y(abovePos) + 0.5, FastNMS.INSTANCE.field$Vec3i$z(abovePos) + 0.5),
                        BukkitItemManager.instance().createWrappedItem(ItemKeys.REDSTONE, null)
                );
                if (FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, blockPos) != blockPos) {
                    return;
                }
            }
        }

        if (changed) {
            customState = customState.with(this.openProperty, hasSignal);
            FastNMS.INSTANCE.method$Level$getCraftWorld(level).sendGameEvent(null,
                    hasSignal ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE,
                    new Vector(FastNMS.INSTANCE.field$Vec3i$x(blockPos), FastNMS.INSTANCE.field$Vec3i$y(blockPos), FastNMS.INSTANCE.field$Vec3i$z(blockPos))
            );
            this.playSound(LocationUtils.fromBlockPos(blockPos), world, hasSignal);
        }

        FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, customState.with(this.poweredProperty, hasSignal).customBlockState().literalObject(), UpdateOption.Flags.UPDATE_CLIENTS);
    }

    private void toggle(ImmutableBlockState state, World world, BlockPos pos, @Nullable Player player) {
        ImmutableBlockState newState;
        if (state.get(this.openProperty)) {
            newState = state.with(this.openProperty, false);
        } else {
            ImmutableBlockState blockState = state;
            if (player != null) {
                Direction direction = player.getDirection();
                if (state.get(this.facingProperty).toDirection() == direction.opposite()) {
                    blockState = blockState.with(this.facingProperty, direction.toHorizontalDirection());
                }
            }
            newState = blockState.with(this.openProperty, true);
        }
        FastNMS.INSTANCE.method$LevelWriter$setBlock(world.serverWorld(), LocationUtils.toBlockPos(pos), newState.customBlockState().literalObject(), UpdateOption.UPDATE_ALL.flags());
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

    public static boolean connectsToDirection(BlockStateWrapper state, HorizontalDirection direction) {
        FenceGateBlockBehavior fence = BlockStateUtils.getOptionalCustomBlockState(state.literalObject())
                .map(ImmutableBlockState::behavior)
                .flatMap(behavior -> behavior.getAs(FenceGateBlockBehavior.class))
                .orElse(null);
        if (fence == null) return false;
        Direction facing = null;
        ImmutableBlockState customState = BlockStateUtils.getOptionalCustomBlockState(state.literalObject()).orElse(null);
        if (customState == null) return false;
        Property<?> facingProperty = customState.owner().value().getProperty("facing");
        if (facingProperty != null && facingProperty.valueClass() == HorizontalDirection.class) {
            facing = ((HorizontalDirection) customState.get(facingProperty)).toDirection();
        }
        return facing != null && facing.axis() == direction.toDirection().clockWise().axis();
    }

    private static class Factory implements BlockBehaviorFactory<FenceGateBlockBehavior> {

        @Override
        @SuppressWarnings("unchecked")
        public FenceGateBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Property<HorizontalDirection> facing = (Property<HorizontalDirection>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("facing"), "warning.config.block.behavior.fence_gate.missing_facing");
            Property<Boolean> inWall = (Property<Boolean>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("in_wall"), "warning.config.block.behavior.fence_gate.missing_in_wall");
            Property<Boolean> open = (Property<Boolean>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("open"), "warning.config.block.behavior.fence_gate.missing_open");
            Property<Boolean> powered = (Property<Boolean>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("powered"), "warning.config.block.behavior.fence_gate.missing_powered");
            boolean canOpenWithHand = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("can-open-with-hand", true), "can-open-with-hand");
            boolean canOpenByWindCharge = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("can-open-by-wind-charge", true), "can-open-by-wind-charge");
            Map<String, Object> sounds = (Map<String, Object>) arguments.get("sounds");
            SoundData openSound = null;
            SoundData closeSound = null;
            if (sounds != null) {
                openSound = Optional.ofNullable(sounds.get("open")).map(obj -> SoundData.create(obj, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.ranged(0.9f, 1f))).orElse(null);
                closeSound = Optional.ofNullable(sounds.get("close")).map(obj -> SoundData.create(obj, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.ranged(0.9f, 1f))).orElse(null);
            }
            return new FenceGateBlockBehavior(block, facing, inWall, open, powered, canOpenWithHand, canOpenByWindCharge, openSound, closeSound);
        }
    }
}
