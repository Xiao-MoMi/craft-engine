package net.momirealms.craftengine.core.plugin.entityculling;

/**
 * Hierarchical occlusion map that maintains 2x2x2 block group summaries.
 * Enables fast skipping of empty regions during ray traversal.
 * 
 * The map divides the 32x32x32 local cache into 16x16x16 groups of 2x2x2 blocks each.
 * Each group is represented by a single bit indicating whether ANY block in the group is occluding.
 * 
 * Memory usage: 64 longs = 512 bytes (4096 bits / 64 bits per long)
 * 
 */
public final class HierarchicalOcclusionMap {
    
    /** Size of each group in blocks */
    public static final int GROUP_SIZE = 2;
    
    /** Number of groups per axis (32 / 2 = 16) */
    public static final int GROUPS_PER_AXIS = LocalOcclusionCache.SIZE / GROUP_SIZE;
    
    /** Total number of groups (16 * 16 * 16 = 4096) */
    private static final int TOTAL_GROUPS = GROUPS_PER_AXIS * GROUPS_PER_AXIS * GROUPS_PER_AXIS;
    
    /** Number of longs needed to store all group bits (4096 / 64 = 64) */
    private static final int LONGS_NEEDED = TOTAL_GROUPS / 64;
    
    /** Bit-packed group occlusion data */
    private final long[] groupData = new long[LONGS_NEEDED];
    
    /** Whether the map contains valid data */
    private boolean valid = false;
    
    /**
     * Updates the hierarchical map from the local occlusion cache.
     * A group is marked as occluding if ANY block in the 2x2x2 region is occluding.
     * 
     * @param cache the local occlusion cache to read from
     */
    public void updateFromLocalCache(LocalOcclusionCache cache) {
        // Clear existing data
        java.util.Arrays.fill(groupData, 0L);
        
        // Iterate through all groups
        for (int groupZ = 0; groupZ < GROUPS_PER_AXIS; groupZ++) {
            for (int groupY = 0; groupY < GROUPS_PER_AXIS; groupY++) {
                for (int groupX = 0; groupX < GROUPS_PER_AXIS; groupX++) {
                    // Check if any block in the 2x2x2 group is occluding
                    boolean groupOccluding = isAnyBlockOccluding(cache, groupX, groupY, groupZ);
                    
                    if (groupOccluding) {
                        setGroupOccluding(groupX, groupY, groupZ, true);
                    }
                }
            }
        }
        
        this.valid = true;
    }
    
