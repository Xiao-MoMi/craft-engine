package net.momirealms.craftengine.core.plugin.entityculling;

import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for HierarchicalOcclusionMap.
 * 
 */
class HierarchicalOcclusionMapPropertyTest {

    /**
     * Property 7: Hierarchical Occlusion Consistency
     * 
     * For any 2x2x2 block group, the hierarchical occlusion value SHALL be TRUE 
     * if and only if at least one block in the group is occluding.
     * 
     */
    @Property(tries = 100)
    void hierarchicalOcclusionConsistency(
            @ForAll("groupCoordinates") int groupX,
            @ForAll("groupCoordinates") int groupY,
            @ForAll("groupCoordinates") int groupZ,
            @ForAll("occlusionPattern") boolean[] pattern) {
        
        // Create and configure local occlusion cache
        LocalOcclusionCache cache = new LocalOcclusionCache();
        setBaseCoordinates(cache, 0, 0, 0);
        setValid(cache, true);
        
        // Calculate the base local coordinates for this group
        int baseLocalX = groupX * HierarchicalOcclusionMap.GROUP_SIZE;
        int baseLocalY = groupY * HierarchicalOcclusionMap.GROUP_SIZE;
        int baseLocalZ = groupZ * HierarchicalOcclusionMap.GROUP_SIZE;
        
        // Set occlusion values for the 2x2x2 group based on pattern
        // Pattern has 8 booleans for the 8 blocks in the group
        boolean anyOccluding = false;
        int patternIndex = 0;
        for (int dz = 0; dz < HierarchicalOcclusionMap.GROUP_SIZE; dz++) {
            for (int dy = 0; dy < HierarchicalOcclusionMap.GROUP_SIZE; dy++) {
                for (int dx = 0; dx < HierarchicalOcclusionMap.GROUP_SIZE; dx++) {
                    boolean occluding = pattern[patternIndex++];
                    if (occluding) {
                        anyOccluding = true;
                    }
                    setOccludingViaReflection(cache, 
                            baseLocalX + dx, 
                            baseLocalY + dy, 
                            baseLocalZ + dz, 
                            occluding);
                }
            }
        }
        
        // Create hierarchical map and update from cache
        HierarchicalOcclusionMap hierarchicalMap = new HierarchicalOcclusionMap();
        hierarchicalMap.updateFromLocalCache(cache);
        
        // Verify hierarchical occlusion matches expected value
        boolean groupOccluding = hierarchicalMap.isGroupOccluding(groupX, groupY, groupZ);
        
        assertEquals(anyOccluding, groupOccluding,
                String.format("Group (%d,%d,%d) occlusion mismatch: expected %s (any block occluding), got %s",
                        groupX, groupY, groupZ, anyOccluding, groupOccluding));
    }


    /**
     * Property: Empty groups have no occluding blocks.
     * 
     * If no blocks in a group are set to occluding, the group should not be marked as occluding.
     * 
     */
    @Property(tries = 100)
    void emptyGroupsAreNotOccluding(
            @ForAll("groupCoordinates") int groupX,
            @ForAll("groupCoordinates") int groupY,
            @ForAll("groupCoordinates") int groupZ) {
        
        // Create cache with no occluding blocks (default state)
        LocalOcclusionCache cache = new LocalOcclusionCache();
        setBaseCoordinates(cache, 0, 0, 0);
        setValid(cache, true);
        // Note: All blocks default to non-occluding (0 bits)
        
        // Create hierarchical map and update from cache
        HierarchicalOcclusionMap hierarchicalMap = new HierarchicalOcclusionMap();
        hierarchicalMap.updateFromLocalCache(cache);
        
        // Verify group is not occluding
        assertFalse(hierarchicalMap.isGroupOccluding(groupX, groupY, groupZ),
                String.format("Empty group (%d,%d,%d) should not be occluding", groupX, groupY, groupZ));
    }

    /**
     * Property: Single occluding block makes group occluding.
     * 
     * If exactly one block in a group is occluding, the entire group should be marked as occluding.
     * 
     */
    @Property(tries = 100)
    void singleOccludingBlockMakesGroupOccluding(
            @ForAll("groupCoordinates") int groupX,
            @ForAll("groupCoordinates") int groupY,
            @ForAll("groupCoordinates") int groupZ,
            @ForAll("blockOffset") int dx,
            @ForAll("blockOffset") int dy,
            @ForAll("blockOffset") int dz) {
        
        // Create cache with single occluding block
        LocalOcclusionCache cache = new LocalOcclusionCache();
        setBaseCoordinates(cache, 0, 0, 0);
        setValid(cache, true);
        
        // Set single block in the group to occluding
        int localX = groupX * HierarchicalOcclusionMap.GROUP_SIZE + dx;
        int localY = groupY * HierarchicalOcclusionMap.GROUP_SIZE + dy;
        int localZ = groupZ * HierarchicalOcclusionMap.GROUP_SIZE + dz;
        setOccludingViaReflection(cache, localX, localY, localZ, true);
        
        // Create hierarchical map and update from cache
        HierarchicalOcclusionMap hierarchicalMap = new HierarchicalOcclusionMap();
        hierarchicalMap.updateFromLocalCache(cache);
        
        // Verify group is occluding
        assertTrue(hierarchicalMap.isGroupOccluding(groupX, groupY, groupZ),
                String.format("Group (%d,%d,%d) with single occluding block at local(%d,%d,%d) should be occluding",
                        groupX, groupY, groupZ, localX, localY, localZ));
    }

