package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntitySelectors;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MGameEvents;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.BooleanProperty;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.entity.player.InteractionResult;
import net.momirealms.craftengine.core.item.context.UseOnContext;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.HorizontalDirection;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.VersionHelper;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class ButtonBlockBehavior extends BukkitBlockBehavior {
    public static final Factory FACTORY = new Factory();
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
        if (!state.get(this.poweredProperty)) {
            press(BlockStateUtils.getBlockOwner(state.customBlockState().literalObject()),
                    state, context.getLevel().serverWorld(), LocationUtils.toBlockPos(context.getClickedPos()),
                    context.getPlayer() != null ? context.getPlayer().serverPlayer() : null);
            return InteractionResult.SUCCESS_AND_CANCEL;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onExplosionHit(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        ImmutableBlockState blockState = BlockStateUtils.getOptionalCustomBlockState(args[0]).orElse(null);
        if (blockState == null) return;
        if (FastNMS.INSTANCE.method$Explosion$canTriggerBlocks(args[3]) && !blockState.get(this.poweredProperty)) {
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
        Object arrow = this.canButtonBeActivatedByArrows ? FastNMS.INSTANCE.method$EntityGetter$getEntitiesOfClass(
                level, CoreReflections.clazz$AbstractArrow, FastNMS.INSTANCE.method$AABB$move(
                        FastNMS.INSTANCE.method$VoxelShape$bounds(FastNMS.INSTANCE.method$BlockState$getShape(
                                state, level, pos, CoreReflections.Instance.collisionContext$empty
                        )), pos), MEntitySelectors.NO_SPECTATORS).stream().findFirst().orElse(null) : null;
        boolean on = arrow != null;
        ImmutableBlockState blockState = BlockStateUtils.getOptionalCustomBlockState(state).orElse(null);
        if (blockState == null) return;
        boolean poweredValue = blockState.get(this.poweredProperty);
        if (on != poweredValue) {
            FastNMS.INSTANCE.method$LevelWriter$setBlock(level, pos, blockState.with(this.poweredProperty, on).customBlockState().literalObject(), UpdateOption.UPDATE_ALL.flags());
            updateNeighbours(thisBlock, blockState, level, pos);
            playSound(level, pos, on);
            Object gameEvent = VersionHelper.isOrAbove1_20_5()
                    ? FastNMS.INSTANCE.method$Holder$direct(on ? MGameEvents.BLOCK_ACTIVATE : MGameEvents.BLOCK_DEACTIVATE)
                    : on ? MGameEvents.BLOCK_ACTIVATE : MGameEvents.BLOCK_DEACTIVATE;
            FastNMS.INSTANCE.method$LevelAccessor$gameEvent(level, arrow, gameEvent, pos);
        }

        if (on) {
            FastNMS.INSTANCE.method$ScheduledTickAccess$scheduleBlockTick(level, pos, thisBlock, this.ticksToStayPressed);
        }
    }

    private void updateNeighbours(Object thisBlock, ImmutableBlockState state, Object level, Object pos) {
        Direction direction = FaceAttachedHorizontalDirectionalBlockBehavior.getConnectedDirection(state);
        if (direction == null) return;
        Direction opposite = direction.opposite();
        Object nmsDirection = DirectionUtils.toNMSDirection(opposite);
        Object orientation = null;
        if (VersionHelper.isOrAbove1_21_2()) {
            @SuppressWarnings("unchecked")
            Property<HorizontalDirection> facing = (Property<HorizontalDirection>) state.owner().value().getProperty("facing");
            if (facing != null) {
                orientation = FastNMS.INSTANCE.method$ExperimentalRedstoneUtils$initialOrientation(
                        level, nmsDirection, opposite.axis().isHorizontal() ? CoreReflections.Instance.direction$UP : DirectionUtils.toNMSDirection(state.get(facing).toDirection())
                );
            }
        }
        FastNMS.INSTANCE.method$Level$updateNeighborsAt(level, pos, thisBlock, orientation);
        FastNMS.INSTANCE.method$Level$updateNeighborsAt(level, FastNMS.INSTANCE.method$BlockPos$relative(pos, nmsDirection), thisBlock, orientation);
    }

    private void playSound(Object level, Object pos, boolean on) {
        SoundData soundData = getSound(on);
        if (soundData == null) return;
        Object sound = FastNMS.INSTANCE.constructor$SoundEvent(KeyUtils.toResourceLocation(soundData.id()), Optional.empty());
        FastNMS.INSTANCE.method$LevelAccessor$playSound(level, null, pos, sound, CoreReflections.Instance.soundSource$BLOCKS, soundData.volume().get(), soundData.pitch().get());
    }

    private SoundData getSound(boolean on) {
        return on ? this.buttonClickOnSound : this.buttonClickOffSound;
    }

    private void press(Object thisBlock, ImmutableBlockState state, Object level, Object pos, @Nullable Object player) {
        FastNMS.INSTANCE.method$LevelWriter$setBlock(level, pos, state.with(this.poweredProperty, true).customBlockState().literalObject(), UpdateOption.UPDATE_ALL.flags());
        FastNMS.INSTANCE.method$ScheduledTickAccess$scheduleBlockTick(level, pos, thisBlock, this.ticksToStayPressed);
        playSound(level, pos, true);
        Object gameEvent = VersionHelper.isOrAbove1_20_5() ? FastNMS.INSTANCE.method$Holder$direct(MGameEvents.BLOCK_ACTIVATE) : MGameEvents.BLOCK_ACTIVATE;
        FastNMS.INSTANCE.method$LevelAccessor$gameEvent(level, player, gameEvent, pos);
    }

    public static class Factory implements BlockBehaviorFactory {

        @SuppressWarnings({"unchecked", "DuplicatedCode"})
        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            BooleanProperty powered = (BooleanProperty) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("powered"), "warning.config.block.behavior.button.missing_powered");
            int ticksToStayPressed = ResourceConfigUtils.getAsInt(arguments.getOrDefault("ticks-to-stay-pressed", 30), "ticks-to-stay-pressed");
            boolean canButtonBeActivatedByArrows = ResourceConfigUtils.getAsBoolean(arguments.getOrDefault("can-be-activated-by-arrows", true), "can-be-activated-by-arrows");
            Map<String, Object> sounds = (Map<String, Object>) arguments.get("sounds");
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
