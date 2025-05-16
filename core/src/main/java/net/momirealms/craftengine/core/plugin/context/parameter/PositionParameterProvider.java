package net.momirealms.craftengine.core.plugin.context.parameter;

import net.momirealms.craftengine.core.plugin.context.ChainParameterProvider;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.util.MCUtils;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class PositionParameterProvider implements ChainParameterProvider<WorldPosition> {
    private static final Map<ContextKey<?>, Function<WorldPosition, Object>> CONTEXT_FUNCTIONS = new HashMap<>();
    static {
        CONTEXT_FUNCTIONS.put(DirectContextParameters.WORLD, WorldPosition::world);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.COORDINATE, p -> p);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.X, WorldPosition::x);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.Y, WorldPosition::y);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.Z, WorldPosition::z);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_X, p -> MCUtils.fastFloor(p.x()));
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_Y, p -> MCUtils.fastFloor(p.y()));
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_Z, p -> MCUtils.fastFloor(p.z()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter, WorldPosition position) {
        return (Optional<T>) Optional.ofNullable(CONTEXT_FUNCTIONS.get(parameter)).map(f -> f.apply(position));
    }
}