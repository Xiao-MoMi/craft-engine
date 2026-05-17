package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.core.block.behavior.*;

public interface CombinedBlockBehavior extends PrioritizedFallOnHandler, PathFindingBlock, BukkitFallableBlock,
        BonemealableBlock, WorldlyContainerHolder, BukkitSimpleWaterloggedBlock, EntityBlock, RandomTickBlock {
}