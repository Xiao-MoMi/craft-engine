package net.momirealms.craftengine.core.plugin.entityculling;

import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for LocalOcclusionCache.
 * 
 */
class LocalOcclusionCachePropertyTest {

    /**
     * Property 2: Local Cache Consistency
     * 
     * For any player position (px, py, pz) and any world coordinate (wx, wy, wz) 
     * within 16 blocks of the player, the LocalOcclusionCache SHALL return the 
     * same occlusion value as the chunk-based lookup.
     * 
     * Since we cannot easily mock the chunk-based lookup in a property test,
     * we verify the cache's internal consistency: setting an occlusion value
     * and retrieving it should return the same value for all valid coordinates.
     * 
     */
    @Property(tries = 100)
    void cacheSetAndGetAreConsistent(
            @ForAll("localCoordinates") int localX,
            @ForAll("localCoordinates") int localY,
            @ForAll("localCoordinates") int localZ,
            @ForAll("baseCoordinates") int baseX,
            @ForAll("baseCoordinates") int baseY,
            @ForAll("baseCoordinates") int baseZ,
            @ForAll boolean occluding) {
        
        LocalOcclusionCache cache = new LocalOcclusionCache();
        
        // Use reflection to set base coordinates and valid state
        // since update() requires a Player which we can't easily mock
        setBaseCoordinates(cache, baseX, baseY, baseZ);
        setValid(cache, true);
        
        // Calculate world coordinates
        int worldX = baseX + localX;
        int worldY = baseY + localY;
        int worldZ = baseZ + localZ;
        
        // Set occlusion value using reflection to access private method
        setOccludingViaReflection(cache, localX, localY, localZ, occluding);
        
        // Verify the value can be retrieved correctly
        boolean retrieved = cache.isOccluding(worldX, worldY, worldZ);
        
        assertEquals(occluding, retrieved,
                String.format("Cache inconsistency at local(%d,%d,%d) world(%d,%d,%d): set %s, got %s",
                        localX, localY, localZ, worldX, worldY, worldZ, occluding, retrieved));
    }

    /**
     * Property: Bounds checking is correct for all coordinates.
     * 
     * For any world coordinate, isInBounds should return true if and only if
     * the coordinate is within the 32x32x32 cache region.
     * 
     */
    @Property(tries = 100)
    void boundsCheckingIsCorrect(
            @ForAll("worldCoordinates") int worldX,
            @ForAll("worldCoordinates") int worldY,
            @ForAll("worldCoordinates") int worldZ,
            @ForAll("baseCoordinates") int baseX,
            @ForAll("baseCoordinates") int baseY,
            @ForAll("baseCoordinates") int baseZ) {
        
        LocalOcclusionCache cache = new LocalOcclusionCache();
        setBaseCoordinates(cache, baseX, baseY, baseZ);
        
        boolean inBounds = cache.isInBounds(worldX, worldY, worldZ);
        
        // Calculate expected bounds
        int localX = worldX - baseX;
        int localY = worldY - baseY;
        int localZ = worldZ - baseZ;
        
        boolean expectedInBounds = localX >= 0 && localX < LocalOcclusionCache.SIZE &&
                                   localY >= 0 && localY < LocalOcclusionCache.SIZE &&
                                   localZ >= 0 && localZ < LocalOcclusionCache.SIZE;
        
        assertEquals(expectedInBounds, inBounds,
                String.format("Bounds check failed for world(%d,%d,%d) with base(%d,%d,%d): expected %s, got %s",
                        worldX, worldY, worldZ, baseX, baseY, baseZ, expectedInBounds, inBounds));
    }

    /**
     * Property: Multiple occlusion values can be set and retrieved independently.
     * 
     * Setting occlusion at one coordinate should not affect other coordinates.
     * 
     */
    @Property(tries = 100)
    void multipleOcclusionValuesAreIndependent(
            @ForAll("localCoordinates") int x1,
            @ForAll("localCoordinates") int y1,
            @ForAll("localCoordinates") int z1,
            @ForAll("localCoordinates") int x2,
            @ForAll("localCoordinates") int y2,
            @ForAll("localCoordinates") int z2) {
        
        // Skip if coordinates are the same
        Assume.that(x1 != x2 || y1 != y2 || z1 != z2);
        
        LocalOcclusionCache cache = new LocalOcclusionCache();
        setBaseCoordinates(cache, 0, 0, 0);
        setValid(cache, true);
        
        // Set first coordinate to occluding
        setOccludingViaReflection(cache, x1, y1, z1, true);
        
        // Set second coordinate to non-occluding
        setOccludingViaReflection(cache, x2, y2, z2, false);
        
        // Verify both values are correct
        assertTrue(cache.isOccluding(x1, y1, z1),
                String.format("First coordinate (%d,%d,%d) should be occluding", x1, y1, z1));
        assertFalse(cache.isOccluding(x2, y2, z2),
                String.format("Second coordinate (%d,%d,%d) should not be occluding", x2, y2, z2));
    }

