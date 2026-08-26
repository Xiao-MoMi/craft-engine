package net.momirealms.craftengine.core.plugin.context.parameter;

import net.momirealms.craftengine.core.entity.Entity;
import net.momirealms.craftengine.core.entity.LivingEntity;
import net.momirealms.craftengine.core.entity.item.ItemEntity;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.ChainParameterProvider;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.util.MiscUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class EntityParameterProvider implements ChainParameterProvider<Entity> {
    public static final EntityParameterProvider INSTANCE = new EntityParameterProvider();
    private static final Map<ContextKey<?>, Function<Entity, Object>> CONTEXT_FUNCTIONS = new HashMap<>();
    static {
        CONTEXT_FUNCTIONS.put(DirectContextParameters.PLAYER, e -> {
            if (e instanceof Player player) {
                return player;
            }
            return null;
        });
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
        CONTEXT_FUNCTIONS.put(DirectContextParameters.FIRE_TICKS, Entity::fireTicks);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.SPEED, Entity::speed);
        CONTEXT_FUNCTIONS.put(DirectContextParameters.HEALTH, e -> {
            if (e instanceof LivingEntity living) {
                return living.health();
            }
            return 0d;
        });
        CONTEXT_FUNCTIONS.put(DirectContextParameters.MAX_HEALTH, e -> {
            if (e instanceof LivingEntity living) {
                return living.maxHealth();
            }
            return 0d;
        });
        CONTEXT_FUNCTIONS.put(DirectContextParameters.ATTR, e -> {
            if (e instanceof LivingEntity living) {
                return new EntityAttributeParameterSource(living);
            }
            return null;
        });
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
