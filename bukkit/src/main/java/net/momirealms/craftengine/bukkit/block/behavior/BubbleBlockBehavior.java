package net.momirealms.craftengine.bukkit.block.behavior;

import java.util.Map;
import java.util.concurrent.Callable;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;

public class BubbleBlockBehavior extends BukkitBlockBehavior {
    public static final Factory FACTORY = new Factory();
    
    public boolean drag; // false for upward, true for downward
    public boolean direction; // true for upward, false for downward
    public LazyReference<BlockStateWrapper> blockState;
    public int limit;

    public BubbleBlockBehavior(CustomBlock customBlock, boolean drag, boolean direction, int limit) {
            super(customBlock);
            this.drag = drag;
            this.direction = direction;
            this.limit = limit;
            if (drag) {
                this.blockState = LazyReference.lazyReference(() -> CraftEngine.instance().blockManager().createPackedBlockState("minecraft:bubble_column[drag=true]"));
            } else {
                this.blockState = LazyReference.lazyReference(() -> CraftEngine.instance().blockManager().createPackedBlockState("minecraft:bubble_column[drag=false]"));
            }
    }

    @Override
    public void onPlace(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        Object world = args[1];
        Object blockPos = args[2];
        FastNMS.INSTANCE.method$ScheduledTickAccess$scheduleBlockTick(world, blockPos, thisBlock, 20);
    }

    @Override
    public void tick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        World level = (World) args[1];
        BlockPos blockPos = LocationUtils.fromBlockPos(args[2]);
        if (direction) {
                BlockPos current = blockPos.above();
                for (int i = 0; i < limit; i++) {
                        current = (BlockPos) LocationUtils.above(current);
                        if (FastNMS.INSTANCE.method$BlockGetter$getFluidState(level, blockPos) == MFluids.WATER) {
                                level.setBlockAt(current.x(), current.y(), current.z(), this.blockState.get(), 3);
                        } else {
                                break;
                        }
                }
        } else {
                BlockPos current = (BlockPos) LocationUtils.below(blockPos);
                for (int i = 0; i < limit; i++) {
                        current = (BlockPos) LocationUtils.below(current);
                        if (FastNMS.INSTANCE.method$BlockGetter$getFluidState(level, blockPos) == MFluids.WATER) {
                                level.setBlockAt(current.x(), current.y(), current.z(), this.blockState.get(), 3);
                        } else {
                                break;
                        }
                }
        }
    }

    @Override
    public void affectNeighborsAfterRemoval(Object thisBlock, Object[] args, Callable<Object> superMethod) {
        this.updateNeighbours(args[1], args[2], thisBlock);
    }

    @Override
    public void onRemove(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object state = args[0];
        Object level = args[1];
        Object pos = args[2];
        Object newState = args[3];
        if (!FastNMS.INSTANCE.method$BlockStateBase$is(state, FastNMS.INSTANCE.method$BlockState$getBlock(newState))) {
            this.updateNeighbours(level, pos, thisBlock);
            superMethod.call();
        }
    }
    
    private void updateNeighbours(Object level, Object pos, Object thisBlock) {
        World level = (World) args[1];
        BlockPos blockPos = (BlockPos) args[2];
        if (direction) {
                BlockPos current = blockPos.above();
                for (int i = 0; i < limit; i++) {
                        current = (BlockPos) LocationUtils.above(current);
                        if (FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, current) != MBlocks.BUBBLE_COLUMN) break;
                        FastNMS.INSTANCE.method$LevelWriter$setBlock(level, current, MFluids.WATER, 3);
                }
        } else {
                BlockPos current = (BlockPos) LocationUtils.below(blockPos);
                for (int i = 0; i < limit; i++) {
                        current = (BlockPos) LocationUtils.below(current);
                        if (FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, current) != MBlocks.BUBBLE_COLUMN) break;
                        FastNMS.INSTANCE.method$LevelWriter$setBlock(level, current, MFluids.WATER, 3);
                }
        }
    }

    private static class Factory implements BlockBehaviorFactory {
        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
                boolean direction = arguments.getOrDefault("direction", "down").toString().equalsIgnoreCase("down");
                boolean drag = arguments.getOrDefault("drag", "up").toString().equalsIgnoreCase("up");
                int limit = (int) Integer.valueOf(arguments.getOrDefault("limit", 100).toString());
                return new BubbleBlockBehavior(block, drag, direction, limit);
        }
    }
}
