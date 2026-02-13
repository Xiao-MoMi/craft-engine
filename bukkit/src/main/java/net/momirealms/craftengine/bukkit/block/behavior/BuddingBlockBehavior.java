package net.momirealms.craftengine.bukkit.block.behavior;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MBlocks;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MFluids;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.properties.BooleanProperty;
import net.momirealms.craftengine.core.block.properties.Property;
import net.momirealms.craftengine.core.util.*;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.resources.IdentifierProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.StateHolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.properties.BlockStatePropertiesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;

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
        Object nmsDirection = DirectionProxy.VALUES[RandomUtils.generateRandomInt(0, 6)];
        Direction direction = DirectionUtils.fromNMSDirection(nmsDirection);
        Object blockPos = BlockPosProxy.INSTANCE.relative(args[2], nmsDirection);
        Object blockState = BlockGetterProxy.INSTANCE.getBlockState(args[1], blockPos);
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
                newState = newState.with(waterlogged, FluidStateProxy.INSTANCE.getType(BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getFluidState(blockState)) == MFluids.WATER);
            }
            LevelWriterProxy.INSTANCE.setBlock(level, blockPos, newState.customBlockState().literalObject(), 3);
        } else if (blockId.namespace().equals("minecraft")) {
            Object block = RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.BLOCK, IdentifierProxy.INSTANCE.newInstance("minecraft", blockId.value()));
            if (block == null) return;
            Object newState = BlockProxy.INSTANCE.getDefaultBlockState(block);
            newState = StateHolderProxy.INSTANCE.trySetValue(newState, BlockStatePropertiesProxy.WATERLOGGED, FluidStateProxy.INSTANCE.getType(BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getFluidState(blockState)) == MFluids.WATER);
            newState = StateHolderProxy.INSTANCE.trySetValue(newState, BlockStatePropertiesProxy.FACING, (Comparable<?>) nmsDirection);
            LevelWriterProxy.INSTANCE.setBlock(level, blockPos, newState, 3);
        }
    }

    public static boolean canClusterGrowAtState(Object state) {
        return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isAir(state)
                || BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$0(state, MBlocks.WATER)
                && FluidStateProxy.INSTANCE.getAmount(BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getFluidState(state)) == 8;
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
