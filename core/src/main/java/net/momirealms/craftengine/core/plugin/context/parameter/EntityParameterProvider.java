package net.momirealms.craftengine.core.plugin.context.parameter;

import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.entity.item.ItemEntity;
import net.momirealms.craftengine.core.plugin.context.ChainParameterProvider;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class EntityParameterProvider implements ChainParameterProvider<Entity> {
    private static final Map<ContextKey<?>, Function<Entity, Object>> CONTEXT_FUNCTIONS = new HashMap<>();
    static {
        CONTEXT_FUNCTIONS.put(DirectContextParameters.X, Entity::x);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.Y, Entity::y);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.Z, Entity::z);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.YAW, Entity::xRot);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.PITCH, Entity::yRot);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.POSITION, Entity::position);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_X, p -> MiscUtils.floor(p.x()));
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_Y, p -> MiscUtils.floor(p.y()));
        CONTEXT_FUNCTIONS.put(DirectContextParameters.BLOCK_Z, p -> MiscUtils.floor(p.z()));
        CONTEXT_FUNCTIONS.put(DirectContextParameters.NAME, Entity::name);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.UUID, Entity::uuid);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.WORLD, Entity::world);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.ITEM, e -> {
            if (e instanceof ItemEntity itemEntity) {
                return itemEntity.getItem();
            }
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Optional<T> getOptionalParameter(ContextKey<T> parameter, Entity entity) {
        return (Optional<T>) Optional.ofNullable(CONTEXT_FUNCTIONS.get(parameter)).map(f -> f.apply(entity));
    }
}