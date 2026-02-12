package net.momirealms.craftengine.bukkit.block.behavior;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.*;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.BooleanProperty;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.util.random.RandomUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class BuddingBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<BuddingBlockBehavior> FACTORY = new Factory();
    private final float growthChance;
    private final List<Key> blocks;

    public BuddingBlockBehavior(CustomBlock customBlock, float growthChance, List<Key> blocks) {
        super(customBlock);
        this.growthChance = growthChance;
        this.blocks = blocks;
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        if (RandomUtils.generateRandomFloat(0, 1) >= growthChance) return;
        Object nmsDirection = CoreReflections.instance$Direction$values[RandomUtils.generateRandomInt(0, 6)];
        Direction direction = DirectionUtils.fromNMSDirection(nmsDirection);
        Object blockPos = FastNMS.INSTANCE.method$BlockPos$relative(args[2], nmsDirection);
        Object blockState = FastNMS.INSTANCE.method$BlockGetter$getBlockState(args[1], blockPos);
        if (canClusterGrowAtState(blockState)) {
            Key blockId = blocks.getFirst();
            CustomBlock firstBlock = BukkitBlockManager.instance().blockById(blockId).orElse(null);
            placeWithPropertyBlock(firstBlock, blockId, direction, nmsDirection, args[1], blockPos, blockState);
        } else {
            Key blockId = BlockStateUtils.getOptionalCustomBlockState(blockState)
                    .map(it -> it.owner().value().id())
                    .orElseGet(() -> BlockStateUtils.getBlockOwnerIdFromState(blockState));
            int blockIdIndex = blocks.indexOf(blockId);
            if (blockIdIndex < 0 || blockIdIndex == blocks.size() - 1) return;
            Key nextBlockId = blocks.get(blockIdIndex + 1);
            CustomBlock nextBlock = BukkitBlockManager.instance().blockById(nextBlockId).orElse(null);
            placeWithPropertyBlock(nextBlock, nextBlockId, direction, nmsDirection, args[1], blockPos, blockState);
        }
    }

    @SuppressWarnings("unchecked")
    private void placeWithPropertyBlock(CustomBlock customBlock, Key blockId, Direction direction, Object nmsDirection, Object level, Object blockPos, Object blockState) {
        if (customBlock != null) {
            ImmutableBlockState newState = customBlock.defaultState();
            Property<?> facing = customBlock.getProperty("facing");
            if (facing != null) {
                if (facing.valueClass() == Direction.class) {
                    newState = newState.with((Property<Direction>) facing, direction);
                } else if (facing.valueClass() == HorizontalDirection.class) {
                    if (!direction.axis().isHorizontal()) return;
                    newState = newState.with((Property<HorizontalDirection>) facing, direction.toHorizontalDirection());
                }
            }
            BooleanProperty waterlogged = (BooleanProperty) customBlock.getProperty("waterlogged");
            if (waterlogged != null) {
                newState = newState.with(waterlogged, FastNMS.INSTANCE.method$FluidState$getType(FastNMS.INSTANCE.field$BlockBehaviour$BlockStateBase$fluidState(blockState)) == MFluids.WATER);
            }
            FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, newState.customBlockState().literalObject(), 3);
        } else if (blockId.namespace().equals("minecraft")) {
            Object block = FastNMS.INSTANCE.method$Registry$getValue(MBuiltInRegistries.BLOCK, FastNMS.INSTANCE.method$ResourceLocation$fromNamespaceAndPath("minecraft", blockId.value()));
            if (block == null) return;
            Object newState = FastNMS.INSTANCE.method$Block$defaultState(block);
            newState = FastNMS.INSTANCE.method$StateHolder$trySetValue(newState, MBlockStateProperties.WATERLOGGED, FastNMS.INSTANCE.method$FluidState$getType(FastNMS.INSTANCE.field$BlockBehaviour$BlockStateBase$fluidState(blockState)) == MFluids.WATER);
            newState = FastNMS.INSTANCE.method$StateHolder$trySetValue(newState, MBlockStateProperties.FACING, (Comparable<?>) nmsDirection);
            FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, newState, 3);
        }
    }

    public static boolean canClusterGrowAtState(Object state) {
        return FastNMS.INSTANCE.method$BlockStateBase$isAir(state)
                || FastNMS.INSTANCE.method$BlockStateBase$isBlock(state, MBlocks.WATER)
                && FastNMS.INSTANCE.field$FluidState$amount(FastNMS.INSTANCE.field$BlockBehaviour$BlockStateBase$fluidState(state)) == 8;
    }

    private static class Factory implements BlockBehaviorFactory<BuddingBlockBehavior> {

        @Override
        public BuddingBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            float growthChance = ResourceConfigUtils.getAsFloat(arguments.getOrDefault("growth-chance", 0.2), "growth-chance");
            List<Key> blocks = new ObjectArrayList<>();
            MiscUtils.getAsStringList(arguments.get("blocks")).forEach(s -> blocks.add(Key.of(s)));
            return new BuddingBlockBehavior(block, growthChance, blocks);
        }
    }
}
