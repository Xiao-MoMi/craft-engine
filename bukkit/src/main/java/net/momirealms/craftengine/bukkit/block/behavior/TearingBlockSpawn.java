package net.momirealms.craftengine.bukkit.block.behavior;

import java.util.List;
import java.util.Map;

import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.PointedDripstone;

import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;

public class TearingBlockSpawn extends BukkitBlockBehavior {
    public static final Factory FACTORY = new Factory();
    public boolean water; //true == water , false == lava
    private static final List<Object> WATER = List.of(MFluids.WATER, MFluids.FLOWING_WATER);
    private static final List<Object> LAVA = List.of(MFluids.LAVA, MFluids.FLOWING_LAVA);
    private Key toPlace;

    public int heightLimit = 256;


    public TearingBlockSpawn(CustomBlock customBlock, Key toPlace, Boolean water) {
        super(customBlock);
        this.water = water; // default to water
        this.toPlace = toPlace;
    }

    public BlockPos getTearingDripstone(World level, BlockPos pos) {
        for (int y = pos.y(); y < heightLimit; y++) {
            BlockPos currentPos = new BlockPos(pos.x(), y, pos.z());
            Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, currentPos);
            if (blockState != MBlocks.AIR$defaultState) {
                BlockData blockData = BlockStateUtils.fromBlockData(blockState);
                if (blockData instanceof PointedDripstone pDripstone) {
                    if (pDripstone.getVerticalDirection() == BlockFace.UP) {
                        return currentPos;
                    }
                }
            }
        }
        return null;
    }

    public boolean canTear(World level, BlockPos pos) {
        Object relativeBlockState1 = FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, pos);
        BlockData blockData1 = BlockStateUtils.fromBlockData(relativeBlockState1);
        if (blockData1 instanceof PointedDripstone pDripstone){
            if(pDripstone.getVerticalDirection() == BlockFace.DOWN){
                BlockPos highest = findHighestConnectedDripstone(pos, level);
                if (highest != null) {
                    BlockPos fluidPos = highest.above().above();
                    if(water && WATER.contains(FastNMS.INSTANCE.method$BlockGetter$getFluidState(level, fluidPos))) {
                        return true;
                    } else if (!water && LAVA.contains(FastNMS.INSTANCE.method$BlockGetter$getFluidState(level, fluidPos))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public BlockPos findHighestConnectedDripstone(BlockPos start, World world) {
        BlockPos pos = start;
        while (true) {
            BlockPos above = pos.above();
            BlockData blockData = BlockStateUtils.fromBlockData(
                FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, above)
            );
            if (blockData instanceof PointedDripstone pDripstone && pDripstone.getVerticalDirection() == BlockFace.DOWN) {
                pos = above;
            } else {
                break;
            }
        }
        return pos;
    }
    
    public static class Factory implements BlockBehaviorFactory {
        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Boolean water = arguments.getOrDefault("liquid", "water").toString().equalsIgnoreCase("water");
            Key toPlace = Key.fromString((String) arguments.getOrDefault("toPlace", "dripstone"));
            return new TearingBlockSpawn(block, toPlace, water);
        }
    }
}
