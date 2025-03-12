package net.momirealms.craftengine.core.world;

import net.momirealms.craftengine.core.item.context.BlockPlaceContext;

public interface WorldBlock {

    default boolean canBeReplaced(BlockPlaceContext blockPlaceContext) {
        return false;
    }

    default boolean isWaterSource(BlockPlaceContext blockPlaceContext) {
        return false;
    }
}
