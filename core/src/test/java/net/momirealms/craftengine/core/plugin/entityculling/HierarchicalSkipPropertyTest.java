package net.momirealms.craftengine.core.plugin.entityculling;

import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for Hierarchical Skip functionality.
 * 
 */
class HierarchicalSkipPropertyTest {

    /**
     * Property 8: Hierarchical Skip Correctness
     * 
     * For any ray traversal through a non-occluding 2x2x2 group, the DDA algorithm 
     * SHALL advance by at least 2 blocks in a single step.
     * 
     * This test verifies that getSkipDistance returns 2 for non-occluding groups
     * and 1 for occluding groups.
     * 
     */
    @Property(tries = 100)
    void skipDistanceIsCorrectForNonOccludingGroups(
            @ForAll("groupCoordinates") int groupX,
            @ForAll("groupCoordinates") int groupY,
            @ForAll("groupCoordinates") int groupZ) {
        
        // Create cache with no occluding blocks (empty group)
        LocalOcclusionCache cache = new LocalOcclusionCache();
        setBaseCoordinates(cache, 0, 0, 0);
        setValid(cache, true);
        
        // Create hierarchical map and update from cache
        HierarchicalOcclusionMap hierarchicalMap = new HierarchicalOcclusionMap();
        hierarchicalMap.updateFromLocalCache(cache);
        
        // Verify skip distance is 2 for non-occluding group
        int skipDistance = hierarchicalMap.getSkipDistance(groupX, groupY, groupZ);
        
        assertEquals(HierarchicalOcclusionMap.GROUP_SIZE, skipDistance,
                String.format("Non-occluding group (%d,%d,%d) should have skip distance of %d, got %d",
                        groupX, groupY, groupZ, HierarchicalOcclusionMap.GROUP_SIZE, skipDistance));
    }

    /**
     * Property: Skip distance is 1 for occluding groups.
     * 
     * When a group contains at least one occluding block, the skip distance should be 1
     * (must check block by block).
     * 
     */
    @Property(tries = 100)
    void skipDistanceIsOneForOccludingGroups(
            @ForAll("groupCoordinates") int groupX,
            @ForAll("groupCoordinates") int groupY,
            @ForAll("groupCoordinates") int groupZ,
            @ForAll("blockOffset") int dx,
            @ForAll("blockOffset") int dy,
            @ForAll("blockOffset") int dz) {
        
        // Create cache with single occluding block in the group
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
        
        // Verify skip distance is 1 for occluding group
        int skipDistance = hierarchicalMap.getSkipDistance(groupX, groupY, groupZ);
        
        assertEquals(1, skipDistance,
                String.format("Occluding group (%d,%d,%d) should have skip distance of 1, got %d",
                        groupX, groupY, groupZ, skipDistance));
    }


    /**
     * Property: Skip distance matches occlusion state.
     * 
     * For any group with a random occlusion pattern, the skip distance should be:
     * - 2 if no blocks are occluding
     * - 1 if any block is occluding
     * 
     */
    @Property(tries = 100)
    void skipDistanceMatchesOcclusionState(
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
        
        // Verify skip distance matches expected value
        int skipDistance = hierarchicalMap.getSkipDistance(groupX, groupY, groupZ);
        int expectedSkipDistance = anyOccluding ? 1 : HierarchicalOcclusionMap.GROUP_SIZE;
        
        assertEquals(expectedSkipDistance, skipDistance,
                String.format("Group (%d,%d,%d) skip distance mismatch: expected %d (anyOccluding=%s), got %d",
                        groupX, groupY, groupZ, expectedSkipDistance, anyOccluding, skipDistance));
    }

    /**
     * Property: Adjacent groups have independent skip distances.
     * 
     * Setting occlusion in one group should not affect the skip distance of adjacent groups.
     * 
     */
    @Property(tries = 100)
    void adjacentGroupsHaveIndependentSkipDistances(
            @ForAll("innerGroupCoordinates") int groupX,
            @ForAll("innerGroupCoordinates") int groupY,
            @ForAll("innerGroupCoordinates") int groupZ) {
        
        // Create cache with single occluding block in the target group
        LocalOcclusionCache cache = new LocalOcclusionCache();
        setBaseCoordinates(cache, 0, 0, 0);
        setValid(cache, true);
        
        // Set a block in the target group to occluding
        int localX = groupX * HierarchicalOcclusionMap.GROUP_SIZE;
        int localY = groupY * HierarchicalOcclusionMap.GROUP_SIZE;
        int localZ = groupZ * HierarchicalOcclusionMap.GROUP_SIZE;
        setOccludingViaReflection(cache, localX, localY, localZ, true);
        
        // Create hierarchical map and update from cache
        HierarchicalOcclusionMap hierarchicalMap = new HierarchicalOcclusionMap();
        hierarchicalMap.updateFromLocalCache(cache);
        
        // Verify target group has skip distance 1
        assertEquals(1, hierarchicalMap.getSkipDistance(groupX, groupY, groupZ),
                "Target group should have skip distance 1");
        
        // Verify adjacent groups (if in bounds) have skip distance 2
        if (groupX + 1 < HierarchicalOcclusionMap.GROUPS_PER_AXIS) {
            assertEquals(HierarchicalOcclusionMap.GROUP_SIZE, 
                    hierarchicalMap.getSkipDistance(groupX + 1, groupY, groupZ),
                    "Adjacent group (+X) should have skip distance 2");
        }
        if (groupY + 1 < HierarchicalOcclusionMap.GROUPS_PER_AXIS) {
            assertEquals(HierarchicalOcclusionMap.GROUP_SIZE, 
                    hierarchicalMap.getSkipDistance(groupX, groupY + 1, groupZ),
                    "Adjacent group (+Y) should have skip distance 2");
        }
        if (groupZ + 1 < HierarchicalOcclusionMap.GROUPS_PER_AXIS) {
            assertEquals(HierarchicalOcclusionMap.GROUP_SIZE, 
                    hierarchicalMap.getSkipDistance(groupX, groupY, groupZ + 1),
                    "Adjacent group (+Z) should have skip distance 2");
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
    Arbitrary<Integer> groupCoordinates() {
        return Arbitraries.integers().between(0, HierarchicalOcclusionMap.GROUPS_PER_AXIS - 1);
    }

    @Provide
    Arbitrary<Integer> innerGroupCoordinates() {
        // Use inner coordinates to ensure adjacent groups are also in bounds
        return Arbitraries.integers().between(0, HierarchicalOcclusionMap.GROUPS_PER_AXIS - 2);
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
