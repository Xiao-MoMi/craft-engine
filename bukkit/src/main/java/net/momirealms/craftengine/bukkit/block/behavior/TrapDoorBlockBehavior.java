package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.InteractUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.PathFindingBlock;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.block.property.type.SingleBlockHalf;
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
import net.momirealms.craftengine.proxy.minecraft.world.level.*;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.RedstoneWireBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidsProxy;
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
public final class TrapDoorBlockBehavior extends BukkitBlockBehavior implements PathFindingBlock {
    public static final BlockBehaviorFactory<TrapDoorBlockBehavior> FACTORY = new Factory();
    public final Property<SingleBlockHalf> halfProperty;
    public final Property<Direction> facingProperty;
    public final Property<Boolean> poweredProperty;
    public final Property<Boolean> openProperty;
    public final Property<Boolean> waterloggedProperty;
    public final boolean canOpenWithHand;
    public final boolean canOpenByWindCharge;
    public final SoundData openSound;
    public final SoundData closeSound;

    private TrapDoorBlockBehavior(BlockDefinition block,
                                  Property<SingleBlockHalf> halfProperty,
                                  Property<Direction> facingProperty,
                                  Property<Boolean> poweredProperty,
                                  Property<Boolean> openProperty,
                                  Property<Boolean> waterloggedProperty,
                                  boolean canOpenWithHand,
                                  boolean canOpenByWindCharge,
                                  SoundData openSound,
                                  SoundData closeSound) {
        super(block);
        this.halfProperty = halfProperty;
        this.facingProperty = facingProperty;
        this.poweredProperty = poweredProperty;
        this.openProperty = openProperty;
        this.waterloggedProperty = waterloggedProperty;
        this.canOpenWithHand = canOpenWithHand;
        this.canOpenByWindCharge = canOpenByWindCharge;
        this.openSound = openSound;
        this.closeSound = closeSound;
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args) {
        Object blockState = args[0];
        if (this.waterloggedProperty != null) {
            BlockStateUtils.getOptionalCustomBlockState(blockState).ifPresent(customState -> {
                if (customState.get(this.waterloggedProperty)) {
                    LevelAccessorProxy.INSTANCE.scheduleTick$1(args[updateShape$level], args[updateShape$blockPos], FluidsProxy.WATER, 5);
                }
            });
        }
        return blockState;
    }

    @Override
    public ImmutableBlockState updateStateForPlacement(BlockPlaceContext context, ImmutableBlockState state) {
        Object level = context.getLevel().minecraftWorld();
        Object clickedPos = LocationUtils.toBlockPos(context.getClickedPos());
        Direction clickedFace = context.getClickedFace();
        if (!context.replacingClickedBlock() && clickedFace.axis().isHorizontal()) {
            state = state.with(this.facingProperty, clickedFace)
                    .with(this.halfProperty, context.getClickedLocation().y - context.getClickedPos().y() > 0.5 ? SingleBlockHalf.TOP : SingleBlockHalf.BOTTOM);
        } else {
            state = state.with(this.facingProperty, context.getHorizontalDirection().opposite())
                    .with(this.halfProperty, clickedFace == Direction.UP ? SingleBlockHalf.BOTTOM : SingleBlockHalf.TOP);
        }
        if (SignalGetterProxy.INSTANCE.hasNeighborSignal(level, clickedPos)) {
            state = state.with(this.poweredProperty, true).with(this.openProperty, true);
        }
        if (this.waterloggedProperty != null && FluidStateProxy.INSTANCE.getType(BlockGetterProxy.INSTANCE.getFluidState(level, clickedPos)) == FluidsProxy.WATER) {
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
            return optionalCustomState.get().get(this.openProperty);
        } else if (type == PathComputationTypeProxy.WATER) {
            return optionalCustomState.get().get(this.waterloggedProperty);
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
        ImmutableBlockState customState = optionalCustomState.get();
        Object level = args[1];
        Object blockPos = args[2];
        boolean hasSignal = SignalGetterProxy.INSTANCE.hasNeighborSignal(level, blockPos);
        if (hasSignal == customState.get(this.poweredProperty)) return;

        Block bblock = CraftBlockProxy.INSTANCE.at(level, blockPos);
        int power = bblock.getBlockPower();
        int oldPower = customState.get(this.openProperty) ? 15 : 0;
        Object neighborBlock = args[3];

        if (oldPower == 0 ^ power == 0 || BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isSignalSource(BlockProxy.INSTANCE.getDefaultBlockState(neighborBlock))) {
            BlockRedstoneEvent event = new BlockRedstoneEvent(bblock, oldPower, power);
            Bukkit.getPluginManager().callEvent(event);
            hasSignal = event.getNewCurrent() > 0;
        }

        World world = BukkitAdaptor.adapt(LevelProxy.INSTANCE.getWorld(level));
        boolean changed = customState.get(this.openProperty) != hasSignal;
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
        if (this.waterloggedProperty != null && customState.get(this.waterloggedProperty)) {
            LevelAccessorProxy.INSTANCE.scheduleTick$1(level, blockPos, FluidsProxy.WATER, 5);
        }
    }

    private void toggle(ImmutableBlockState state, World world, BlockPos pos, @Nullable Player player) {
        ImmutableBlockState newState = state.cycle(this.openProperty);
        LevelWriterProxy.INSTANCE.setBlock(world.minecraftWorld(), LocationUtils.toBlockPos(pos), newState.customBlockState().minecraftState(), UpdateFlags.UPDATE_ALL);
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

    private static class Factory implements BlockBehaviorFactory<TrapDoorBlockBehavior> {
        private static final String[] CAN_OPEN_WITH_HAND = new String[] {"can_open_with_hand", "can-open-with-hand"};
        private static final String[] CAN_OPEN_BY_WIND_CHARGE = new String[] {"can_open_by_wind_charge", "can-open-by-wind-charge"};

        @Override
        public TrapDoorBlockBehavior create(BlockDefinition block, ConfigSection section) {
            ConfigSection soundSection = section.getSection("sounds");
            SoundData openSound = null;
            SoundData closeSound = null;
            if (soundSection != null) {
                openSound = soundSection.getValue("open", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.RANGED_0_9_1));
                closeSound = soundSection.getValue("close", v -> SoundData.fromConfig(v, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.RANGED_0_9_1));
            }
            return new TrapDoorBlockBehavior(
                    block,
                    BlockBehaviorFactory.getProperty(section.path(), block, "half", SingleBlockHalf.class),
                    BlockBehaviorFactory.getProperty(section.path(), block, "facing", Direction.class),
                    BlockBehaviorFactory.getProperty(section.path(), block, "powered", Boolean.class),
                    BlockBehaviorFactory.getProperty(section.path(), block, "open", Boolean.class),
                    BlockBehaviorFactory.getOptionalProperty(block, "waterlogged", Boolean.class),
                    section.getBoolean(CAN_OPEN_WITH_HAND, true),
                    section.getBoolean(CAN_OPEN_BY_WIND_CHARGE, true),
                    openSound,
                    closeSound
            );
        }
    }
}
