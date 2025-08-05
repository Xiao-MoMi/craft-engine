package net.momirealms.craftengine.bukkit.block.behavior;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.PointedDripstone;
import org.bukkit.event.block.BlockFormEvent;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.bukkit.CraftBukkitReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.core.block.BlockBehavior;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.EmptyBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.RandomUtils;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.World;

public class TearingBlockSpawn extends BukkitBlockBehavior {
    public static final Factory FACTORY = new Factory();
    private static final List<Object> WATER = List.of(MFluids.WATER, MFluids.FLOWING_WATER);
    private static final List<Object> LAVA = List.of(MFluids.LAVA, MFluids.FLOWING_LAVA);

    private final boolean water; // true == water , false == lava
    private final Key toPlace;
    private final float chance;
    private final int heightLimit;
    private Object defaultBlockState;
    private ImmutableBlockState defaultImmutableBlockState;

    public TearingBlockSpawn(CustomBlock customBlock, Key toPlace, Boolean water, float chance, int heightLimit) {
        super(customBlock);
        this.water = water;
        this.toPlace = toPlace;
        this.chance = chance;
        this.heightLimit = heightLimit;
    }

    public Object getDefaultBlockState() {
        if (this.defaultBlockState != null) {
            return this.defaultBlockState;
        }
        Optional<CustomBlock> optionalCustomBlock = BukkitBlockManager.instance().blockById(this.toPlace);
        if (optionalCustomBlock.isPresent()) {
            CustomBlock customBlock = optionalCustomBlock.get();
            this.defaultBlockState = customBlock.defaultState().customBlockState().handle();
            this.defaultImmutableBlockState = customBlock.defaultState();
        } else {
            CraftEngine.instance().logger().warn("Failed to create solid block " + this.toPlace + " in ConcretePowderBlockBehavior");
            this.defaultBlockState = MBlocks.STONE$defaultState;
            this.defaultImmutableBlockState = EmptyBlock.STATE;
        }
        return this.defaultBlockState;
    }

    public ImmutableBlockState defaultImmutableBlockState() {
        if (this.defaultImmutableBlockState == null) {
            this.getDefaultBlockState();
        }
        return this.defaultImmutableBlockState;
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
        if (!(blockData1 instanceof PointedDripstone pDripstone)) return false;
        if (pDripstone.getVerticalDirection() != BlockFace.DOWN) return false;
        BlockPos highest = findHighestConnectedDripstone(pos, level);
        if (highest == null) return false;
        BlockPos fluidPos = highest.above().above();
        return (water && WATER.contains(FastNMS.INSTANCE.method$BlockGetter$getFluidState(level, fluidPos)) || !water && LAVA.contains(FastNMS.INSTANCE.method$BlockGetter$getFluidState(level, fluidPos)));
    }

    public BlockPos findHighestConnectedDripstone(BlockPos start, World world) {
        BlockPos pos = start;
        while (true) {
            BlockPos above = pos.above();
            BlockData blockData = BlockStateUtils.fromBlockData(FastNMS.INSTANCE.method$BlockGetter$getBlockState(world, above));
            if (blockData instanceof PointedDripstone pDripstone && pDripstone.getVerticalDirection() == BlockFace.DOWN) {
                pos = above;
            } else {
                break;
            }
        }
        return pos;
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        World level = (World) args[0];
        BlockPos pos = (BlockPos) args[1];
        if (FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, pos.above()) != MBlocks.AIR$defaultState) return;
        if (RandomUtils.generateRandomFloat(0, 1) > chance) return;
        BlockPos targetPos = pos.above();
        BlockPos tearing = getTearingDripstone(level, targetPos);
        if (tearing == null) return;
        if (!canTear(level, tearing)) return;
        placeBlock(level, targetPos);
    }

    public void placeBlock(World level, BlockPos pos) {
        BlockState craftBlockState;
        try {
            craftBlockState = (BlockState) CraftBukkitReflections.method$CraftBlockStates$getBlockState.invoke(null, level, pos);
            BlockData blockData = BlockStateUtils.fromBlockData(getDefaultBlockState());
            BlockState state = blockData.createBlockState();
            BlockFormEvent event = new BlockFormEvent(craftBlockState.getBlock(), state);
            if(!event.callEvent()) return;
            level.setBlockAt(pos.x(), pos.y(), pos.z(), defaultImmutableBlockState.customBlockState(),3);

        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to update state for placement " + pos, e);
        }
    }

    public static class Factory implements BlockBehaviorFactory {
        @Override
        public BlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            Boolean water = arguments.getOrDefault("liquid", "water").toString().equalsIgnoreCase("water");
            Key toPlace = Key.from((String) arguments.getOrDefault("toPlace", "dripstone"));
            float chance = Float.parseFloat(arguments.getOrDefault("chance", 0.011377778F).toString());
            int heightLimit = Integer.parseInt(arguments.getOrDefault("heightLimit", 12).toString());
            return new TearingBlockSpawn(block, toPlace, water, chance, heightLimit);
        }
    }
}
