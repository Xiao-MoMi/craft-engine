package net.momirealms.craftengine.core.world;

import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.CustomBlock;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.StatePropertyAccessor;
import net.momirealms.craftengine.core.item.context.BlockPlaceContext;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ExistingBlock {

    default boolean canBeReplaced(BlockPlaceContext blockPlaceContext) {
        return false;
    }

    default boolean isWaterSource(BlockPlaceContext blockPlaceContext) {
        return false;
    }

    @Nullable
    CustomBlock customBlock();

    boolean isCustom();

    @Nullable
    ImmutableBlockState customBlockState();

    @NotNull
    BlockStateWrapper blockState();

    @NotNull
    StatePropertyAccessor createStatePropertyAccessor();

    default WorldPosition position() {
        return new WorldPosition(world(), x(), y(), z());
    }

    World world();

    Key id();

    int x();

    int y();

    int z();
}
