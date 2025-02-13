package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.BlockTags;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.parameter.LootParameters;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.util.context.ContextHolder;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.shared.block.BlockBehavior;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.block.LeavesDecayEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.Callable;

public class LeavesBlockBehavior extends BlockBehavior {
    public static final Factory FACTORY = new Factory();
    private static final Object LOG_TAG = BlockTags.getOrCreate(Key.of("minecraft", "logs"));
    private final int maxDistance;
    private final Property<Integer> distanceProperty;
    private final Property<Boolean> persistentProperty;
    @Nullable
    private final Property<Boolean> waterloggedProperty;

    public LeavesBlockBehavior(int maxDistance, Property<Integer> distanceProperty, Property<Boolean> persistentProperty, @Nullable Property<Boolean> waterloggedProperty) {
        this.maxDistance = maxDistance;
        this.distanceProperty = distanceProperty;
        this.persistentProperty = persistentProperty;
        this.waterloggedProperty = waterloggedProperty;
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
        Object world;
        Object blockPos;
        Object neighborState;
        Object blockState = args[0];
        if (VersionHelper.isVersionNewerThan1_21_2()) {
            world = args[1];
            neighborState = args[6];
            blockPos = args[3];
        } else {
            world = args[3];
            blockPos = args[4];
            neighborState = args[2];
        }
        ImmutableBlockState thisState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(blockState));
        if (thisState != null && thisState.behavior() instanceof LeavesBlockBehavior behavior) {
            int distance = behavior.getDistanceAt(neighborState) + 1;
            if (distance != 1 || behavior.getDistance(thisState) != distance) {
                Reflections.method$LevelAccessor$scheduleTick.invoke(world, blockPos, thisBlock, 1);
            }
        }
        return blockState;
    }

    @Override
    public void tick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object blockState = args[0];
        Object level = args[1];
        Object blockPos = args[2];
        ImmutableBlockState currentState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(blockState));
        if (currentState != null && !currentState.isEmpty() && currentState.behavior() instanceof LeavesBlockBehavior behavior) {
            ImmutableBlockState newState = behavior.updateDistance(currentState, level, blockPos);
            if (newState != currentState) {
                if (blockState == newState.customBlockState().handle()) {
                    Reflections.method$BlockStateBase$updateNeighbourShapes.invoke(blockState, level, blockPos, UpdateOption.UPDATE_ALL.flags(), 512);
                } else {
                    Reflections.method$Level$setBlock.invoke(level, blockPos, newState.customBlockState().handle(), UpdateOption.UPDATE_ALL.flags());
                }
            }
        }
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object level = args[1];
        Object blockPos = args[2];
        ImmutableBlockState immutableBlockState = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(args[0]));
        if (immutableBlockState != null && immutableBlockState.behavior() instanceof LeavesBlockBehavior behavior && behavior.isDecaying(immutableBlockState)) {
            World bukkitWorld = (World) Reflections.method$Level$getCraftWorld.invoke(level);
            BlockPos pos = LocationUtils.fromBlockPos(blockPos);
            // call bukkit event
            LeavesDecayEvent event = new LeavesDecayEvent(bukkitWorld.getBlockAt(pos.x(), pos.y(), pos.z()));
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                return;
            }
            Reflections.method$Level$removeBlock.invoke(level, blockPos, false);
            if (isWaterLogged(immutableBlockState)) {
                bukkitWorld.setBlockData(pos.x(), pos.y(), pos.z(), Material.WATER.createBlockData());
            }
            Vec3d vec3d = Vec3d.atCenterOf(pos);
            net.momirealms.craftengine.core.world.World world = new BukkitWorld(bukkitWorld);
            ContextHolder.Builder builder = ContextHolder.builder()
                    .withParameter(LootParameters.LOCATION, vec3d)
                    .withParameter(LootParameters.WORLD, world);
            for (Item<Object> item : immutableBlockState.getDrops(builder, world)) {
                world.dropItemNaturally(vec3d, item);
            }
        }
    }

    private boolean isDecaying(ImmutableBlockState blockState) {
        return !isPersistent(blockState) && getDistance(blockState) == this.maxDistance;
    }

    private ImmutableBlockState updateDistance(ImmutableBlockState state, Object world, Object blockPos) throws ReflectiveOperationException {
        int i = this.maxDistance;
        Object mutablePos = Reflections.constructor$MutableBlockPos.newInstance();
        int j = Direction.values().length;
        for (int k = 0; k < j; ++k) {
            Object direction = Reflections.instance$Directions[k];
            Reflections.method$MutableBlockPos$setWithOffset.invoke(mutablePos, blockPos, direction);
            Object blockState = Reflections.method$BlockGetter$getBlockState.invoke(world, mutablePos);
            i = Math.min(i, getDistanceAt(blockState) + 1);
            if (i == 1) {
                break;
            }
        }
        return state.with(this.distanceProperty, i);
    }

    private int getDistanceAt(Object blockState) throws ReflectiveOperationException {
        boolean isLog = (boolean) Reflections.method$BlockStateBase$hasTag.invoke(blockState, LOG_TAG);
        if (isLog) return 0;
        int id = BlockStateUtils.blockStateToId(blockState);
        if (BlockStateUtils.isVanillaBlock(id)) {
            Object distanceProperty = Reflections.field$LeavesBlock$DISTANCE.get(null);
            boolean hasDistanceProperty = (boolean) Reflections.method$StateHolder$hasProperty.invoke(blockState, distanceProperty);
            if (!hasDistanceProperty) return this.maxDistance;
            return (int) Reflections.method$StateHolder$getValue.invoke(blockState, distanceProperty);
        } else {
            ImmutableBlockState anotherBlockState = BukkitBlockManager.instance().getImmutableBlockStateUnsafe(id);
            if (!(anotherBlockState.behavior() instanceof LeavesBlockBehavior otherBehavior)) return this.maxDistance;
            return otherBehavior.getDistance(anotherBlockState);
        }
    }

    @SuppressWarnings("unchecked")
    public static class Factory implements BlockBehaviorFactory {
        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Property<Boolean> persistent = (Property<Boolean>) block.getProperty("persistent");
            if (persistent == null) {
                throw new NullPointerException("persistent property not set for block " + block.id());
            }
            Property<Integer> distance = (Property<Integer>) block.getProperty("distance");
            if (distance == null) {
                throw new NullPointerException("distance not set for block " + block.id());
            }
            Property<Boolean> waterlogged = (Property<Boolean>) block.getProperty("waterlogged");
            int actual = distance.possibleValues().get(distance.possibleValues().size() - 1);
            return new LeavesBlockBehavior(actual, distance, persistent, waterlogged);
        }
    }

    @Override
    public Object getFluidState(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object blockState = args[0];
        ImmutableBlockState state = BukkitBlockManager.instance().getImmutableBlockState(BlockStateUtils.blockStateToId(blockState));
        if (state == null || state.isEmpty() || waterloggedProperty == null) return super.getFluidState(thisBlock, args, superMethod);
        boolean waterlogged = state.get(waterloggedProperty);
        return waterlogged ? Reflections.method$FlowingFluid$getSource.invoke(Reflections.instance$Fluids$WATER, false) : super.getFluidState(thisBlock, args, superMethod);
    }
}
