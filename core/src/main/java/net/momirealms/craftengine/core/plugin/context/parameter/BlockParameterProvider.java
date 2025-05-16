package net.momirealms.craftengine.core.plugin.context.parameter;

import net.momirealms.craftengine.core.plugin.context.ChainParameterProvider;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.world.BlockInWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class BlockParameterProvider implements ChainParameterProvider<BlockInWorld> {
    private static final Map<ContextKey<?>, Function<BlockInWorld, Object>> CONTEXT_FUNCTIONS = new HashMap<>();
    static {
        CONTEXT_FUNCTIONS.put(DirectContextParameters.X, BlockInWorld::x);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.Y, BlockInWorld::y);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.Z, BlockInWorld::z);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_X, BlockInWorld::x);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_Y, BlockInWorld::y);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_Z, BlockInWorld::z);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.CUSTOM_BLOCK, BlockInWorld::customBlock);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_STATE, BlockInWorld::customBlockState);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.WORLD, BlockInWorld::world);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.POSITION, BlockInWorld::position);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter, BlockInWorld block) {
        return (Optional<T>) Optional.ofNullable(CONTEXT_FUNCTIONS.get(parameter)).map(f -> f.apply(block));
    }
}