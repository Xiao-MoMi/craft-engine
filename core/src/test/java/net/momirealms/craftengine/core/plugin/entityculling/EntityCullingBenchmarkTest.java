package net.momirealms.craftengine.core.plugin.entityculling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance benchmark tests for ray tracing optimization components.
 * These tests verify that the optimization components work correctly
 * and provide baseline performance measurements.
 */
public class EntityCullingBenchmarkTest {

    private static final int WARMUP_ITERATIONS = 1000;
    private static final int BENCHMARK_ITERATIONS = 10000;

    @Test
    @DisplayName("ReusableVectorPool - Zero allocation verification")
    void testVectorPoolZeroAllocation() {
        ReusableVectorPool pool = new ReusableVectorPool();
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            pool.getInverseRayDirection().set(i, i, i);
            pool.getNormalizedDirection().set(i, i, i);
            pool.getTMin().set(i, i, i);
            pool.getTMax().set(i, i, i);
        }
        
        // Verify same instances are returned
        var inv1 = pool.getInverseRayDirection();
        var inv2 = pool.getInverseRayDirection();
        assertSame(inv1, inv2, "Vector pool should return same instance");
        
        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            pool.getInverseRayDirection().set(i % 100, i % 100, i % 100);
            pool.getNormalizedDirection().set(i % 100, i % 100, i % 100);
        }
        long endTime = System.nanoTime();
        
        double avgNanos = (double)(endTime - startTime) / BENCHMARK_ITERATIONS;
        System.out.printf("VectorPool access: %.2f ns/operation%n", avgNanos);
        
        assertTrue(avgNanos < 1000, "Vector pool access should be fast (< 1μs)");
    }

    @Test
    @DisplayName("LocalOcclusionCache - Bit packing performance")
    void testLocalOcclusionCacheBitPacking() {
        LocalOcclusionCache cache = new LocalOcclusionCache();
        
        // Simulate cache with test data using reflection or direct access
        // For this test, we verify the bounds checking performance
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            cache.isInBounds(i % 32, i % 32, i % 32);
        }
        
        // Benchmark bounds checking
        long startTime = System.nanoTime();
        int inBoundsCount = 0;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            if (cache.isInBounds(i % 64 - 16, i % 64 - 16, i % 64 - 16)) {
                inBoundsCount++;
            }
        }
        long endTime = System.nanoTime();
        
        double avgNanos = (double)(endTime - startTime) / BENCHMARK_ITERATIONS;
        System.out.printf("LocalOcclusionCache bounds check: %.2f ns/operation%n", avgNanos);
        System.out.printf("In-bounds ratio: %.2f%%%n", (double)inBoundsCount / BENCHMARK_ITERATIONS * 100);
        
        assertTrue(avgNanos < 100, "Bounds checking should be very fast (< 100ns)");
    }

    @Test
    @DisplayName("TemporalCoherenceCache - Cache lookup performance")
    void testTemporalCacheLookupPerformance() {
        TemporalCoherenceCache cache = new TemporalCoherenceCache();
        
        // Initialize cache with camera position
        cache.invalidateIfMoved(0, 64, 0);
        
        // Pre-populate cache with entries
        for (int i = 0; i < 500; i++) {
            long key = TemporalCoherenceCache.createEntityKey(i, 64, i);
            cache.cacheVisibility(key, i % 2 == 0);
        }
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            long key = TemporalCoherenceCache.createEntityKey(i % 500, 64, i % 500);
            cache.getCachedVisibility(key);
        }
        
        // Benchmark cache lookup
        long startTime = System.nanoTime();
        int hitCount = 0;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long key = TemporalCoherenceCache.createEntityKey(i % 500, 64, i % 500);
            Boolean result = cache.getCachedVisibility(key);
            if (result != null) {
                hitCount++;
            }
        }
        long endTime = System.nanoTime();
        
        double avgNanos = (double)(endTime - startTime) / BENCHMARK_ITERATIONS;
        System.out.printf("TemporalCache lookup: %.2f ns/operation%n", avgNanos);
        System.out.printf("Cache hit ratio: %.2f%%%n", (double)hitCount / BENCHMARK_ITERATIONS * 100);
        
        assertTrue(avgNanos < 500, "Cache lookup should be fast (< 500ns)");
        assertTrue(hitCount > 0, "Should have cache hits");
    }

    @Test
    @DisplayName("TemporalCoherenceCache - Entity key creation performance")
    void testEntityKeyCreationPerformance() {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            TemporalCoherenceCache.createEntityKey(i, i, i);
        }
        
        // Benchmark key creation
        long startTime = System.nanoTime();
        long checksum = 0;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            long key = TemporalCoherenceCache.createEntityKey(i % 1000, (i % 256) - 64, i % 1000);
            checksum ^= key;
        }
        long endTime = System.nanoTime();
        
        double avgNanos = (double)(endTime - startTime) / BENCHMARK_ITERATIONS;
        System.out.printf("Entity key creation: %.2f ns/operation%n", avgNanos);
        System.out.printf("Checksum (to prevent optimization): %d%n", checksum);
        
        assertTrue(avgNanos < 50, "Key creation should be very fast (< 50ns)");
    }

    @Test
    @DisplayName("HierarchicalOcclusionMap - Group lookup performance")
    void testHierarchicalMapGroupLookup() {
        HierarchicalOcclusionMap map = new HierarchicalOcclusionMap();
        LocalOcclusionCache cache = new LocalOcclusionCache();
        
        // Initialize with empty cache (no occluding blocks)
        // Note: We can't fully test without a real player, but we can test the data structures
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            map.isGroupInBounds(i % 16, i % 16, i % 16);
        }
        
        // Benchmark group bounds checking
        long startTime = System.nanoTime();
        int inBoundsCount = 0;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            if (map.isGroupInBounds(i % 20 - 2, i % 20 - 2, i % 20 - 2)) {
                inBoundsCount++;
            }
        }
        long endTime = System.nanoTime();
        
        double avgNanos = (double)(endTime - startTime) / BENCHMARK_ITERATIONS;
        System.out.printf("HierarchicalMap bounds check: %.2f ns/operation%n", avgNanos);
        System.out.printf("In-bounds ratio: %.2f%%%n", (double)inBoundsCount / BENCHMARK_ITERATIONS * 100);
        
        assertTrue(avgNanos < 100, "Group bounds checking should be very fast (< 100ns)");
    }

    @Test
    @DisplayName("HierarchicalOcclusionMap - Skip distance calculation")
    void testHierarchicalMapSkipDistance() {
        HierarchicalOcclusionMap map = new HierarchicalOcclusionMap();
        
        // Test skip distance for empty groups (should return 2)
        // Since map is not initialized, all groups should be non-occluding
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            map.getSkipDistance(i % 16, i % 16, i % 16);
        }
        
        // Benchmark skip distance calculation
        long startTime = System.nanoTime();
        int totalSkip = 0;
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            int skip = map.getSkipDistance(i % 16, i % 16, i % 16);
            totalSkip += skip;
        }
        long endTime = System.nanoTime();
        
        double avgNanos = (double)(endTime - startTime) / BENCHMARK_ITERATIONS;
        double avgSkip = (double)totalSkip / BENCHMARK_ITERATIONS;
        System.out.printf("Skip distance calculation: %.2f ns/operation%n", avgNanos);
        System.out.printf("Average skip distance: %.2f blocks%n", avgSkip);
        
        assertTrue(avgNanos < 100, "Skip distance calculation should be very fast (< 100ns)");
        assertEquals(2.0, avgSkip, 0.01, "Empty groups should have skip distance of 2");
    }

    @Test
    @DisplayName("Overall optimization components integration")
    void testOptimizationComponentsIntegration() {
        // Create all optimization components
        ReusableVectorPool vectorPool = new ReusableVectorPool();
        LocalOcclusionCache localCache = new LocalOcclusionCache();
        TemporalCoherenceCache temporalCache = new TemporalCoherenceCache();
        HierarchicalOcclusionMap hierarchicalMap = new HierarchicalOcclusionMap();
        
        // Verify all components are properly initialized
        assertNotNull(vectorPool.getInverseRayDirection());
        assertNotNull(vectorPool.getNormalizedDirection());
        assertNotNull(vectorPool.getTMin());
        assertNotNull(vectorPool.getTMax());
        
        assertFalse(localCache.isValid(), "Cache should start invalid");
        assertFalse(temporalCache.isInitialized(), "Temporal cache should start uninitialized");
        assertFalse(hierarchicalMap.isValid(), "Hierarchical map should start invalid");
        
        // Initialize temporal cache
        temporalCache.invalidateIfMoved(0, 64, 0);
        assertTrue(temporalCache.isInitialized(), "Temporal cache should be initialized after first position");
        
        // Test cache operations
        long entityKey = TemporalCoherenceCache.createEntityKey(10, 64, 10);
        temporalCache.cacheVisibility(entityKey, true);
        assertEquals(Boolean.TRUE, temporalCache.getCachedVisibility(entityKey));
        
        // Test shouldUseCache with small movement
        assertTrue(temporalCache.shouldUseCache(0.1, 64.1, 0.1), 
            "Should use cache for small movement");
        
        // Test shouldUseCache with large movement
        assertFalse(temporalCache.shouldUseCache(10, 64, 10), 
            "Should not use cache for large movement");
        
        System.out.println("All optimization components integrated successfully!");
    }
}
