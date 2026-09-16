package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.world.collision.AABB;

import java.util.Objects;

public final class ColliderConfig {
    public final AABB bounds;
    public final ColliderProperties properties;

    public ColliderConfig(AABB bounds, ColliderProperties properties) {
        this.bounds = bounds;
        this.properties = properties;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof ColliderConfig config
                && Objects.equals(this.bounds, config.bounds)
                && this.properties == config.properties;
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hashCode(this.bounds) + Objects.hashCode(this.properties);
    }
}
