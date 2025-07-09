package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.BlockTags;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.block.LeavesDecayEvent;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

public class LeavesBlockBehavior extends BukkitBlockBehavior {
    public static final Factory FACTORY = new Factory();
    private static final Object LOG_TAG = BlockTags.getOrCreate(Key.of("minecraft", "logs"));
    private final int maxDistance;
    private final Property<Integer> distanceProperty;
    private final Property<Boolean> persistentProperty;

    public LeavesBlockBehavior(CustomBlock block,
                               int maxDistance,
                               Property<Integer> distanceProperty,
                               Property<Boolean> persistentProperty) {
        super(block);
        this.maxDistance = maxDistance;
        this.distanceProperty = distanceProperty;
        this.persistentProperty = persistentProperty;
    }

    public int getDistance(ImmutableBlockState state) {
        return state.get(this.distanceProperty);
    }

    public boolean isPersistent(ImmutableBlockState state) {
        return state.get(this.persistentProperty);
    }

    public boolean isWaterLogged(ImmutableBlockState state) {
        if (this.waterloggedProperty == null) return false;
        return state.get(this.waterloggedProperty);
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object world = args[updateShape$level];
        Object blockPos = args[updateShape$blockPos];
        Object neighborState = args[updateShape$neighborState];
        Object blockState = args[0];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isPresent()) {
            Optional<LeavesBlockBehavior> optionalBehavior = optionalCustomState.get().behavior().getAs(LeavesBlockBehavior.class);
            if (optionalBehavior.isPresent()) {
                LeavesBlockBehavior behavior = optionalBehavior.get();
                int distance = behavior.getDistanceAt(neighborState) + 1;
                if (distance != 1 || behavior.getDistance(optionalCustomState.get()) != distance) {
                    FastNMS.INSTANCE.method$LevelAccessor$scheduleBlockTick(world, blockPos, thisBlock, 1);
                }
            }
        }
        return blockState;
    }

    @Override
    public void tick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object blockState = args[0];
        Object level = args[1];
        Object blockPos = args[2];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isPresent()) {
            ImmutableBlockState customState = optionalCustomState.get();
            Optional<LeavesBlockBehavior> optionalBehavior = customState.behavior().getAs(LeavesBlockBehavior.class);
            if (optionalBehavior.isPresent()) {
                LeavesBlockBehavior behavior = optionalBehavior.get();
                ImmutableBlockState newState = behavior.updateDistance(customState, level, blockPos);
                if (newState != customState) {
                    if (blockState == newState.customBlockState().handle()) {
                        CoreReflections.method$BlockStateBase$updateNeighbourShapes.invoke(blockState, level, blockPos, UpdateOption.UPDATE_ALL.flags(), 512);
                    } else {
                        FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, newState.customBlockState().handle(), UpdateOption.UPDATE_ALL.flags());
                    }
                }
            }
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object blockState = args[0];
        Object level = args[1];
        Object blockPos = args[2];
        BlockStateUtils.getOptionalCustomBlockState(blockState).ifPresent(customState -> {
            // 可能是另一种树叶
            Optional<LeavesBlockBehavior> optionalBehavior = customState.behavior().getAs(LeavesBlockBehavior.class);
            if (optionalBehavior.isPresent()) {
                LeavesBlockBehavior behavior = optionalBehavior.get();
                if (behavior.isDecaying(customState)) {
                    World bukkitWorld = FastNMS.INSTANCE.method$Level$getCraftWorld(level);
                    BlockPos pos = LocationUtils.fromBlockPos(blockPos);
                    // call bukkit event
                    LeavesDecayEvent event = new LeavesDecayEvent(bukkitWorld.getBlockAt(pos.x(), pos.y(), pos.z()));
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        return;
                    }
                    FastNMS.INSTANCE.method$Level$removeBlock(level, blockPos, false);
                    FastNMS.INSTANCE.method$Block$dropResources(blockState, level, blockPos);
                }
            }
        });
    }

    private boolean isDecaying(ImmutableBlockState blockState) {
        return !isPersistent(blockState) && getDistance(blockState) == this.maxDistance;
    }

    private ImmutableBlockState updateDistance(ImmutableBlockState state, Object world, Object blockPos) throws ReflectiveOperationException {
        int i = this.maxDistance;
        Object mutablePos = CoreReflections.constructor$MutableBlockPos.newInstance();
        int j = Direction.values().length;
        for (int k = 0; k < j; ++k) {
            Object direction = CoreReflections.instance$Directions[k];
            CoreReflections.method$MutableBlockPos$setWithOffset.invoke(mutablePos, blockPos, direction);
            Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, mutablePos);
            i = Math.min(i, getDistanceAt(blockState) + 1);
            if (i == 1) {
                break;
            }
        }
        return state.with(this.distanceProperty, i);
    }

    private int getDistanceAt(Object blockState) throws ReflectiveOperationException {
        boolean isLog = FastNMS.INSTANCE.method$BlockStateBase$is(blockState, LOG_TAG);
        if (isLog) return 0;
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) {
            Object distanceProperty = CoreReflections.field$LeavesBlock$DISTANCE.get(null);
            boolean hasDistanceProperty = (boolean) CoreReflections.method$StateHolder$hasProperty.invoke(blockState, distanceProperty);
            if (!hasDistanceProperty) return this.maxDistance;
            return (int) CoreReflections.method$StateHolder$getValue.invoke(blockState, distanceProperty);
        } else {
            ImmutableBlockState anotherBlockState = optionalCustomState.get();
            Optional<LeavesBlockBehavior> optionalAnotherBehavior = anotherBlockState.behavior().getAs(LeavesBlockBehavior.class);
            return optionalAnotherBehavior.map(leavesBlockBehavior -> leavesBlockBehavior.getDistance(anotherBlockState)).orElse(this.maxDistance);
        }
    }

    @SuppressWarnings("unchecked")
    public static class Factory implements BlockBehaviorFactory {
        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Property<Boolean> persistent = (Property<Boolean>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("persistent"), "warning.config.block.behavior.leaves.missing_persistent");
            Property<Integer> distance = (Property<Integer>) ResourceConfigUtils.requireNonNullOrThrow(block.getProperty("distance"), "warning.config.block.behavior.leaves.missing_distance");
            int actual = distance.possibleValues().get(distance.possibleValues().size() - 1);
            return new LeavesBlockBehavior(block, actual, distance, persistent);
        }
    }
}
