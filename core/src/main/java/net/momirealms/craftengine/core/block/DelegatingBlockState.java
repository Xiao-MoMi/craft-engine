package net.momirealms.craftengine.core.block;

import org.jetbrains.annotations.Nullable;

/**
 * Interface representing a block state that can delegate its underlying immutable state.
 *
 * @see ImmutableBlockState The immutable state container being delegated
 */
public interface DelegatingBlockState {

    /**
     * Gets the current immutable block state being delegated.
     *
     * @return The current immutable block state instance
     */
    @Nullable
    ImmutableBlockState blockState();

    /**
     * Replaces the currently delegated block state with a new immutable state.
     *
     * @param state The new immutable state to delegate to
     */
    void setBlockState(@Nullable ImmutableBlockState state);
}
