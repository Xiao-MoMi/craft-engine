package net.momirealms.craftengine.bukkit.block.behavior;

import io.papermc.paper.event.entity.EntityInsideBlockEvent;
import net.momirealms.antigrieflib.Flag;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.*;
import net.momirealms.craftengine.bukkit.world.BukkitWorldManager;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldEvents;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.block.CraftBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.core.Vec3iProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntitySelectorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.LivingEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.EntityGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BasePressurePlateBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.AABBProxy;
import org.bukkit.GameEvent;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class PressurePlateBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<PressurePlateBlockBehavior> FACTORY = new Factory();
    private final Property<Boolean> poweredProperty;
    private final SoundData onSound;
    private final SoundData offSound;
    private final PressurePlateSensitivity pressurePlateSensitivity;
    private final int pressedTime;

    public PressurePlateBlockBehavior(
            CustomBlock block,
            Property<Boolean> poweredProperty,
            SoundData onSound,
            SoundData offSound,
            PressurePlateSensitivity pressurePlateSensitivity,
            int pressedTime
    ) {
        super(block);
        this.poweredProperty = poweredProperty;
        this.onSound = onSound;
        this.offSound = offSound;
        this.pressurePlateSensitivity = pressurePlateSensitivity;
        this.pressedTime = pressedTime;
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object state = args[0];
        Object level = args[updateShape$level];
        Object blockPos = args[updateShape$blockPos];
        Direction direction = DirectionUtils.fromNMSDirection(VersionHelper.isOrAbove1_21_2() ? args[4] : args[1]);
        if (direction == Direction.DOWN && !BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.canSurvive(state, level, blockPos)) {
            Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(state);
            if (optionalCustomState.isEmpty()) {
                return BlocksProxy.AIR$defaultState;
            }
            ImmutableBlockState customState = optionalCustomState.get();
            LevelAccessorProxy.INSTANCE.levelEvent(level, WorldEvents.BLOCK_BREAK_EFFECT, blockPos, customState.customBlockState().registryId());
            return BlocksProxy.AIR$defaultState;
        }
        return state;
    }

    @Override
    public boolean canSurvive(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object blockPos = LocationUtils.below(args[2]);
        Object level = args[1];
        return BlockProxy.INSTANCE.canSupportRigidBlock(level, blockPos)
                || BlockProxy.INSTANCE.canSupportCenter(level, blockPos, DirectionProxy.UP);
    }

    @Override
    public void tick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object state = args[0];
        int signalForState = this.getSignalForState(state);
        if (signalForState > 0) {
            this.checkPressed(null, args[1], args[2], state, signalForState, thisBlock);
        }
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public void entityInside(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Entity entity = EntityProxy.INSTANCE.getBukkitEntity(args[3]);
        Block block = CraftBlockProxy.INSTANCE.at(args[1], args[2]);
        EntityInsideBlockEvent event = new EntityInsideBlockEvent(entity, block);
        if (EventUtils.fireAndCheckCancel(event)) {
            return;
        }
        boolean cannotInteract = entity instanceof Player p && !BukkitCraftEngine.instance().antiGriefProvider().test(p, Flag.USE_PRESSURE_PLATE, block.getLocation());
        if (cannotInteract) {
            return;
        }
        Object state = args[0];
        int signalForState = this.getSignalForState(state);
        if (signalForState == 0) {
            this.checkPressed(args[3], args[1], args[2], state, signalForState, thisBlock);
        }
    }

    protected int getSignalStrength(Object level, Object pos) {
        Class<?> clazz = switch (this.pressurePlateSensitivity) {
            case EVERYTHING -> EntityProxy.CLASS;
            case MOBS -> LivingEntityProxy.CLASS;
        };
        Object box = AABBProxy.INSTANCE.move$1(BasePressurePlateBlockProxy.INSTANCE.getTouchAABB(), pos);
        return !EntityGetterProxy.INSTANCE.getEntitiesOfClass(
                level, clazz, box,
                EntitySelectorProxy.NO_SPECTATORS.and(entity -> !EntityProxy.INSTANCE.isIgnoringBlockTriggers(entity))
        ).isEmpty() ? 15 : 0;
    }

    private Object setSignalForState(Object state, int strength) {
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(state);
        if (optionalCustomState.isEmpty()) return state;
        return optionalCustomState.get().with(this.poweredProperty, strength > 0).customBlockState().literalObject();
    }

    private void checkPressed(@Nullable Object entity, Object level, Object pos, Object state, int currentSignal, Object thisBlock) {
        int signalStrength = this.getSignalStrength(level, pos);
        boolean wasActive = currentSignal > 0;
        boolean isActive = signalStrength > 0;

        if (currentSignal != signalStrength) {
            Object blockState = this.setSignalForState(state, signalStrength);
            LevelWriterProxy.INSTANCE.setBlock(level, pos, blockState, 2);
            this.updateNeighbours(level, pos, thisBlock);
            LevelProxy.INSTANCE.setBlocksDirty(level, pos, state, blockState);
        }

        org.bukkit.World craftWorld = LevelProxy.INSTANCE.getWorld(level);
        int x = Vec3iProxy.INSTANCE.getX(pos);
        int y = Vec3iProxy.INSTANCE.getY(pos);
        int z = Vec3iProxy.INSTANCE.getZ(pos);
        Vector positionVector = new Vector(x, y, z);

        if (!isActive && wasActive) {
            handleDeactivation(entity, craftWorld, pos, positionVector);
        } else if (isActive && !wasActive) {
            handleActivation(entity, craftWorld, pos, positionVector);
        }

        if (isActive) {
            LevelUtils.scheduleBlockTick(level, pos, thisBlock, this.pressedTime);
        }
    }

    private void handleDeactivation(Object entity, org.bukkit.World craftWorld, Object pos, Vector positionVector) {
        World world = BukkitWorldManager.instance().getWorld(craftWorld).world();
        world.playBlockSound(LocationUtils.toVec3d(LocationUtils.fromBlockPos(pos)), this.offSound);
        craftWorld.sendGameEvent(
                entity != null ? EntityProxy.INSTANCE.getBukkitEntity(entity) : null,
                GameEvent.BLOCK_DEACTIVATE,
                positionVector
        );
    }

    private void handleActivation(Object entity, org.bukkit.World craftWorld, Object pos, Vector positionVector) {
        World world = BukkitWorldManager.instance().getWorld(craftWorld).world();
        world.playBlockSound(LocationUtils.toVec3d(LocationUtils.fromBlockPos(pos)), this.onSound);
        craftWorld.sendGameEvent(
                entity != null ? EntityProxy.INSTANCE.getBukkitEntity(entity) : null,
                GameEvent.BLOCK_ACTIVATE,
                positionVector
        );
    }

    @Override
    public void affectNeighborsAfterRemoval(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        boolean flag;
        if (VersionHelper.isOrAbove1_21_5()) {
            flag = !(boolean) args[3];
        } else {
            flag = !(boolean) args[4] && !BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$0(args[0], BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getBlock(args[3]));
        }
        if (flag) {
            if (this.getSignalForState(args[0]) > 0) {
                this.updateNeighbours(args[1], args[2], thisBlock);
            }
            if (!VersionHelper.isOrAbove1_21_5()) {
                superMethod.call();
            }
        }
    }

    private void updateNeighbours(Object level, Object pos, Object thisBlock) {
        if (VersionHelper.isOrAbove1_21_5()) {
            LevelAccessorProxy.INSTANCE.updateNeighborsAt(level, pos, thisBlock);
            LevelAccessorProxy.INSTANCE.updateNeighborsAt(level, LocationUtils.below(pos), thisBlock);
        } else {
            LevelProxy.INSTANCE.updateNeighborsAt(level, pos, thisBlock);
            LevelProxy.INSTANCE.updateNeighborsAt(level, LocationUtils.below(pos), thisBlock);
        }
    }

    @Override
    public int getSignal(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        return this.getSignalForState(args[0]);
    }

    private int getSignalForState(Object state) {
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(state);
        return optionalCustomState.filter(immutableBlockState -> immutableBlockState.get(this.poweredProperty)).map(immutableBlockState -> 15).orElse(0);
    }

    @Override
    public int getDirectSignal(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Direction direction = DirectionUtils.fromNMSDirection(args[3]);
        return direction == Direction.UP ? this.getSignalForState(args[0]) : 0;
    }

    @Override
    public boolean isSignalSource(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        return true;
    }

    private static class Factory implements BlockBehaviorFactory<PressurePlateBlockBehavior> {

        @SuppressWarnings({"unchecked", "DuplicatedCode"})
        @Override
        public PressurePlateBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Property<Boolean> powered = (Property<Boolean>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("powered"), "warning.config.block.behavior.pressure_plate.missing_powered");
            PressurePlateSensitivity pressurePlateSensitivity = PressurePlateSensitivity.byName(arguments.getOrDefault("sensitivity", "everything").toString());
            int pressedTime = ResourceConfigUtils.getAsInt(arguments.getOrDefault("pressed-time", 20), "pressed-time");
            Map<String, Object> sounds = MiscUtils.castToMap(arguments.get("sounds"), true);
            SoundData onSound = null;
            SoundData offSound = null;
            if (sounds != null) {
                onSound = Optional.ofNullable(sounds.get("on")).map(obj -> SoundData.create(obj, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.ranged(0.9f, 1f))).orElse(null);
                offSound = Optional.ofNullable(sounds.get("off")).map(obj -> SoundData.create(obj, SoundData.SoundValue.FIXED_1, SoundData.SoundValue.ranged(0.9f, 1f))).orElse(null);
            }
            return new PressurePlateBlockBehavior(block, powered, onSound, offSound, pressurePlateSensitivity, pressedTime);
        }
    }
}
