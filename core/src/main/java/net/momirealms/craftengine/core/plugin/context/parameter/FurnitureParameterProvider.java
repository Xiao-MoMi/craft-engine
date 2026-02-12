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
        CONTEXT_FUNCTIONS.put(DirectContextParameters.ID, f -> f.config().id());
        CONTEXT_FUNCTIONS.put(DirectContextParameters.UUID, Furniture::uuid);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.VARIANT, f -> f.currentVariant().name());
        CONTEXT_FUNCTIONS.put(DirectContextParameters.X, furniture -> furniture.position().x());
        CONTEXT_FUNCTIONS.put(DirectContextParameters.Y, furniture -> furniture.position().y());
        CONTEXT_FUNCTIONS.put(DirectContextParameters.Z, furniture -> furniture.position().z());
        CONTEXT_FUNCTIONS.put(DirectContextParameters.PITCH, furniture -> furniture.position().xRot());
        CONTEXT_FUNCTIONS.put(DirectContextParameters.YAW, furniture -> furniture.position().yRot());
        CONTEXT_FUNCTIONS.put(DirectContextParameters.POSITION, Furniture::position);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter, Furniture world) {
        return (Optional<T>) Optional.ofNullable(CONTEXT_FUNCTIONS.get(parameter)).map(f -> f.apply(world));
    }
}