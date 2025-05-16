package net.momirealms.craftengine.core.plugin.context.parameter;

import net.momirealms.craftengine.core.plugin.context.ChainParameterProvider;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class WorldParameterProvider implements ChainParameterProvider<World> {
    private static final Map<ContextKey<?>, Function<World, Object>> CONTEXT_FUNCTIONS = new HashMap<>();
    static {
        CONTEXT_FUNCTIONS.put(DirectContextParameters.NAME, World::name);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.TIME, World::time);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.UUID, World::uuid);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter, World world) {
        return (Optional<T>) Optional.ofNullable(CONTEXT_FUNCTIONS.get(parameter)).map(f -> f.apply(world));
    }
}