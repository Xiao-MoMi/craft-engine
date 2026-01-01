package net.momirealms.craftengine.core.plugin.entityculling;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.world.ChunkPos;
import net.momirealms.craftengine.core.world.chunk.client.ClientChunk;

/**
 * 32x32x32 bit-packed local occlusion cache centered on the player.
 * Provides fast direct array access for block occlusion lookups within cache bounds,
 * eliminating the need for chunk hash lookups in the hot path.
 * 
 * Memory usage: 512 longs = 4KB per player (32768 bits / 64 bits per long)
 * 
 */
public final class LocalOcclusionCache {
    
    /** Cache size in blocks per axis */
    public static final int SIZE = 32;
    
    /** Total number of bits needed (32 * 32 * 32 = 32768) */
    private static final int TOTAL_BITS = SIZE * SIZE * SIZE;
    
    /** Number of longs needed to store all bits (32768 / 64 = 512) */
    private static final int LONGS_NEEDED = TOTAL_BITS / 64;
    
    /** Half size for centering calculations */
    private static final int HALF_SIZE = SIZE / 2;
    
    /** Bit-packed occlusion data */
    private final long[] data = new long[LONGS_NEEDED];
    
    /** Base world coordinates (cache is centered at base + HALF_SIZE) */
    private int baseX;
    private int baseY;
    private int baseZ;
    
    /** Whether the cache contains valid data */
    private boolean valid = false;
    
    /**
     * Updates the cache with occlusion data centered on the player's position.
     * 
     * @param player the player to center the cache on
     */
    public void update(Player player) {
        // Get player block position
        int playerX = (int) Math.floor(player.x());
        int playerY = (int) Math.floor(player.y());
        int playerZ = (int) Math.floor(player.z());
        
        // Calculate base coordinates (cache covers baseX to baseX+31, etc.)
        this.baseX = playerX - HALF_SIZE;
        this.baseY = playerY - HALF_SIZE;
        this.baseZ = playerZ - HALF_SIZE;
        
        // Clear existing data
        java.util.Arrays.fill(data, 0L);
        
        // Populate cache from chunk data
        for (int localZ = 0; localZ < SIZE; localZ++) {
            int worldZ = baseZ + localZ;
            int chunkZ = worldZ >> 4;
            
            for (int localX = 0; localX < SIZE; localX++) {
                int worldX = baseX + localX;
                int chunkX = worldX >> 4;
                
                // Get chunk for this column
                ClientChunk chunk = player.getTrackedChunk(ChunkPos.asLong(chunkX, chunkZ));
                if (chunk == null) {
                    continue; // Leave as non-occluding
                }
                
                for (int localY = 0; localY < SIZE; localY++) {
                    int worldY = baseY + localY;
                    
                    if (chunk.isOccluding(worldX, worldY, worldZ)) {
                        setOccluding(localX, localY, localZ, true);
                    }
                }
            }
        }
        
        this.valid = true;
    }
    
    /**
     * Checks if a block at the given world coordinates is occluding.
     * Only valid for coordinates within cache bounds.
     * 
     * @param worldX world X coordinate
     * @param worldY world Y coordinate
     * @param worldZ world Z coordinate
     * @return true if the block is occluding, false otherwise
     */
    public boolean isOccluding(int worldX, int worldY, int worldZ) {
        int localX = worldX - baseX;
        int localY = worldY - baseY;
        int localZ = worldZ - baseZ;
        
        int index = getOcclusionIndex(localX, localY, localZ);
        int arrayIndex = index >>> 6; // / 64
        int bitIndex = index & 63;    // % 64
        
        return (data[arrayIndex] & (1L << bitIndex)) != 0;
    }
    
    /**
     * Checks if the given world coordinates are within the cache bounds.
     * 
     * @param worldX world X coordinate
     * @param worldY world Y coordinate
     * @param worldZ world Z coordinate
     * @return true if the coordinates are within bounds
     */
    public boolean isInBounds(int worldX, int worldY, int worldZ) {
        int localX = worldX - baseX;
        int localY = worldY - baseY;
        int localZ = worldZ - baseZ;
        
        return localX >= 0 && localX < SIZE &&
               localY >= 0 && localY < SIZE &&
               localZ >= 0 && localZ < SIZE;
    }
    
    /**
     * Invalidates the cache, marking it as needing an update.
     */
    public void invalidate() {
        this.valid = false;
    }
    
    /**
     * Checks if the cache contains valid data.
     * 
     * @return true if the cache is valid
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Gets the base X coordinate of the cache.
     * 
     * @return base X coordinate
     */
    public int getBaseX() {
        return baseX;
    }
    
    /**
     * Gets the base Y coordinate of the cache.
     * 
     * @return base Y coordinate
     */
    public int getBaseY() {
        return baseY;
    }
    
    /**
     * Gets the base Z coordinate of the cache.
     * 
     * @return base Z coordinate
     */
    public int getBaseZ() {
        return baseZ;
    }
    
    /**
     * Sets the occlusion state for a local coordinate.
     * 
     * @param localX local X coordinate (0-31)
     * @param localY local Y coordinate (0-31)
     * @param localZ local Z coordinate (0-31)
     * @param occluding whether the block is occluding
     */
    private void setOccluding(int localX, int localY, int localZ, boolean occluding) {
        int index = getOcclusionIndex(localX, localY, localZ);
        int arrayIndex = index >>> 6; // / 64
        int bitIndex = index & 63;    // % 64
        
        if (occluding) {
            data[arrayIndex] |= (1L << bitIndex);
        } else {
            data[arrayIndex] &= ~(1L << bitIndex);
        }
    }
    
    /**
     * Calculates the linear index for a local coordinate.
     * Index = localX + (localY * 32) + (localZ * 32 * 32)
     * 
     * @param localX local X coordinate (0-31)
     * @param localY local Y coordinate (0-31)
     * @param localZ local Z coordinate (0-31)
     * @return linear index (0-32767)
     */
    private static int getOcclusionIndex(int localX, int localY, int localZ) {
        return localX + (localY << 5) + (localZ << 10);
    }
}
