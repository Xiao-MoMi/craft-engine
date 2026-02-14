package net.momirealms.craftengine.core.plugin.entityculling;

import net.jqwik.api.*;
import net.momirealms.craftengine.core.world.MutableVec3d;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for ReusableVectorPool.
 * 
 */
class ReusableVectorPoolPropertyTest {

    /**
     * Property 1: Zero Allocation in Hot Path
     * 
     * For any sequence of N visibility calculations (N > 0), the number of new object 
     * allocations during the isVisible() method execution SHALL be zero.
     * 
     * This test verifies that the pool returns the same pre-allocated instances
     * across multiple accesses, ensuring no new allocations occur.
     * 
     */
    @Property(tries = 100)
    void poolReturnsSameInstancesAcrossMultipleAccesses(
            @ForAll("accessCounts") int accessCount) {
        
        ReusableVectorPool pool = new ReusableVectorPool();
        
        // Store initial references
        MutableVec3d initialInverseRayDirection = pool.getInverseRayDirection();
        MutableVec3d initialNormalizedDirection = pool.getNormalizedDirection();
        MutableVec3d initialTMin = pool.getTMin();
        MutableVec3d initialTMax = pool.getTMax();
        
        // Access the pool multiple times and verify same instances are returned
        for (int i = 0; i < accessCount; i++) {
            assertSame(initialInverseRayDirection, pool.getInverseRayDirection(),
                    "inverseRayDirection should be the same instance on access " + i);
            assertSame(initialNormalizedDirection, pool.getNormalizedDirection(),
                    "normalizedDirection should be the same instance on access " + i);
            assertSame(initialTMin, pool.getTMin(),
                    "tMin should be the same instance on access " + i);
            assertSame(initialTMax, pool.getTMax(),
                    "tMax should be the same instance on access " + i);
        }
    }

    /**
     * Property: Pool vectors can be modified and reused without creating new objects.
     * 
     * For any sequence of modifications to pool vectors, the same instances
     * should be returned, demonstrating zero allocation during hot path operations.
     * 
     */
    @Property(tries = 100)
    void poolVectorsCanBeModifiedAndReused(
            @ForAll("coordinates") double x,
            @ForAll("coordinates") double y,
            @ForAll("coordinates") double z) {
        
        ReusableVectorPool pool = new ReusableVectorPool();
        
        // Get initial references
        MutableVec3d inverseRayDirection = pool.getInverseRayDirection();
        MutableVec3d normalizedDirection = pool.getNormalizedDirection();
        
        // Modify the vectors (simulating hot path usage)
        inverseRayDirection.set(x, y, z);
        normalizedDirection.set(x * 2, y * 2, z * 2);
        
        // Verify same instances are still returned after modification
        assertSame(inverseRayDirection, pool.getInverseRayDirection(),
                "Should return same instance after modification");
        assertSame(normalizedDirection, pool.getNormalizedDirection(),
                "Should return same instance after modification");
        
        // Verify the modifications persisted
        assertEquals(x, pool.getInverseRayDirection().x, 0.0001);
        assertEquals(y, pool.getInverseRayDirection().y, 0.0001);
        assertEquals(z, pool.getInverseRayDirection().z, 0.0001);
    }

    /**
     * Property: All pool vectors are distinct instances.
     * 
     * Each getter should return a different pre-allocated instance to avoid
     * data corruption when multiple vectors are used simultaneously.
     */
    @Property(tries = 100)
    void allPoolVectorsAreDistinctInstances(@ForAll("accessCounts") int ignored) {
        ReusableVectorPool pool = new ReusableVectorPool();
        
        MutableVec3d inverseRayDirection = pool.getInverseRayDirection();
        MutableVec3d normalizedDirection = pool.getNormalizedDirection();
        MutableVec3d tMin = pool.getTMin();
        MutableVec3d tMax = pool.getTMax();
        
        // All vectors should be distinct instances
        assertNotSame(inverseRayDirection, normalizedDirection);
        assertNotSame(inverseRayDirection, tMin);
        assertNotSame(inverseRayDirection, tMax);
        assertNotSame(normalizedDirection, tMin);
        assertNotSame(normalizedDirection, tMax);
        assertNotSame(tMin, tMax);
    }

    @Provide
    Arbitrary<Integer> accessCounts() {
        return Arbitraries.integers().between(1, 1000);
    }

    @Provide
    Arbitrary<Double> coordinates() {
        return Arbitraries.doubles().between(-1000.0, 1000.0);
    }
}
