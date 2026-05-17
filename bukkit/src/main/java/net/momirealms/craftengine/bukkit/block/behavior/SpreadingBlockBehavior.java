package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.UpdateFlags;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.block.behavior.RandomTickBlock;
import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.random.RandomUtils;
import net.momirealms.craftengine.proxy.minecraft.core.BlockPosProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.BlockGetterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.LevelWriterProxy;
import net.momirealms.craftengine.proxy.minecraft.world.level.block.state.BlockBehaviourProxy;

import java.util.Objects;

public final class SpreadingBlockBehavior extends BukkitBlockBehavior implements RandomTickBlock {
    public static final BlockBehaviorFactory<SpreadingBlockBehavior> FACTORY = new Factory();
    public final LazyReference<Object> targetBlock;

    private SpreadingBlockBehavior(BlockDefinition blockDefinition,
                                   String targetBlock) {
        super(blockDefinition);
        this.targetBlock = LazyReference.lazyReference(() -> Objects.requireNonNull(BukkitBlockManager.instance().createBlockState(targetBlock)).minecraftState());
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args) {
        Object level = args[1];
        Object pos = args[2];
        Object blockPos = BlockPosProxy.INSTANCE.offset(pos, RandomUtils.generateRandomInt(-1, 2), RandomUtils.generateRandomInt(-3, 2), RandomUtils.generateRandomInt(-1, 2));
        if (BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.is$0(BlockGetterProxy.INSTANCE.getBlockState(level, blockPos), BlockBehaviourProxy.BlockStateBaseProxy.INSTANCE.getBlock(this.targetBlock.get()))) {
            LevelWriterProxy.INSTANCE.setBlock(level, blockPos, this.block().defaultState().customBlockState().minecraftState(), UpdateFlags.UPDATE_ALL);
        }
    }

    @Override
    public boolean canRandomlyTick(ImmutableBlockState state) {
        return true;
    }

    private static class Factory implements BlockBehaviorFactory<SpreadingBlockBehavior> {
        private static final String[] TARGET_BLOCK = new String[] {"target_block", "target-block"};

        @Override
        public SpreadingBlockBehavior create(BlockDefinition block, ConfigSection section) {
            return new SpreadingBlockBehavior(
                    block,
                    section.getNonNullString(TARGET_BLOCK)
            );
        }
    }
}
