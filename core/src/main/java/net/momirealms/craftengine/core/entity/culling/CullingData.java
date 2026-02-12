package net.momirealms.craftengine.core.entity.culling;

import net.momirealms.craftengine.core.world.collision.AABB;

public final class CullingData {
    public final AABB aabb;
    public final int maxDistance;
    public final double aabbExpansion;
    public final boolean rayTracing;

    public CullingData(AABB aabb, int maxDistance, double aabbExpansion, boolean rayTracing) {
        this.aabb = aabb;
        this.maxDistance = maxDistance;
        this.aabbExpansion = aabbExpansion;
        this.rayTracing = rayTracing;
    }

    public AABB aabb() {
        return this.aabb;
    }

    public int maxDistance() {
        return this.maxDistance;
    }

    public double aabbExpansion() {
        return this.aabbExpansion;
    }

    public boolean rayTracing() {
        return this.rayTracing;
    }
}