    /**
     * Checks if any block in a 2x2x2 group is occluding.
     * 
     * @param cache the local occlusion cache
     * @param groupX group X coordinate (0-15)
     * @param groupY group Y coordinate (0-15)
     * @param groupZ group Z coordinate (0-15)
     * @return true if any block in the group is occluding
     */
    private boolean isAnyBlockOccluding(LocalOcclusionCache cache, int groupX, int groupY, int groupZ) {
        int baseX = cache.getBaseX();
        int baseY = cache.getBaseY();
        int baseZ = cache.getBaseZ();
        
        // Calculate world coordinates for the group's base block
        int worldBaseX = baseX + (groupX * GROUP_SIZE);
        int worldBaseY = baseY + (groupY * GROUP_SIZE);
        int worldBaseZ = baseZ + (groupZ * GROUP_SIZE);
        
        // Check all 8 blocks in the 2x2x2 group
        for (int dz = 0; dz < GROUP_SIZE; dz++) {
            for (int dy = 0; dy < GROUP_SIZE; dy++) {
                for (int dx = 0; dx < GROUP_SIZE; dx++) {
                    if (cache.isOccluding(worldBaseX + dx, worldBaseY + dy, worldBaseZ + dz)) {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }

    
    /**
     * Checks if a 2x2x2 group contains any occluding blocks.
     * 
     * @param groupX group X coordinate (0-15)
     * @param groupY group Y coordinate (0-15)
     * @param groupZ group Z coordinate (0-15)
     * @return true if the group contains at least one occluding block
     */
    public boolean isGroupOccluding(int groupX, int groupY, int groupZ) {
        int index = getGroupIndex(groupX, groupY, groupZ);
        int arrayIndex = index >>> 6; // / 64
        int bitIndex = index & 63;    // % 64
        
        return (groupData[arrayIndex] & (1L << bitIndex)) != 0;
    }
    
    /**
     * Gets the skip distance for a group.
     * If the group is not occluding, returns 2 (can skip the entire 2x2x2 region).
     * If the group is occluding, returns 1 (must check block by block).
     * 
     * @param groupX group X coordinate (0-15)
     * @param groupY group Y coordinate (0-15)
     * @param groupZ group Z coordinate (0-15)
     * @return skip distance (2 for empty groups, 1 for occluding groups)
     */
    public int getSkipDistance(int groupX, int groupY, int groupZ) {
        return isGroupOccluding(groupX, groupY, groupZ) ? 1 : GROUP_SIZE;
    }
    
    /**
     * Checks if the given group coordinates are within valid bounds.
     * 
     * @param groupX group X coordinate
     * @param groupY group Y coordinate
     * @param groupZ group Z coordinate
     * @return true if the coordinates are within bounds (0-15 for each axis)
     */
    public boolean isGroupInBounds(int groupX, int groupY, int groupZ) {
        return groupX >= 0 && groupX < GROUPS_PER_AXIS &&
               groupY >= 0 && groupY < GROUPS_PER_AXIS &&
               groupZ >= 0 && groupZ < GROUPS_PER_AXIS;
    }
    
    /**
     * Converts local block coordinates to group coordinates.
     * 
     * @param localCoord local block coordinate (0-31)
     * @return group coordinate (0-15)
     */
    public static int toGroupCoord(int localCoord) {
        return localCoord >>> 1; // / 2
    }
    
    /**
     * Converts world block coordinates to group coordinates relative to a cache base.
     * 
     * @param worldCoord world block coordinate
     * @param baseCoord cache base coordinate
     * @return group coordinate (0-15), or -1 if out of bounds
     */
    public static int worldToGroupCoord(int worldCoord, int baseCoord) {
        int local = worldCoord - baseCoord;
        if (local < 0 || local >= LocalOcclusionCache.SIZE) {
            return -1;
        }
        return local >>> 1; // / 2
    }
    
    /**
     * Sets the occlusion state for a group.
     * 
     * @param groupX group X coordinate (0-15)
     * @param groupY group Y coordinate (0-15)
     * @param groupZ group Z coordinate (0-15)
     * @param occluding whether the group contains occluding blocks
     */
    private void setGroupOccluding(int groupX, int groupY, int groupZ, boolean occluding) {
        int index = getGroupIndex(groupX, groupY, groupZ);
        int arrayIndex = index >>> 6; // / 64
        int bitIndex = index & 63;    // % 64
        
        if (occluding) {
            groupData[arrayIndex] |= (1L << bitIndex);
        } else {
            groupData[arrayIndex] &= ~(1L << bitIndex);
        }
    }
    
    /**
     * Calculates the linear index for a group coordinate.
     * Index = groupX + (groupY * 16) + (groupZ * 16 * 16)
     * 
     * @param groupX group X coordinate (0-15)
     * @param groupY group Y coordinate (0-15)
     * @param groupZ group Z coordinate (0-15)
     * @return linear index (0-4095)
     */
    private static int getGroupIndex(int groupX, int groupY, int groupZ) {
        return groupX + (groupY << 4) + (groupZ << 8);
    }
    
    /**
     * Invalidates the map, marking it as needing an update.
     */
    public void invalidate() {
        this.valid = false;
    }
    
    /**
     * Checks if the map contains valid data.
     * 
     * @return true if the map is valid
     */
    public boolean isValid() {
        return valid;
    }
    
    /**
     * Gets the raw group data array for testing purposes.
     * 
     * @return the group data array
     */
    long[] getGroupData() {
        return groupData;
    }
}
