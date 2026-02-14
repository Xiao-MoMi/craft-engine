package net.momirealms.craftengine.core.plugin.entityculling;

import net.momirealms.craftengine.core.world.MutableVec3d;

/**
 * Pre-allocated vector pool for ray tracing calculations.
 * This class eliminates GC pressure by reusing vector objects
 * instead of creating new instances during hot path operations.
 * 
 */
public final class ReusableVectorPool {
    
    // Ray intersection calculations
    private final MutableVec3d inverseRayDirection = new MutableVec3d(0, 0, 0);
    private final MutableVec3d normalizedDirection = new MutableVec3d(0, 0, 0);
    
    // AABB calculations
    private final MutableVec3d tMin = new MutableVec3d(0, 0, 0);
    private final MutableVec3d tMax = new MutableVec3d(0, 0, 0);
    
    /**
     * Gets the pre-allocated vector for storing inverse ray direction.
     * @return the reusable inverse ray direction vector
     */
    public MutableVec3d getInverseRayDirection() {
        return inverseRayDirection;
    }
    
    /**
     * Gets the pre-allocated vector for storing normalized direction.
     * @return the reusable normalized direction vector
     */
    public MutableVec3d getNormalizedDirection() {
        return normalizedDirection;
    }
    
    /**
     * Gets the pre-allocated vector for storing minimum t values.
     * @return the reusable tMin vector
     */
    public MutableVec3d getTMin() {
        return tMin;
    }
    
    /**
     * Gets the pre-allocated vector for storing maximum t values.
     * @return the reusable tMax vector
     */
    public MutableVec3d getTMax() {
        return tMax;
    }
}
