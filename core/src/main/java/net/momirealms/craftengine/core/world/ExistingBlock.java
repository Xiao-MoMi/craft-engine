package net.momirealms.craftengine.core.world;

import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.BlockStateWrapper;
import net.momirealms.craftengine.core.block.ImmutableBlockState;
import net.momirealms.craftengine.core.block.StatePropertyAccessor;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.context.BlockPlaceContext;
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
    BlockDefinition customBlock();

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

    boolean is(Key tag);

    World world();

    Key id();

    int x();

    int y();

    int z();
}
