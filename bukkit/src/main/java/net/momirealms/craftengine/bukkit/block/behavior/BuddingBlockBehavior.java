package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.KeyUtils;
import net.momirealms.craftengine.bukkit.util.RegistryUtils;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.RandomTickBlock;
import net.momirealms.craftengine.core.block.property.BooleanProperty;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.ConfigValue;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.core.DirectionProxy;
import net.momirealms.craftengine.proxy.minecraft.core.registries.BuiltInRegistriesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlockProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.BlocksProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.StateHolderProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.properties.BlockStatePropertiesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidStateProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.material.FluidsProxy;

import java.util.List;

public final class BuddingBlockBehavior extends BukkitBlockBehavior implements RandomTickBlock {
    public static final BlockBehaviorFactory<BuddingBlockBehavior> FACTORY = new Factory();
    public final float growthChance;
    public final List<Key> blocks;

    private BuddingBlockBehavior(BlockDefinition blockDefinition,
                                 float growthChance,
                                 List<Key> blocks) {
        super(blockDefinition);
        this.growthChance = growthChance;
        this.blocks = List.copyOf(blocks);
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args) {
        if (RandomUtils.generateRandomFloat(0, 1) >= this.growthChance) return;
        Object nmsDirection = DirectionProxy.VALUES[RandomUtils.generateRandomInt(0, 6)];
        Direction direction = DirectionUtils.fromNMSDirection(nmsDirection);
        Object blockPos = BlockPosProxy.INSTANCE.relative(args[2], nmsDirection);
        Object blockState = BlockGetterProxy.INSTANCE.getBlockState(args[1], blockPos);
        if (canClusterGrowAtState(blockState)) {
            Key blockId = this.blocks.getFirst();
            BlockDefinition firstBlock = BukkitBlockManager.instance().blockById(blockId).orElse(null);
            placeWithPropertyBlock(firstBlock, blockId, direction, nmsDirection, args[1], blockPos, blockState);
        } else {
            Key blockId = BlockStateUtils.getOptionalCustomBlockState(blockState)
                    .map(it -> it.owner().value().id())
                    .orElseGet(() -> BlockStateUtils.getBlockOwnerIdFromState(blockState));
            int blockIdIndex = this.blocks.indexOf(blockId);
            if (blockIdIndex < 0 || blockIdIndex == this.blocks.size() - 1) return;
            Key nextBlockId = this.blocks.get(blockIdIndex + 1);
            BlockDefinition nextBlock = BukkitBlockManager.instance().blockById(nextBlockId).orElse(null);
            placeWithPropertyBlock(nextBlock, nextBlockId, direction, nmsDirection, args[1], blockPos, blockState);
        }
    }

    @Override
    public boolean canRandomlyTick(ImmutableBlockState state) {
        return true;
    }

    @SuppressWarnings("unchecked")
    private void placeWithPropertyBlock(BlockDefinition blockDefinition, Key blockId, Direction direction, Object nmsDirection, Object level, Object blockPos, Object blockState) {
        if (blockDefinition != null) {
            ImmutableBlockState newState = blockDefinition.defaultState();
            Property<Direction> facing = (Property<Direction>) blockDefinition.getProperty("facing");
            if (facing != null) {
                newState = newState.with(facing, direction);
            }
            BooleanProperty waterlogged = (BooleanProperty) blockDefinition.getProperty("waterlogged");
            if (waterlogged != null) {
                newState = newState.with(waterlogged, FluidStateProxy.INSTANCE.getType(BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getFluidState(blockState)) == FluidsProxy.WATER);
            }
            LevelWriterProxy.INSTANCE.setBlock(level, blockPos, newState.customBlockState().minecraftState(), UpdateFlags.UPDATE_ALL);
        } else {
            Object block = RegistryUtils.getRegistryValue(BuiltInRegistriesProxy.BLOCK, KeyUtils.toIdentifier(blockId));
            if (block == null) return;
            Object newState = BlockProxy.INSTANCE.getDefaultBlockState(block);
            newState = StateHolderProxy.INSTANCE.trySetValue(newState, BlockStatePropertiesProxy.WATERLOGGED, FluidStateProxy.INSTANCE.getType(BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getFluidState(blockState)) == FluidsProxy.WATER);
            newState = StateHolderProxy.INSTANCE.trySetValue(newState, BlockStatePropertiesProxy.FACING, (Comparable<?>) nmsDirection);
            LevelWriterProxy.INSTANCE.setBlock(level, blockPos, newState, UpdateFlags.UPDATE_ALL);
        }
    }

    public static boolean canClusterGrowAtState(Object state) {
        return BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.isAir(state)
                || BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$0(state, BlocksProxy.WATER)
                && FluidStateProxy.INSTANCE.getAmount(BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getFluidState(state)) == 8;
    }

    private static class Factory implements BlockBehaviorFactory<BuddingBlockBehavior> {
        private static final String[] GROWTH_CHANCE = new String[] {"growth_chance", "growth-chance"};

        @Override
        public BuddingBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new BuddingBlockBehavior(
                    block,
                    section.getFloat(GROWTH_CHANCE, 0.2f),
                    section.getList("blocks", ConfigValue::getAsIdentifier)
            );
        }
    }
}
