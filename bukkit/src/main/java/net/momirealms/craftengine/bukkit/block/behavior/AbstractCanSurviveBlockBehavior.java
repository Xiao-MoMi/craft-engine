package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldEvents;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.Optional;
import java.util.concurrent.Callable;

public abstract class AbstractCanSurviveBlockBehavior extends BukkitBlockBehavior {
    protected final int delay;

    protected AbstractCanSurviveBlockBehavior(CustomBlock customBlock, int delay) {
        super(customBlock);
        this.delay = delay;
    }

    @Override
    public void tick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        if (this.delay == 0) return;
        Object blockState = args[0];
        Object level = args[1];
        Object blockPos = args[2];
        if (!canSurvive(thisBlock, args, () -> true)) {
            BlockStateUtils.getOptionalCustomBlockState(blockState).ifPresent(customState -> {
                if (!customState.isEmpty() && customState.owner().value() == this.customBlock) {
                    net.momirealms.craftengine.core.world.World world = new BukkitWorld(FastNMS.INSTANCE.method$Level$getCraftWorld(level));
                    WorldPosition position = new WorldPosition(world, Vec3d.atCenterOf(LocationUtils.fromBlockPos(blockPos)));
                    world.playBlockSound(position, customState.settings().sounds().breakSound());
                    FastNMS.INSTANCE.method$Level$destroyBlock(level, blockPos, true);
                }
            });
        }
    }

    @Override
    public boolean canSurvive(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object state = args[0];
        Object world = args[1];
        Object pos = args[2];
        return canSurvive(thisBlock, state, world, pos);
    }

    @Override
    public void onPlace(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object world = args[1];
        Object blockPos = args[2];
        FastNMS.INSTANCE.method$LevelAccessor$scheduleBlockTick(world, blockPos, thisBlock, 2);
    }

    @Override
    public Object updateShape(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object level = args[updateShape$level];
        Object blockPos = args[updateShape$blockPos];
        Object state = args[0];
        Optional<ImmutableBlockState> optionalCustomState = BlockStateUtils.getOptionalCustomBlockState(state);
        if (optionalCustomState.isEmpty()) {
            return state;
        }
        if (this.delay != 0) {
            FastNMS.INSTANCE.method$LevelAccessor$scheduleBlockTick(level, blockPos, thisBlock, this.delay);
            return state;
        }
        if (!canSurvive(thisBlock, new Object[] {state, level, blockPos}, () -> true)) {
            BlockPos pos = LocationUtils.fromBlockPos(blockPos);
            ImmutableBlockState customState = optionalCustomState.get();
            net.momirealms.craftengine.core.world.World world = new BukkitWorld(FastNMS.INSTANCE.method$Level$getCraftWorld(level));
            WorldPosition position = new WorldPosition(world, Vec3d.atCenterOf(pos));
            world.playBlockSound(position, customState.settings().sounds().breakSound());
            FastNMS.INSTANCE.method$LevelAccessor$levelEvent(level, WorldEvents.BLOCK_BREAK_EFFECT, blockPos, customState.customBlockState().registryId());
            return MBlocks.AIR$defaultState;
        }
        return state;
    }

    protected abstract boolean canSurvive(Object thisBlock, Object state, Object world, Object blockPos) throws Exception;
}
