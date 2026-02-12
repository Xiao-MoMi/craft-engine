package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.bukkit.block.BukkitBlockManager;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.UpdateOption;
import net.momirealms.craftengine.core.block.behavior.BlockBehaviorFactory;
import net.momirealms.craftengine.core.util.LazyReference;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import net.momirealms.craftengine.core.util.random.RandomUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;

public class SpreadingBlockBehavior extends BukkitBlockBehavior {
    public static final BlockBehaviorFactory<SpreadingBlockBehavior> FACTORY = new Factory();
    private final LazyReference<Object> targetBlock;

    public SpreadingBlockBehavior(CustomBlock customBlock, String targetBlock) {
        super(customBlock);
        this.targetBlock = LazyReference.lazyReference(() -> Objects.requireNonNull(BukkitBlockManager.instance().createBlockState(targetBlock)).literalObject());
    }

    @Override
    public void randomTick(Object thisBlock, Object[] args, Callable<Object> superMethod) throws Exception {
        Object level = args[1];
        Object pos = args[2];
        Object blockPos = FastNMS.INSTANCE.method$BlockPos$offset(pos, RandomUtils.generateRandomInt(-1, 2), RandomUtils.generateRandomInt(-3, 2), RandomUtils.generateRandomInt(-1, 2));
        if (FastNMS.INSTANCE.method$BlockStateBase$isBlock(FastNMS.INSTANCE.method$BlockGetter$getBlockState(level, blockPos), FastNMS.INSTANCE.method$BlockState$getBlock(this.targetBlock.get()))) {
            FastNMS.INSTANCE.method$LevelWriter$setBlock(level, blockPos, this.block().defaultState().customBlockState().literalObject(), UpdateOption.UPDATE_ALL.flags());
        }
    }

    private static class Factory implements BlockBehaviorFactory<SpreadingBlockBehavior> {

        @Override
        public SpreadingBlockBehavior create(CustomBlock block, Map<String, Object> arguments) {
            String targetBlock = ResourceConfigUtils.requireNonEmptyStringOrThrow(arguments.get("target-block"), "warning.config.block.behavior.spreading.missing_target_block");
            return new SpreadingBlockBehavior(block, targetBlock);
        }
    }
}
