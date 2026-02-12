package net.momirealms.craftengine.core.plugin.entityculling;

import java.util.HashMap;
import java.util.Map;

/**
 * Frame-to-frame visibility cache for temporal coherence optimization.
 * Caches visibility results for entities when the camera hasn't moved significantly,
 * avoiding redundant ray tracing calculations.
 * 
 */
public final class TemporalCoherenceCache {
    
    /** Maximum number of entries in the cache */
    public static final int MAX_ENTRIES = 1024;
    
    /** Camera movement threshold for cache invalidation */
    public static final double INVALIDATION_THRESHOLD = 0.5;
    
    /** Squared threshold for faster distance comparison */
    private static final double INVALIDATION_THRESHOLD_SQ = INVALIDATION_THRESHOLD * INVALIDATION_THRESHOLD;
    
    /** Visibility cache mapping entity keys to visibility results */
    private final Map<Long, Boolean> visibilityCache;
    
    /** Last known camera position */
    private double lastCameraX;
    private double lastCameraY;
    private double lastCameraZ;
    
    /** Whether the cache has been initialized with a camera position */
    private boolean initialized = false;
    
    /**
     * Creates a new temporal coherence cache.
     */
    public TemporalCoherenceCache() {
        this.visibilityCache = new HashMap<>(MAX_ENTRIES);
    }
    
    /**
     * Checks if the cache should be used based on camera movement.
     * Returns true if the camera has moved less than the invalidation threshold
     * since the last check.
     * 
     * @param cameraX current camera X position
     * @param cameraY current camera Y position
     * @param cameraZ current camera Z position
     * @return true if cached results can be used, false if cache should be invalidated
     */
    public boolean shouldUseCache(double cameraX, double cameraY, double cameraZ) {
        if (!initialized) {
            return false;
        }
        
        double dx = cameraX - lastCameraX;
        double dy = cameraY - lastCameraY;
        double dz = cameraZ - lastCameraZ;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        
        return distanceSq < INVALIDATION_THRESHOLD_SQ;
    }
    
    /**
     * Gets the cached visibility result for an entity.
     * 
     * @param entityKey the unique key for the entity
     * @return the cached visibility result, or null if not cached
     */
    public Boolean getCachedVisibility(long entityKey) {
        return visibilityCache.get(entityKey);
    }
    
    /**
     * Caches a visibility result for an entity.
     * If the cache is at maximum capacity, the oldest entries are evicted.
     * 
     * @param entityKey the unique key for the entity
     * @param visible whether the entity is visible
     */
    public void cacheVisibility(long entityKey, boolean visible) {
        // Enforce maximum size
        if (visibilityCache.size() >= MAX_ENTRIES && !visibilityCache.containsKey(entityKey)) {
            // Simple eviction: clear half the cache when full
            // This is a simple LRU-like strategy without tracking access order
            evictOldEntries();
        }
        
        visibilityCache.put(entityKey, visible);
    }
    
    /**
     * Invalidates the cache if the camera has moved beyond the threshold.
     * Also updates the stored camera position.
     * 
     * @param cameraX current camera X position
     * @param cameraY current camera Y position
     * @param cameraZ current camera Z position
     */
    public void invalidateIfMoved(double cameraX, double cameraY, double cameraZ) {
        if (!initialized) {
            // First call - initialize camera position
            lastCameraX = cameraX;
            lastCameraY = cameraY;
            lastCameraZ = cameraZ;
            initialized = true;
            return;
        }
        
        double dx = cameraX - lastCameraX;
        double dy = cameraY - lastCameraY;
        double dz = cameraZ - lastCameraZ;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        
        if (distanceSq >= INVALIDATION_THRESHOLD_SQ) {
            // Camera moved beyond threshold - invalidate cache
            visibilityCache.clear();
            lastCameraX = cameraX;
            lastCameraY = cameraY;
            lastCameraZ = cameraZ;
        }
    }
    
    /**
     * Clears all cached visibility results.
     */
    public void clear() {
        visibilityCache.clear();
    }
    
    /**
     * Gets the current number of cached entries.
     * 
     * @return the number of cached entries
     */
    public int size() {
        return visibilityCache.size();
    }
    
    /**
     * Checks if the cache is empty.
     * 
     * @return true if the cache is empty
     */
    public boolean isEmpty() {
        return visibilityCache.isEmpty();
    }
    
    /**
     * Checks if the cache has been initialized with a camera position.
     * 
     * @return true if initialized
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Gets the last known camera X position.
     * 
     * @return last camera X position
     */
    public double getLastCameraX() {
        return lastCameraX;
    }
    
    /**
     * Gets the last known camera Y position.
     * 
     * @return last camera Y position
     */
    public double getLastCameraY() {
        return lastCameraY;
    }
    
    /**
     * Gets the last known camera Z position.
     * 
     * @return last camera Z position
     */
    public double getLastCameraZ() {
        return lastCameraZ;
    }
    
    /**
     * Creates a unique key for an entity based on its position.
     * Uses bit packing to create a compact 64-bit key.
     * 
     * @param x entity X coordinate (block position)
     * @param y entity Y coordinate (block position)
     * @param z entity Z coordinate (block position)
     * @return unique entity key
     */
    public static long createEntityKey(int x, int y, int z) {
        // Pack coordinates into a long:
        // X: 26 bits (supports -33M to +33M)
        // Y: 12 bits (supports -2048 to +2047)
        // Z: 26 bits (supports -33M to +33M)
        return ((long)(x & 0x3FFFFFF) << 38) | 
               ((long)(y & 0xFFF) << 26) | 
               (z & 0x3FFFFFF);
    }
    
    /**
     * Evicts approximately half of the cache entries.
     * Simple eviction strategy that clears entries without tracking access order.
     */
    private void evictOldEntries() {
        int targetSize = MAX_ENTRIES / 2;
        int toRemove = visibilityCache.size() - targetSize;
        
        if (toRemove <= 0) {
            return;
        }
        
        // Simple eviction: remove entries by iterating
        var iterator = visibilityCache.keySet().iterator();
        int removed = 0;
        while (iterator.hasNext() && removed < toRemove) {
            iterator.next();
            iterator.remove();
            removed++;
        }
    }
}
