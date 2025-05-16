package net.momirealms.craftengine.core.plugin.context.parameter;

import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.plugin.context.ChainParameterProvider;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.util.MCUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class EntityParameterProvider implements ChainParameterProvider<Entity> {
    private static final Map<ContextKey<?>, Function<Entity, Object>> CONTEXT_FUNCTIONS = new HashMap<>();
    static {
        CONTEXT_FUNCTIONS.put(DirectContextParameters.X, Entity::x);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.Y, Entity::y);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.Z, Entity::z);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.POSITION, Entity::position);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_X, p -> MCUtils.fastFloor(p.x()));
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_Y, p -> MCUtils.fastFloor(p.y()));
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_Z, p -> MCUtils.fastFloor(p.z()));
        CONTEXT_FUNCTIONS.put(DirectContextParameters.NAME, Entity::name);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.UUID, Entity::uuid);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.WORLD, Entity::world);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter, Entity entity) {
        return (Optional<T>) Optional.ofNullable(CONTEXT_FUNCTIONS.get(parameter)).map(f -> f.apply(entity));
    }
}