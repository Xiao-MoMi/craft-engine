package net.momirealms.craftengine.core.entity;

import net.momirealms.craftengine.core.entity.data.EntityData;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.context.ChainParameterSource;
import net.momirealms.craftengine.core.plugin.context.ContextKey;
import net.momirealms.craftengine.core.plugin.context.parameter.EntityParameterProvider;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface Entity extends ChainParameterSource {
    Key type();

    boolean isValid();

    double x();

    double y();

    double z();

    default WorldPosition position() {
        return new WorldPosition(world(), x(), y(), z(), xRot(), yRot());
    }

    float xRot();

    float yRot();

    int entityId();

    World world();

    Direction getDirection();

    Object platformEntity();

    Object minecraftEntity();

    String name();

    UUID uuid();

    Object entityData();

    <T> T getEntityData(EntityData<T> entityData);

    default <T> void setEntityData(EntityData<T> data, T value) {
        setEntityData(data, value, false);
    }

    <T> void setEntityData(EntityData<T> data, T value, boolean force);

    void remove();

    Set<Player> getTrackedBy();

    WorldPosition eyePosition();

    void teleport(WorldPosition worldPosition);

    @Override
    default <T> Optional<T> getParameter(ContextKey<T> key) {
        return EntityParameterProvider.INSTANCE.getOptionalParameter(key, this);
    }
}
