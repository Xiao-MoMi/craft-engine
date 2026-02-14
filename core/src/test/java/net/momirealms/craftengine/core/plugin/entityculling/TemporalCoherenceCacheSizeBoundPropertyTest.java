package net.momirealms.craftengine.core.plugin.entityculling;

import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for TemporalCoherenceCache size bounds.
 *
 *
 */
class TemporalCoherenceCacheSizeBoundPropertyTest {

    /**
     * Property 6: Temporal Cache Size Bound
     *
     * For any sequence of visibility calculations, the temporal cache size
     * SHALL never exceed 1024 entries.
     *
     */
    @Property(tries = 100)
    void cacheSizeNeverExceedsMaxEntries(
            @ForAll("entryCountsAboveMax") int entryCount) {

        TemporalCoherenceCache cache = new TemporalCoherenceCache();

        // Add more entries than the maximum
        for (int i = 0; i < entryCount; i++) {
            long entityKey = TemporalCoherenceCache.createEntityKey(i, 0, 0);
            cache.cacheVisibility(entityKey, i % 2 == 0);

            // Verify size never exceeds maximum
            assertTrue(cache.size() <= TemporalCoherenceCache.MAX_ENTRIES,
                    String.format("Cache size (%d) exceeded maximum (%d) after adding entry %d",
                            cache.size(), TemporalCoherenceCache.MAX_ENTRIES, i));
        }

        // Final verification
        assertTrue(cache.size() <= TemporalCoherenceCache.MAX_ENTRIES,
                String.format("Final cache size (%d) exceeded maximum (%d)",
                        cache.size(), TemporalCoherenceCache.MAX_ENTRIES));
    }

    /**
     * Property: Cache eviction maintains valid entries.
     *
     * After eviction, remaining entries should still be retrievable.
     */
    @Property(tries = 50)
    void cacheEvictionMaintainsValidEntries(
            @ForAll("entryCountsAboveMax") int entryCount) {

        TemporalCoherenceCache cache = new TemporalCoherenceCache();

        // Add entries
        for (int i = 0; i < entryCount; i++) {
            long entityKey = TemporalCoherenceCache.createEntityKey(i, 0, 0);
            cache.cacheVisibility(entityKey, i % 2 == 0);
        }

        // Verify all remaining entries are valid
        int validEntries = 0;
        for (int i = 0; i < entryCount; i++) {
            long entityKey = TemporalCoherenceCache.createEntityKey(i, 0, 0);
            Boolean value = cache.getCachedVisibility(entityKey);
            if (value != null) {
                validEntries++;
                // If entry exists, it should have the correct value
                assertEquals(i % 2 == 0, value,
                        String.format("Entry %d has incorrect value", i));
            }
        }

        // Number of valid entries should match cache size
        assertEquals(cache.size(), validEntries,
                "Number of valid entries should match cache size");
    }

    /**
     * Property: Cache size is bounded even with random access patterns.
     *
     * With random entity positions, cache size should still be bounded.
     */
    @Property(tries = 100)
    void cacheSizeBoundedWithRandomAccess(
            @ForAll("randomEntityPositions") int[][] positions) {

        TemporalCoherenceCache cache = new TemporalCoherenceCache();

        for (int[] pos : positions) {
            long entityKey = TemporalCoherenceCache.createEntityKey(pos[0], pos[1], pos[2]);
            cache.cacheVisibility(entityKey, pos[0] % 2 == 0);

            // Verify size never exceeds maximum
            assertTrue(cache.size() <= TemporalCoherenceCache.MAX_ENTRIES,
                    String.format("Cache size (%d) exceeded maximum (%d)",
                            cache.size(), TemporalCoherenceCache.MAX_ENTRIES));
        }
    }

    @Provide
    Arbitrary<Integer> entryCountsAboveMax() {
        // Generate counts that exceed the maximum to test eviction
        return Arbitraries.integers().between(
                TemporalCoherenceCache.MAX_ENTRIES + 1,
                TemporalCoherenceCache.MAX_ENTRIES + 500);
    }

    @Provide
    Arbitrary<int[][]> randomEntityPositions() {
        return Arbitraries.integers().between(-1000, 1000)
                .array(int[].class).ofSize(3)
                .array(int[][].class).ofMinSize(1500).ofMaxSize(2000);
    }
}
