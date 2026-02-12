package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntitySelectors;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MGameEvents;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.BooleanProperty;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.context.UseOnContext;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundEventProxy;
import net.momirealms.craftengine.proxy.minecraft.sounds.SoundSourceProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.projectile.AbstractArrowProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.*;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.redstone.ExperimentalRedstoneUtilsProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.AABBProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.shape.CollisionContextProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.shape.VoxelShapeProxy;
import org.bukkit.Location;
import org.bukkit.World;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class ButtonBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<ButtonBlockBehavior> FACTORY = new Factory();
    private final BooleanProperty poweredProperty;
    private final int ticksToStayPressed;
    private final boolean canButtonBeActivatedByArrows;
    private final SoundData buttonClickOnSound;
    private final SoundData buttonClickOffSound;

    public ButtonBlockBehavior(CustomBlock customBlock,
                               BooleanProperty powered,
                               int ticksToStayPressed,
                               boolean canButtonBeActivatedByArrows,
                               SoundData buttonClickOnSound,
                               SoundData buttonClickOffSound) {
        super(customBlock);
        this.poweredProperty = powered;
        this.ticksToStayPressed = ticksToStayPressed;
        this.canButtonBeActivatedByArrows = canButtonBeActivatedByArrows;
        this.buttonClickOnSound = buttonClickOnSound;
        this.buttonClickOffSound = buttonClickOffSound;
    }

    @Override
    public InteractionResult useWithoutItem(UseOnContext context, ImmutableBlockState state) {
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        net.momirealms.craftengine.core.world.World world = context.getLevel();
        if (player != null) {
            Location location = new Location((World) world.platformWorld(), pos.x, pos.y, pos.z);
            if (!BukkitCraftEngine.instance().antiGriefProvider().test((org.bukkit.entity.Player) player.platformPlayer(), Flag.USE_BUTTON, location)) {
                return InteractionResult.SUCCESS_AND_CANCEL;
            }
        }
        if (!state.get(this.poweredProperty)) {
            press(BlockStateUtils.getBlockOwner(state.customBlockState().literalObject()),
                    state, world.serverWorld(), LocationUtils.toBlockPos(pos),
                    player != null ? player.serverPlayer() : null);
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onExplosionHit(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        ImmutableBlockState blockState = BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);
        if (blockState == null) return;
        if (ExplosionProxy.INSTANCE.canTriggerBlocks(args[3]) && !blockState.get(this.poweredProperty)) {
            press(thisBlock, blockState, args[1], args[2], null);
        }
    }

    @Override
    public void affectNeighborsAfterRemoval(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        ImmutableBlockState blockState = BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);
        if (blockState == null) return;
        if (!(boolean) args[3] && blockState.get(this.poweredProperty)) {
            updateNeighbours(thisBlock, blockState, args[1], args[2]);
        }
    }

    @Override
    public void onRemove(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        ImmutableBlockState blockState = BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);
        if (blockState == null) return;
        if (!(boolean) args[4] && blockState.get(this.poweredProperty)) {
            updateNeighbours(thisBlock, blockState, args[1], args[2]);
        }
    }

    @Override
    public int getSignal(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        ImmutableBlockState blockState = BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);
        if (blockState == null) return 0;
        return blockState.get(this.poweredProperty) ? 15 : 0;
    }

    @Override
    public int getDirectSignal(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        ImmutableBlockState blockState = BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);
        if (blockState == null) return 0;
        return blockState.get(this.poweredProperty)
                && FaceAttachedHorizontalDirectionalBlockBehavior.getConnectedDirection(blockState)
                == DirectionUtils.fromNMSDirection(args[3]) ? 15 : 0;
    }

    @Override
    public boolean isSignalSource(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        return true;
    }

    @Override
    public void tick(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object state = args[0];
        Object level = args[1];
        Object pos = args[2];
        ImmutableBlockState blockState = BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
        if (blockState == null) return;
        if (blockState.get(this.poweredProperty)) {
            checkPressed(thisBlock, state, level, pos);
        }
    }

    @Override
    public void entityInside(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object state = args[0];
        Object level = args[1];
        Object pos = args[2];
        ImmutableBlockState blockState = BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
        if (blockState == null) return;
        if (this.canButtonBeActivatedByArrows && !blockState.get(this.poweredProperty)) {
            checkPressed(thisBlock, state, level, pos);
        }
    }

    private void checkPressed(Object thisBlock, Object state, Object level, Object pos) {
        Object arrow = this.canButtonBeActivatedByArrows ? EntityGetterProxy.INSTANCE.getEntitiesOfClass(
                level, AbstractArrowProxy.CLASS, AABBProxy.INSTANCE.move$1(
                        VoxelShapeProxy.INSTANCE.bounds(BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getShape(
                                state, level, pos, CollisionContextProxy.INSTANCE.empty()
                        )), pos), MEntitySelectors.NO_SPECTATORS).stream().findFirst().orElse(null) : null;
        boolean on = arrow != null;
        ImmutableBlockState blockState = BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
        if (blockState == null) return;
        boolean poweredValue = blockState.get(this.poweredProperty);
        if (on != poweredValue) {
            LevelWriterProxy.INSTANCE.setBlock(level, pos, blockState.with(this.poweredProperty, on).customBlockState().literalObject(), UpdateOption.UPDATE_ALL.flags());
            updateNeighbours(thisBlock, blockState, level, pos);
            playSound(level, pos, on);
            if (VersionHelper.isOrAbove1_20_5()) {
                LevelAccessorProxy.INSTANCE.gameEvent$0(level, arrow, on ? MGameEvents.BLOCK_ACTIVATE$holder : MGameEvents.BLOCK_DEACTIVATE$holder, pos);
            } else {
                LevelAccessorProxy.INSTANCE.gameEvent$1(level, arrow, on ? MGameEvents.BLOCK_ACTIVATE : MGameEvents.BLOCK_DEACTIVATE, pos);
            }
        }

        if (on) {
            LevelUtils.scheduleBlockTick(level, pos, thisBlock, this.ticksToStayPressed);
        }
    }

    private void updateNeighbours(Object thisBlock, ImmutableBlockState state, Object level, Object pos) {
        Direction direction = FaceAttachedHorizontalDirectionalBlockBehavior.getConnectedDirection(state);
        if (direction == null) return;
        Direction opposite = direction.opposite();
        Object nmsDirection = DirectionUtils.toNMSDirection(opposite);
        if (VersionHelper.isOrAbove1_21_2()) {
            @SuppressWarnings("unchecked")
            Property<HorizontalDirection> facing = (Property<HorizontalDirection>) state.owner().value().getProperty("facing");
            Object orientation = null;
            if (facing != null) {
                orientation = ExperimentalRedstoneUtilsProxy.INSTANCE.initialOrientation(
                        level, nmsDirection, opposite.axis().isHorizontal() ? DirectionProxy.UP : DirectionUtils.toNMSDirection(state.get(facing).toDirection())
                );
            }
            LevelProxy.INSTANCE.updateNeighborsAt(level, pos, thisBlock, orientation);
            LevelProxy.INSTANCE.updateNeighborsAt(level, BlockPosProxy.INSTANCE.relative(pos, nmsDirection), thisBlock, orientation);
        } else {
            LevelProxy.INSTANCE.updateNeighborsAt(level, pos, thisBlock);
            LevelProxy.INSTANCE.updateNeighborsAt(level, BlockPosProxy.INSTANCE.relative(pos, nmsDirection), thisBlock);
        }
    }

    private void playSound(Object level, Object pos, boolean on) {
        SoundData soundData = getSound(on);
        if (soundData == null) return;
        Object sound = SoundEventProxy.INSTANCE.create(KeyUtils.toIdentifier(soundData.id()), Optional.empty());
        if (VersionHelper.isOrAbove1_21_5()) {
            LevelAccessorProxy.INSTANCE.playSound$0(level, null, pos, sound, SoundSourceProxy.BLOCKS, soundData.volume().get(), soundData.pitch().get());
        } else {
            LevelAccessorProxy.INSTANCE.playSound$1(level, null, pos, sound, SoundSourceProxy.BLOCKS, soundData.volume().get(), soundData.pitch().get());
        }
    }

    private SoundData getSound(boolean on) {
        return on ? this.buttonClickOnSound : this.buttonClickOffSound;
    }

    private void press(Object thisBlock, ImmutableBlockState state, Object level, Object pos, @Nullable Object player) {
        LevelWriterProxy.INSTANCE.setBlock(level, pos, state.with(this.poweredProperty, true).customBlockState().literalObject(), UpdateOption.UPDATE_ALL.flags());
        this.updateNeighbours(thisBlock, state, level, pos);
        LevelUtils.scheduleBlockTick(level, pos, thisBlock, this.ticksToStayPressed);
        playSound(level, pos, true);
        if (VersionHelper.isOrAbove1_20_5()) {
            LevelAccessorProxy.INSTANCE.gameEvent$0(level, player, MGameEvents.BLOCK_ACTIVATE$holder, pos);
        } else {
            LevelAccessorProxy.INSTANCE.gameEvent$1(level, player, MGameEvents.BLOCK_ACTIVATE, pos);
        }
    }

    private static class Factory implements BlockBehaviorFactory<ButtonBlockBehavior> {

        @SuppressWarnings("DuplicatedCode")
        @Override
        public ButtonBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            BooleanProperty powered = (BooleanProperty) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("powered"), "warning.config.block.behavior.button.missing_powered");
            int ticksToStayPressed = ResourceConfigUtils.getAsInt(arguments.getOrDefault("ticks-to-stay-pressed", 30), "ticks-to-stay-pressed");
            boolean canButtonBeActivatedByArrows = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("can-be-activated-by-arrows", true), "can-be-activated-by-arrows");
            Map<String, Object> sounds = MiscUtils.castToMap(arguments.get("sounds"), true);
            SoundData buttonClickOnSound = null;
            SoundData buttonClickOffSound = null;
            if (sounds != null) {
                buttonClickOnSound = Optional.ofNullable(sounds.get("on")).map(obj -> SoundData.create(obj, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.ranged(0.9f, 1f))).orElse(null);
                buttonClickOffSound = Optional.ofNullable(sounds.get("off")).map(obj -> SoundData.create(obj, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.ranged(0.9f, 1f))).orElse(null);
            }
            return new ButtonBlockBehavior(block, powered, ticksToStayPressed, canButtonBeActivatedByArrows, buttonClickOnSound, buttonClickOffSound);
        }
    }
}
