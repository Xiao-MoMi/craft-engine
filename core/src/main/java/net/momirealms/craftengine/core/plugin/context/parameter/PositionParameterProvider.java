package net.momirealms.craftengine.core.plugin.context.parameter;

import net.momirealms.craftengine.core.plugin.context.ChainParameterProvider;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class PositionParameterProvider implements ChainParameterProvider<WorldPosition> {
    private static final Map<ContextKey<?>, Function<WorldPosition, Object>> CONTEXT_FUNCTIONS = new HashMap<>();
    static {
        CONTEXT_FUNCTIONS.put(DirectContextParameters.WORLD, WorldPosition::world);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.COORDINATE, p -> p);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.X, WorldPosition::x);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.Y, WorldPosition::y);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.Z, WorldPosition::z);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.YAW, WorldPosition::xRot);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.PITCH, WorldPosition::yRot);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_X, p -> MiscUtils.floor(p.x()));
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_Y, p -> MiscUtils.floor(p.y()));
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_Z, p -> MiscUtils.floor(p.z()));
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter, WorldPosition position) {
        return (Optional<T>) Optional.ofNullable(CONTEXT_FUNCTIONS.get(parameter)).map(f -> f.apply(position));
    }
}