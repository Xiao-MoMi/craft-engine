package net.momirealms.craftengine.core.world;

import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.item.context.BlockPlaceContext;
import org.jetbrains.annotations.Nullable;

public interface BlockInWorld {

    default boolean canBeReplaced(BlockPlaceContext blockPlaceContext) {
        return false;
    }

    default boolean isWaterSource(BlockPlaceContext blockPlaceContext) {
        return false;
    }

    @Nullable
    CustomBlock customBlock();

    @Nullable
    ImmutableBlockState customBlockState();

    default WorldPosition position() {
        return new WorldPosition(world(), x(), y(), z());
    }

    World world();

    int x();

    int y();

    int z();
}