    /**
     * Property: Group bounds checking is correct.
     * 
     * isGroupInBounds should return true for valid group coordinates (0-15) and false otherwise.
     * 
     */
    @Property(tries = 100)
    void groupBoundsCheckingIsCorrect(
            @ForAll("extendedGroupCoordinates") int groupX,
            @ForAll("extendedGroupCoordinates") int groupY,
            @ForAll("extendedGroupCoordinates") int groupZ) {
        
        HierarchicalOcclusionMap hierarchicalMap = new HierarchicalOcclusionMap();
        
        boolean inBounds = hierarchicalMap.isGroupInBounds(groupX, groupY, groupZ);
        
        boolean expectedInBounds = groupX >= 0 && groupX < HierarchicalOcclusionMap.GROUPS_PER_AXIS &&
                                   groupY >= 0 && groupY < HierarchicalOcclusionMap.GROUPS_PER_AXIS &&
                                   groupZ >= 0 && groupZ < HierarchicalOcclusionMap.GROUPS_PER_AXIS;
        
        assertEquals(expectedInBounds, inBounds,
                String.format("Bounds check for group (%d,%d,%d): expected %s, got %s",
                        groupX, groupY, groupZ, expectedInBounds, inBounds));
    }

    /**
     * Property: Local to group coordinate conversion is correct.
     * 
     * toGroupCoord should correctly divide local coordinates by 2.
     * 
     */
    @Property(tries = 100)
    void localToGroupConversionIsCorrect(@ForAll("localCoordinates") int localCoord) {
        int groupCoord = HierarchicalOcclusionMap.toGroupCoord(localCoord);
        
        int expectedGroupCoord = localCoord / HierarchicalOcclusionMap.GROUP_SIZE;
        
        assertEquals(expectedGroupCoord, groupCoord,
                String.format("Local coord %d should map to group coord %d, got %d",
                        localCoord, expectedGroupCoord, groupCoord));
    }

    /**
     * Property: Invalidation clears valid state.
     * 
     * After invalidate() is called, isValid() should return false.
     */
    @Property(tries = 100)
    void invalidationClearsValidState(@ForAll("groupCoordinates") int ignored) {
        HierarchicalOcclusionMap hierarchicalMap = new HierarchicalOcclusionMap();
        
        // Create a valid cache and update the map
        LocalOcclusionCache cache = new LocalOcclusionCache();
        setBaseCoordinates(cache, 0, 0, 0);
        setValid(cache, true);
        hierarchicalMap.updateFromLocalCache(cache);
        
        assertTrue(hierarchicalMap.isValid(), "Map should be valid after update");
        
        hierarchicalMap.invalidate();
        
        assertFalse(hierarchicalMap.isValid(), "Map should be invalid after invalidation");
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
    Arbitrary<Integer> groupCoordinates() {
        return Arbitraries.integers().between(0, HierarchicalOcclusionMap.GROUPS_PER_AXIS - 1);
    }

    @Provide
    Arbitrary<Integer> extendedGroupCoordinates() {
        return Arbitraries.integers().between(-5, HierarchicalOcclusionMap.GROUPS_PER_AXIS + 5);
    }

    @Provide
    Arbitrary<Integer> localCoordinates() {
        return Arbitraries.integers().between(0, LocalOcclusionCache.SIZE - 1);
    }

    @Provide
    Arbitrary<Integer> blockOffset() {
        return Arbitraries.integers().between(0, HierarchicalOcclusionMap.GROUP_SIZE - 1);
    }

    @Provide
    Arbitrary<boolean[]> occlusionPattern() {
        // Generate 8 booleans for the 8 blocks in a 2x2x2 group
        return Arbitraries.of(true, false)
                .list().ofSize(8)
                .map(list -> {
                    boolean[] arr = new boolean[8];
                    for (int i = 0; i < 8; i++) {
                        arr[i] = list.get(i);
                    }
                    return arr;
                });
    }
}
