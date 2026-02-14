package net.momirealms.craftengine.core.plugin.entityculling;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for TemporalCoherenceCache.
 *
 *
 */
class TemporalCoherenceCachePropertyTest {

    @Property(tries = 100)
    void cacheIsUsedWhenCameraMovesLessThanThreshold(
            @ForAll("cameraPositions") double startX,
            @ForAll("cameraPositions") double startY,
            @ForAll("cameraPositions") double startZ,
            @ForAll("smallMovements") double deltaX,
            @ForAll("smallMovements") double deltaY,
            @ForAll("smallMovements") double deltaZ,
            @ForAll @IntRange(min = -1000, max = 1000) int entityX,
            @ForAll @IntRange(min = -100, max = 100) int entityY,
            @ForAll @IntRange(min = -1000, max = 1000) int entityZ,
            @ForAll boolean visibility) {

        double movementSq = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        Assume.that(movementSq < TemporalCoherenceCache.INVALIDATION_THRESHOLD * TemporalCoherenceCache.INVALIDATION_THRESHOLD);

        TemporalCoherenceCache cache = new TemporalCoherenceCache();
        cache.invalidateIfMoved(startX, startY, startZ);

        long entityKey = TemporalCoherenceCache.createEntityKey(entityX, entityY, entityZ);
        cache.cacheVisibility(entityKey, visibility);

        double newX = startX + deltaX;
        double newY = startY + deltaY;
        double newZ = startZ + deltaZ;

        assertTrue(cache.shouldUseCache(newX, newY, newZ),
                String.format("Cache should be used when movement (%.4f) is less than threshold (%.1f)",
                        Math.sqrt(movementSq), TemporalCoherenceCache.INVALIDATION_THRESHOLD));

        Boolean cachedValue = cache.getCachedVisibility(entityKey);
        assertNotNull(cachedValue, "Cached visibility should still be accessible");
        assertEquals(visibility, cachedValue, "Cached visibility should match original value");
    }

    @Property(tries = 100)
    void cacheIsInvalidatedWhenCameraMovesAtOrAboveThreshold(
            @ForAll("cameraPositions") double startX,
            @ForAll("cameraPositions") double startY,
            @ForAll("cameraPositions") double startZ,
            @ForAll("largeMovements") double deltaX,
            @ForAll("largeMovements") double deltaY,
            @ForAll("largeMovements") double deltaZ,
            @ForAll @IntRange(min = -1000, max = 1000) int entityX,
            @ForAll @IntRange(min = -100, max = 100) int entityY,
            @ForAll @IntRange(min = -1000, max = 1000) int entityZ,
            @ForAll boolean visibility) {

        double movementSq = deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
        Assume.that(movementSq >= TemporalCoherenceCache.INVALIDATION_THRESHOLD * TemporalCoherenceCache.INVALIDATION_THRESHOLD);

        TemporalCoherenceCache cache = new TemporalCoherenceCache();
        cache.invalidateIfMoved(startX, startY, startZ);

        long entityKey = TemporalCoherenceCache.createEntityKey(entityX, entityY, entityZ);
        cache.cacheVisibility(entityKey, visibility);

        assertNotNull(cache.getCachedVisibility(entityKey), "Cache should have entry before invalidation");

        double newX = startX + deltaX;
        double newY = startY + deltaY;
        double newZ = startZ + deltaZ;

        assertFalse(cache.shouldUseCache(newX, newY, newZ),
                String.format("Cache should NOT be used when movement (%.4f) is >= threshold (%.1f)",
                        Math.sqrt(movementSq), TemporalCoherenceCache.INVALIDATION_THRESHOLD));

        cache.invalidateIfMoved(newX, newY, newZ);

        assertNull(cache.getCachedVisibility(entityKey), "Cache should be cleared after invalidation");
        assertTrue(cache.isEmpty(), "Cache should be empty after invalidation");
    }

    @Property(tries = 100)
    void cacheStoresAndRetrievesCorrectly(
            @ForAll @IntRange(min = -1000, max = 1000) int entityX,
            @ForAll @IntRange(min = -100, max = 100) int entityY,
            @ForAll @IntRange(min = -1000, max = 1000) int entityZ,
            @ForAll boolean visibility) {

        TemporalCoherenceCache cache = new TemporalCoherenceCache();

        long entityKey = TemporalCoherenceCache.createEntityKey(entityX, entityY, entityZ);

        assertNull(cache.getCachedVisibility(entityKey), "Cache should return null for uncached entity");

        cache.cacheVisibility(entityKey, visibility);

        Boolean cachedValue = cache.getCachedVisibility(entityKey);
        assertNotNull(cachedValue, "Cache should return value for cached entity");
        assertEquals(visibility, cachedValue, "Cached value should match stored value");
    }

    @Property(tries = 100)
    void entityKeyCreationIsDeterministic(
            @ForAll @IntRange(min = -1000000, max = 1000000) int x,
            @ForAll @IntRange(min = -2000, max = 2000) int y,
            @ForAll @IntRange(min = -1000000, max = 1000000) int z) {

        long key1 = TemporalCoherenceCache.createEntityKey(x, y, z);
        long key2 = TemporalCoherenceCache.createEntityKey(x, y, z);

        assertEquals(key1, key2, "Entity key should be deterministic");
    }

    @Property(tries = 100)
    void differentCoordinatesProduceDifferentKeys(
            @ForAll @IntRange(min = -1000, max = 1000) int x1,
            @ForAll @IntRange(min = -100, max = 100) int y1,
            @ForAll @IntRange(min = -1000, max = 1000) int z1,
            @ForAll @IntRange(min = -1000, max = 1000) int x2,
            @ForAll @IntRange(min = -100, max = 100) int y2,
            @ForAll @IntRange(min = -1000, max = 1000) int z2) {

        Assume.that(x1 != x2 || y1 != y2 || z1 != z2);

        long key1 = TemporalCoherenceCache.createEntityKey(x1, y1, z1);
        long key2 = TemporalCoherenceCache.createEntityKey(x2, y2, z2);

        assertNotEquals(key1, key2,
                String.format("Different coordinates (%d,%d,%d) and (%d,%d,%d) should produce different keys",
                        x1, y1, z1, x2, y2, z2));
    }

    @Property(tries = 100)
    void uninitializedCacheShouldNotBeUsed(
            @ForAll("cameraPositions") double cameraX,
            @ForAll("cameraPositions") double cameraY,
            @ForAll("cameraPositions") double cameraZ) {

        TemporalCoherenceCache cache = new TemporalCoherenceCache();

        assertFalse(cache.isInitialized(), "New cache should not be initialized");
        assertFalse(cache.shouldUseCache(cameraX, cameraY, cameraZ),
                "Uninitialized cache should not be used");
    }

    @Provide
    Arbitrary<Double> cameraPositions() {
        return Arbitraries.doubles().between(-10000.0, 10000.0);
    }

    @Provide
    Arbitrary<Double> smallMovements() {
        return Arbitraries.doubles().between(-0.2, 0.2);
    }

    @Provide
    Arbitrary<Double> largeMovements() {
        return Arbitraries.doubles().between(0.5, 5.0);
    }
}