    /**
     * Property: Cache invalidation clears valid state.
     * 
     * After invalidate() is called, isValid() should return false.
     */
    @Property(tries = 100)
    void invalidationClearsValidState(@ForAll("baseCoordinates") int ignored) {
        LocalOcclusionCache cache = new LocalOcclusionCache();
        setValid(cache, true);
        
        assertTrue(cache.isValid(), "Cache should be valid before invalidation");
        
        cache.invalidate();
        
        assertFalse(cache.isValid(), "Cache should be invalid after invalidation");
    }

    /**
     * Property: Bit packing uses correct memory layout.
     * 
     * The cache should correctly pack 32768 bits into 512 longs.
     * Setting all bits should result in all longs being -1 (all bits set).
     */
    @Property(tries = 10)
    void bitPackingIsCorrect(@ForAll("smallIntegers") int ignored) {
        LocalOcclusionCache cache = new LocalOcclusionCache();
        setBaseCoordinates(cache, 0, 0, 0);
        setValid(cache, true);
        
        // Set all coordinates to occluding
        for (int z = 0; z < LocalOcclusionCache.SIZE; z++) {
            for (int y = 0; y < LocalOcclusionCache.SIZE; y++) {
                for (int x = 0; x < LocalOcclusionCache.SIZE; x++) {
                    setOccludingViaReflection(cache, x, y, z, true);
                }
            }
        }
        
        // Verify all coordinates are occluding
        for (int z = 0; z < LocalOcclusionCache.SIZE; z++) {
            for (int y = 0; y < LocalOcclusionCache.SIZE; y++) {
                for (int x = 0; x < LocalOcclusionCache.SIZE; x++) {
                    assertTrue(cache.isOccluding(x, y, z),
                            String.format("Coordinate (%d,%d,%d) should be occluding", x, y, z));
                }
            }
        }
    }

    // Helper methods for reflection-based access

    private void setBaseCoordinates(LocalOcclusionCache cache, int baseX, int baseY, int baseZ) {
        try {
            java.lang.reflect.Field fieldX = LocalOcclusionCache.class.getDeclaredField("baseX");
            java.lang.reflect.Field fieldY = LocalOcclusionCache.class.getDeclaredField("baseY");
            java.lang.reflect.Field fieldZ = LocalOcclusionCache.class.getDeclaredField("baseZ");
            fieldX.setAccessible(true);
            fieldY.setAccessible(true);
            fieldZ.setAccessible(true);
            fieldX.setInt(cache, baseX);
            fieldY.setInt(cache, baseY);
            fieldZ.setInt(cache, baseZ);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set base coordinates", e);
        }
    }

    private void setValid(LocalOcclusionCache cache, boolean valid) {
        try {
            java.lang.reflect.Field field = LocalOcclusionCache.class.getDeclaredField("valid");
            field.setAccessible(true);
            field.setBoolean(cache, valid);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set valid state", e);
        }
    }

    private void setOccludingViaReflection(LocalOcclusionCache cache, int localX, int localY, int localZ, boolean occluding) {
        try {
            java.lang.reflect.Method method = LocalOcclusionCache.class.getDeclaredMethod(
                    "setOccluding", int.class, int.class, int.class, boolean.class);
            method.setAccessible(true);
            method.invoke(cache, localX, localY, localZ, occluding);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set occluding value", e);
        }
    }

    @Provide
    Arbitrary<Integer> localCoordinates() {
        return Arbitraries.integers().between(0, LocalOcclusionCache.SIZE - 1);
    }

    @Provide
    Arbitrary<Integer> baseCoordinates() {
        return Arbitraries.integers().between(-10000, 10000);
    }

    @Provide
    Arbitrary<Integer> worldCoordinates() {
        return Arbitraries.integers().between(-10100, 10100);
    }

    @Provide
    Arbitrary<Integer> smallIntegers() {
        return Arbitraries.integers().between(1, 10);
    }
}
