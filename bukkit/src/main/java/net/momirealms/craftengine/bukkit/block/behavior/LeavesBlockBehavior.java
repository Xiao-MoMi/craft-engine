package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.BlockTags;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.RandomTickBlock;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.core.MutableBlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelAccessorProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.LeavesBlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.StateHolderProxy;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.block.LeavesDecayEvent;

import java.util.Optional;

public final class LeavesBlockBehavior extends BukkitBlockBehavior implements RandomTickBlock {
    public static final BlockBehaviorFactory<LeavesBlockBehavior> FACTORY = new Factory();
    public static final Object LOG_TAG = BlockTags.getOrCreate(Key.of("minecraft", "logs"));
    public final int maxDistance;
    public final Property<Integer> distanceProperty;
    public final Property<Boolean> persistentProperty;
    public final Property<Boolean> waterloggedProperty;

    private LeavesBlockBehavior(BlockDefinition block,
                                Property<Integer> distanceProperty,
                                Property<Boolean> persistentProperty,
                                Property<Boolean> waterloggedProperty) {
        super(block);
        this.maxDistance = distanceProperty.possibleValues().getLast();
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

    @Override
    public boolean canRandomlyTick(ImmutableBlockState state) {
        return !isPersistent(state) && getDistance(state) == this.maxDistance;
    }

    public boolean isWaterLogged(ImmutableBlockState state) {
        if (this.waterloggedProperty == null) return false;
        return state.get(this.waterloggedProperty);
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args) {
        Object world = args[updateShape$level];
        Object blockPos = args[updateShape$blockPos];
        Object neighborState = args[updateShape$neighborState];
        Object blockState = args[0];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        optionalCustomState.ifPresent(state -> state.behavior().let(LeavesBlockBehavior.class, itsBehavior -> {
            int distance = itsBehavior.getDistanceAt(neighborState) + 1;
            if (distance != 1 || itsBehavior.getDistance(state) != distance) {
                LevelAccessorProxy.INSTANCE.scheduleTick$0(world, blockPos, thisBlock, 1);
            }
        }));
        return blockState;
    }

    @Override
    public void tick(Object thisBlock, Object[] args) {
        Object blockState = args[0];
        Object level = args[1];
        Object blockPos = args[2];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        optionalCustomState.ifPresent(customState -> customState.behavior().let(LeavesBlockBehavior.class, itsBehavior -> {
            ImmutableBlockState newState = itsBehavior.updateDistance(customState, level, blockPos);
            if (newState != customState) {
                if (blockState == newState.customBlockState().minecraftState()) {
                    BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.updateNeighbourShapes(blockState, level, blockPos, UpdateFlags.UPDATE_ALL, 512);
                } else {
                    LevelWriterProxy.INSTANCE.setBlock(level, blockPos, newState.customBlockState().minecraftState(), UpdateFlags.UPDATE_ALL);
                }
            }
        }));
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public void randomTick(Object thisBlock, Object[] args) {
        Object blockState = args[0];
        Object level = args[1];
        Object blockPos = args[2];
        BlockStateUtils.getOptionalCustomBlockState(blockState).ifPresent(customState -> {
            // 可能是另一种树叶
            customState.behavior().let(LeavesBlockBehavior.class, itsBehavior -> {
                if (itsBehavior.isDecaying(customState)) {
                    World bukkitWorld = LevelProxy.INSTANCE.getWorld(level);
                    BlockPos pos = LocationUtils.fromBlockPos(blockPos);
                    // call bukkit event
                    LeavesDecayEvent event = new LeavesDecayEvent(bukkitWorld.getBlockAt(pos.x(), pos.y(), pos.z()));
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        return;
                    }
                    LevelProxy.INSTANCE.removeBlock(level, blockPos, false);
                    BlockProxy.INSTANCE.dropResources(blockState, level, blockPos);
                }
            });
        });
    }

    private boolean isDecaying(ImmutableBlockState blockState) {
        return !isPersistent(blockState) && getDistance(blockState) == this.maxDistance;
    }

    private ImmutableBlockState updateDistance(ImmutableBlockState state, Object world, Object blockPos) {
        int i = this.maxDistance;
        Object mutablePos = MutableBlockPosProxy.INSTANCE.newInstance();
        int j = Direction.values().length;
        for (int k = 0; k < j; ++k) {
            Object direction = DirectionProxy.VALUES[k];
            MutableBlockPosProxy.INSTANCE.setWithOffset(mutablePos, blockPos, direction);
            Object blockState = BlockGetterProxy.INSTANCE.getBlockState(world, mutablePos);
            i = Math.min(i, getDistanceAt(blockState) + 1);
            if (i == 1) {
                break;
            }
        }
        return state.with(this.distanceProperty, i);
    }

    private int getDistanceAt(Object blockState) {
        boolean isLog = BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$1(blockState, LOG_TAG);
        if (isLog) return 0;
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(blockState);
        if (optionalCustomState.isEmpty()) {
            boolean hasDistanceProperty = StateHolderProxy.INSTANCE.hasProperty(blockState, LeavesBlockProxy.DISTANCE);
            if (!hasDistanceProperty) return this.maxDistance;
            return StateHolderProxy.INSTANCE.getValue(blockState, LeavesBlockProxy.DISTANCE);
        } else {
            ImmutableBlockState anotherBlockState = optionalCustomState.get();
            LeavesBlockBehavior leavesBlockBehavior = anotherBlockState.behavior().getFirst(LeavesBlockBehavior.class);
            if (leavesBlockBehavior == null) return this.maxDistance;
            return leavesBlockBehavior.getDistance(anotherBlockState);
        }
    }

    private static class Factory implements BlockBehaviorFactory<LeavesBlockBehavior> {

        @Override
        public LeavesBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new LeavesBlockBehavior(
                    block,
                    BlockBehaviorFactory.getProperty(section.path(), block, "distance", Integer.class),
                    BlockBehaviorFactory.getProperty(section.path(), block, "persistent", Boolean.class),
                    BlockBehaviorFactory.getOptionalProperty(block, "waterlogged", Boolean.class)
            );
        }
    }
}
