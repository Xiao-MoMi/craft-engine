package net.momirealms.craftengine.bukkit.block.behavior;

import net.momirealms.craftengine.core.block.ImmutableBlockState;
import org.jetbrains.annotations.Nullable;

public interface LiquidSolidifiableBlock {

    boolean canSolidifyWith(Object fluidState);

    @Nullable
    ImmutableBlockState solidifiedState(ImmutableBlockState sourceState);
}
