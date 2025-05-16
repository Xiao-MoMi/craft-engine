package net.momirealms.craftengine.core.plugin.context.parameter;

import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.plugin.context.ChainParameterProvider;
import net.momirealms.craftengine.core.plugin.context.ContextKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class FurnitureParameterProvider implements ChainParameterProvider<Furniture> {
    private static final Map<ContextKey<?>, Function<Furniture, Object>> CONTEXT_FUNCTIONS = new HashMap<>();
    static {
        CONTEXT_FUNCTIONS.put(DirectContextParameters.ID, Furniture::id);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.UUID, Furniture::uuid);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.ANCHOR_TYPE, Furniture::anchorType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter, Furniture world) {
        return (Optional<T>) Optional.ofNullable(CONTEXT_FUNCTIONS.get(parameter)).map(f -> f.apply(world));
    }
}