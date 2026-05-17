package net.momirealms.craftengine.core.plugin.context.parameter;

import net.momirealms.craftengine.core.plugin.context.ChainParameterProvider;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.world.ExistingBlock;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class BlockParameterProvider implements ChainParameterProvider<ExistingBlock> {
    private static final Map<ContextKey<?>, Function<ExistingBlock, Object>> CONTEXT_FUNCTIONS = new HashMap<>();
    static {
        CONTEXT_FUNCTIONS.put(DirectContextParameters.X, ExistingBlock::x);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.Y, ExistingBlock::y);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.Z, ExistingBlock::z);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_X, ExistingBlock::x);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_Y, ExistingBlock::y);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_Z, ExistingBlock::z);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.CUSTOM_BLOCK, ExistingBlock::customBlock);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.CUSTOM_BLOCK_STATE, ExistingBlock::customBlockState);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.WORLD, ExistingBlock::world);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.POSITION, ExistingBlock::position);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter, ExistingBlock block) {
        return (Optional<T>) Optional.ofNullable(CONTEXT_FUNCTIONS.get(parameter)).map(f -> f.apply(block));
    }
}