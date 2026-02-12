package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.InteractUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.IsPathFindableBlockBehavior;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.block.properties.type.SingleBlockHalf;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.*;
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
public class TrapDoorBlockBehavior extends BukkitBlockBehavior implements IsPathFindableBlockBehavior {
    public static final BlockBehaviorFactory<TrapDoorBlockBehavior> FACTORY = new Factory();
    private final Property<SingleBlockHalf> halfProperty;
    private final Property<HorizontalDirection> facingProperty;
    private final Property<Boolean> poweredProperty;
    private final Property<Boolean> openProperty;
    private final boolean canOpenWithHand;
    private final boolean canOpenByWindCharge;
    private final SoundData openSound;
    private final SoundData closeSound;

    public TrapDoorBlockBehavior(CustomBlock block,
                                 Property<SingleBlockHalf> halfProperty,
                                 Property<HorizontalDirection> facingProperty,
                                 Property<Boolean> poweredProperty,
                                 Property<Boolean> openProperty,
                                 boolean canOpenWithHand,
                                 boolean canOpenByWindCharge,
                                 SoundData openSound,
                                 SoundData closeSound) {
        super(block);
        this.halfProperty = halfProperty;
        this.facingProperty = facingProperty;
        this.poweredProperty = poweredProperty;
        this.openProperty = openProperty;
        this.canOpenWithHand = canOpenWithHand;
        this.canOpenByWindCharge = canOpenByWindCharge;
        this.openSound = openSound;
        this.closeSound = closeSound;
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object blockState = args[0];
        if (super.waterloggedProperty != null) {
            BlockStateUtils.getOptionalCustomBlockState(blockState).ifPresent(customState -> {
                if (customState.get(super.waterloggedProperty)) {
                    FastNMS.INSTANCE.method$ScheduledTickAccess$scheduleFluidTick(args[updateShape$level], args[updateShape$blockPos], MFluids.WATER, 5);
                }
            });
        }
        return blockState;
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        Object level = context.getLevel().serverWorld();
        Object clickedPos = LocationUtils.toBlockPos(context.getClickedPos());
        Direction clickedFace = context.getClickedFace();
        if (!context.replacingClickedBlock() && clickedFace.axis().isHorizontal()) {
            state = state.with(this.facingProperty, clickedFace.toHorizontalDirection())
                    .with(this.halfProperty, context.getClickedLocation().y - context.getClickedPos().y() > 0.5 ? SingleBlockHalf.TOP : SingleBlockHalf.BOTTOM);
        } else {
            state = state.with(this.facingProperty, context.getHorizontalDirection().opposite().toHorizontalDirection())
                    .with(this.halfProperty, clickedFace == Direction.UP ? SingleBlockHalf.BOTTOM : SingleBlockHalf.TOP);
        }
        if (FastNMS.INSTANCE.method$SignalGetter$hasNeighborSignal(level, clickedPos)) {
            state = state.with(this.poweredProperty, true).with(this.openProperty, true);
        }
        if (this.waterloggedProperty != null && FastNMS.INSTANCE.method$FluidState$getType(FastNMS.INSTANCE.method$BlockGetter$getFluidState(level, clickedPos)) == MFluids.WATER) {
            state = state.with(this.waterloggedProperty, true);
        }
        return state;
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
        World world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Location location = new Location((org.bukkit.World) world.platformWorld(), pos.x, pos.y, pos.z);
        if (!BukkitCraftEngine.instance().antiGriefProvider().test((org.bukkit.entity.Player) player.platformPlayer(), Flag.INTERACT, location)) {
            return;
        }
        this.toggle(state, world, pos, player);
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
            return optionalCustomState.get().get(this.openProperty);
        } else if (type == CoreReflections.instance$PathComputationType$WATER) {
            return optionalCustomState.get().get(super.waterloggedProperty);
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
        ImmutableBlockState customState = optionalCustomState.get();
        Object level = args[1];
        Object blockPos = args[2];
        boolean hasSignal = FastNMS.INSTANCE.method$SignalGetter$hasNeighborSignal(level, blockPos);
        if (hasSignal == customState.get(this.poweredProperty)) return;

        Block bblock = FastNMS.INSTANCE.method$CraftBlock$at(level, blockPos);
        int power = bblock.getBlockPower();
        int oldPower = customState.get(this.openProperty) ? 15 : 0;
        Object neighborBlock = args[3];

        if (oldPower == 0 ^ power == 0 || FastNMS.INSTANCE.method$BlockStateBase$isSignalSource(FastNMS.INSTANCE.method$Block$defaultState(neighborBlock))) {
            BlockRedstoneEvent event = new BlockRedstoneEvent(bblock, oldPower, power);
            Bukkit.getPluginManager().callEvent(event);
            hasSignal = event.getNewCurrent() > 0;
        }

        World world = BukkitAdaptors.adapt(FastNMS.INSTANCE.method$Level$getCraftWorld(level));
        boolean changed = customState.get(this.openProperty) != hasSignal;
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
        if (this.waterloggedProperty != null && customState.get(this.waterloggedProperty)) {
            FastNMS.INSTANCE.method$ScheduledTickAccess$scheduleFluidTick(level, blockPos, MFluids.WATER, 5);
        }
    }

    private void toggle(ImmutableBlockState state, World world, BlockPos pos, @Nullable Player player) {
        ImmutableBlockState newState = state.cycle(this.openProperty);
        FastNMS.INSTANCE.method$LevelWriter$setBlock(world.serverWorld(), LocationUtils.toBlockPos(pos), newState.customBlockState().literalObject(), UpdateOption.UPDATE_ALL.flags());
        boolean open = newState.get(this.openProperty);
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

    @SuppressWarnings("unchecked")
    private static class Factory implements BlockBehaviorFactory<TrapDoorBlockBehavior> {

        @Override
        public TrapDoorBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Property<SingleBlockHalf> half = (Property<SingleBlockHalf>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("half"), "warning.config.block.behavior.trapdoor.missing_half");
            Property<HorizontalDirection> facing = (Property<HorizontalDirection>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("facing"), "warning.config.block.behavior.trapdoor.missing_facing");
            Property<Boolean> open = (Property<Boolean>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("open"), "warning.config.block.behavior.trapdoor.missing_open");
            Property<Boolean> powered = (Property<Boolean>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("powered"), "warning.config.block.behavior.trapdoor.missing_powered");
            boolean canOpenWithHand = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("can-open-with-hand", true), "can-open-with-hand");
            boolean canOpenByWindCharge = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("can-open-by-wind-charge", true), "can-open-by-wind-charge");
            Map<String, Object> sounds = MiscUtils.castToMap(arguments.get("sounds"), true);
            SoundData openSound = null;
            SoundData closeSound = null;
            if (sounds != null) {
                openSound = Optional.ofNullable(sounds.get("open")).map(obj -> SoundData.create(obj, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.ranged(0.9f, 1f))).orElse(null);
                closeSound = Optional.ofNullable(sounds.get("close")).map(obj -> SoundData.create(obj, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.ranged(0.9f, 1f))).orElse(null);
            }
            return new TrapDoorBlockBehavior(block, half, facing, powered, open, canOpenWithHand, canOpenByWindCharge, openSound, closeSound);
        }
    }
}
