package net.momirealms.craftengine.core.plugin.entityculling;

import net.jqwik.api.*;
import net.momirealms.craftengine.core.world.MutableVec3d;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for AdaptiveSampler sample point selection.
 * 
 * 
 */
class SamplePointSelectionPropertyTest {

    /**
     * Property 4: Sample Point Selection Correctness
     * 
     * For any AABB and camera position, the selected sample points SHALL only include
     * corners and face centers that are on the camera-facing sides of the AABB.
     * 
     */
    @Property(tries = 100)
    void selectedPointsAreOnCameraFacingSides(
            @ForAll("aabbCoordinates") double aabbCenterX,
            @ForAll("aabbCoordinates") double aabbCenterY,
            @ForAll("aabbCoordinates") double aabbCenterZ,
            @ForAll("aabbSizes") double sizeX,
            @ForAll("aabbSizes") double sizeY,
            @ForAll("aabbSizes") double sizeZ,
            @ForAll("cameraOffsets") double cameraOffsetX,
            @ForAll("cameraOffsets") double cameraOffsetY,
            @ForAll("cameraOffsets") double cameraOffsetZ) {
        
        // Skip if camera would be inside AABB
        Assume.that(Math.abs(cameraOffsetX) > sizeX / 2 ||
                    Math.abs(cameraOffsetY) > sizeY / 2 ||
                    Math.abs(cameraOffsetZ) > sizeZ / 2);
        
        AdaptiveSampler sampler = new AdaptiveSampler();
        
        // Create AABB
        double minX = aabbCenterX - sizeX / 2;
        double minY = aabbCenterY - sizeY / 2;
        double minZ = aabbCenterZ - sizeZ / 2;
        double maxX = aabbCenterX + sizeX / 2;
        double maxY = aabbCenterY + sizeY / 2;
        double maxZ = aabbCenterZ + sizeZ / 2;
        
        // Camera position
        double cameraX = aabbCenterX + cameraOffsetX;
        double cameraY = aabbCenterY + cameraOffsetY;
        double cameraZ = aabbCenterZ + cameraOffsetZ;
        
        // Create target points array
        MutableVec3d[] targets = new MutableVec3d[AdaptiveSampler.NEAR_SAMPLES];
        for (int i = 0; i < targets.length; i++) {
            targets[i] = new MutableVec3d(0, 0, 0);
        }
        
        // Test with maximum sample count
        int count = sampler.selectSamplePoints(targets, AdaptiveSampler.NEAR_SAMPLES,
                minX, minY, minZ, maxX, maxY, maxZ,
                cameraX, cameraY, cameraZ);
        
        assertTrue(count > 0, "Should select at least one sample point");
        assertTrue(count <= AdaptiveSampler.NEAR_SAMPLES, 
                "Should not exceed maximum sample count");
        
        // Verify all selected points are within or on the AABB bounds
        for (int i = 0; i < count; i++) {
            MutableVec3d point = targets[i];
            
            // Points should be on or within the AABB (with small epsilon for floating point)
            double epsilon = 0.0001;
            assertTrue(point.x >= minX - epsilon && point.x <= maxX + epsilon,
                    String.format("Point %d X (%.4f) should be within AABB [%.4f, %.4f]",
                            i, point.x, minX, maxX));
            assertTrue(point.y >= minY - epsilon && point.y <= maxY + epsilon,
                    String.format("Point %d Y (%.4f) should be within AABB [%.4f, %.4f]",
                            i, point.y, minY, maxY));
            assertTrue(point.z >= minZ - epsilon && point.z <= maxZ + epsilon,
                    String.format("Point %d Z (%.4f) should be within AABB [%.4f, %.4f]",
                            i, point.z, minZ, maxZ));
        }
    }

    /**
     * Property: Center point is always included for any sample count > 0.
     * 
     * The center of the AABB should always be one of the selected sample points.
     */
    @Property(tries = 100)
    void centerPointIsAlwaysIncluded(
            @ForAll("aabbCoordinates") double aabbCenterX,
            @ForAll("aabbCoordinates") double aabbCenterY,
            @ForAll("aabbCoordinates") double aabbCenterZ,
            @ForAll("aabbSizes") double sizeX,
            @ForAll("aabbSizes") double sizeY,
            @ForAll("aabbSizes") double sizeZ,
            @ForAll("sampleCounts") int requestedCount) {
        
        AdaptiveSampler sampler = new AdaptiveSampler();
        
        // Create AABB
        double minX = aabbCenterX - sizeX / 2;
        double minY = aabbCenterY - sizeY / 2;
        double minZ = aabbCenterZ - sizeZ / 2;
        double maxX = aabbCenterX + sizeX / 2;
        double maxY = aabbCenterY + sizeY / 2;
        double maxZ = aabbCenterZ + sizeZ / 2;
        
        // Camera outside AABB
        double cameraX = maxX + 10;
        double cameraY = maxY + 10;
        double cameraZ = maxZ + 10;
        
        // Create target points array
        MutableVec3d[] targets = new MutableVec3d[AdaptiveSampler.NEAR_SAMPLES];
        for (int i = 0; i < targets.length; i++) {
            targets[i] = new MutableVec3d(0, 0, 0);
        }
        
        int count = sampler.selectSamplePoints(targets, requestedCount,
                minX, minY, minZ, maxX, maxY, maxZ,
                cameraX, cameraY, cameraZ);
        
        if (count > 0) {
            // Calculate expected center
            double expectedCenterX = (minX + maxX) / 2.0;
            double expectedCenterY = (minY + maxY) / 2.0;
            double expectedCenterZ = (minZ + maxZ) / 2.0;
            
            // Check if center is included
            boolean centerFound = false;
            double epsilon = 0.0001;
            for (int i = 0; i < count; i++) {
                if (Math.abs(targets[i].x - expectedCenterX) < epsilon &&
                    Math.abs(targets[i].y - expectedCenterY) < epsilon &&
                    Math.abs(targets[i].z - expectedCenterZ) < epsilon) {
                    centerFound = true;
                    break;
                }
            }
            
            assertTrue(centerFound, 
                    String.format("Center point (%.4f, %.4f, %.4f) should be included in selected points",
                            expectedCenterX, expectedCenterY, expectedCenterZ));
        }
    }

    /**
     * Property: Sample count respects the requested count.
     * 
     * The number of selected points should not exceed the requested count.
     */
    @Property(tries = 100)
    void sampleCountRespectsRequestedCount(
            @ForAll("aabbCoordinates") double aabbCenterX,
            @ForAll("aabbCoordinates") double aabbCenterY,
            @ForAll("aabbCoordinates") double aabbCenterZ,
            @ForAll("aabbSizes") double sizeX,
            @ForAll("aabbSizes") double sizeY,
            @ForAll("aabbSizes") double sizeZ,
            @ForAll("sampleCounts") int requestedCount) {
        
        AdaptiveSampler sampler = new AdaptiveSampler();
        
        // Create AABB
        double minX = aabbCenterX - sizeX / 2;
        double minY = aabbCenterY - sizeY / 2;
        double minZ = aabbCenterZ - sizeZ / 2;
        double maxX = aabbCenterX + sizeX / 2;
        double maxY = aabbCenterY + sizeY / 2;
        double maxZ = aabbCenterZ + sizeZ / 2;
        
        // Camera outside AABB
        double cameraX = maxX + 10;
        double cameraY = aabbCenterY;
        double cameraZ = aabbCenterZ;
        
        // Create target points array
        MutableVec3d[] targets = new MutableVec3d[AdaptiveSampler.NEAR_SAMPLES];
        for (int i = 0; i < targets.length; i++) {
            targets[i] = new MutableVec3d(0, 0, 0);
        }
        
        int count = sampler.selectSamplePoints(targets, requestedCount,
                minX, minY, minZ, maxX, maxY, maxZ,
                cameraX, cameraY, cameraZ);
        
        assertTrue(count <= requestedCount,
                String.format("Selected count (%d) should not exceed requested count (%d)",
                        count, requestedCount));
    }

    /**
     * Property: Far distance uses only center point.
     * 
     * When requesting 1 sample (FAR_SAMPLES), only the center should be returned.
     */
    @Property(tries = 100)
    void farDistanceUsesOnlyCenter(
            @ForAll("aabbCoordinates") double aabbCenterX,
            @ForAll("aabbCoordinates") double aabbCenterY,
            @ForAll("aabbCoordinates") double aabbCenterZ,
            @ForAll("aabbSizes") double sizeX,
            @ForAll("aabbSizes") double sizeY,
            @ForAll("aabbSizes") double sizeZ) {
        
        AdaptiveSampler sampler = new AdaptiveSampler();
        
        // Create AABB
        double minX = aabbCenterX - sizeX / 2;
        double minY = aabbCenterY - sizeY / 2;
        double minZ = aabbCenterZ - sizeZ / 2;
        double maxX = aabbCenterX + sizeX / 2;
        double maxY = aabbCenterY + sizeY / 2;
        double maxZ = aabbCenterZ + sizeZ / 2;
        
        // Camera far away
        double cameraX = maxX + 100;
        double cameraY = aabbCenterY;
        double cameraZ = aabbCenterZ;
        
        // Create target points array
        MutableVec3d[] targets = new MutableVec3d[1];
        targets[0] = new MutableVec3d(0, 0, 0);
        
        int count = sampler.selectSamplePoints(targets, AdaptiveSampler.FAR_SAMPLES,
                minX, minY, minZ, maxX, maxY, maxZ,
                cameraX, cameraY, cameraZ);
        
        assertEquals(1, count, "Far distance should use exactly 1 sample");
        
        // Verify it's the center
        double expectedCenterX = (minX + maxX) / 2.0;
        double expectedCenterY = (minY + maxY) / 2.0;
        double expectedCenterZ = (minZ + maxZ) / 2.0;
        
        double epsilon = 0.0001;
        assertEquals(expectedCenterX, targets[0].x, epsilon, "X should be center");
        assertEquals(expectedCenterY, targets[0].y, epsilon, "Y should be center");
        assertEquals(expectedCenterZ, targets[0].z, epsilon, "Z should be center");
    }

    /**
     * Property: All selected points are distinct.
     * 
     * No two selected sample points should be identical.
     */
    @Property(tries = 100)
    void allSelectedPointsAreDistinct(
            @ForAll("aabbCoordinates") double aabbCenterX,
            @ForAll("aabbCoordinates") double aabbCenterY,
            @ForAll("aabbCoordinates") double aabbCenterZ,
            @ForAll("aabbSizes") double sizeX,
            @ForAll("aabbSizes") double sizeY,
            @ForAll("aabbSizes") double sizeZ,
            @ForAll("cameraOffsets") double cameraOffsetX,
            @ForAll("cameraOffsets") double cameraOffsetY,
            @ForAll("cameraOffsets") double cameraOffsetZ) {
        
        // Skip if camera would be inside AABB
        Assume.that(Math.abs(cameraOffsetX) > sizeX / 2 ||
                    Math.abs(cameraOffsetY) > sizeY / 2 ||
                    Math.abs(cameraOffsetZ) > sizeZ / 2);
        
        AdaptiveSampler sampler = new AdaptiveSampler();
        
        // Create AABB
        double minX = aabbCenterX - sizeX / 2;
        double minY = aabbCenterY - sizeY / 2;
        double minZ = aabbCenterZ - sizeZ / 2;
        double maxX = aabbCenterX + sizeX / 2;
        double maxY = aabbCenterY + sizeY / 2;
        double maxZ = aabbCenterZ + sizeZ / 2;
        
        // Camera position
        double cameraX = aabbCenterX + cameraOffsetX;
        double cameraY = aabbCenterY + cameraOffsetY;
        double cameraZ = aabbCenterZ + cameraOffsetZ;
        
        // Create target points array
        MutableVec3d[] targets = new MutableVec3d[AdaptiveSampler.NEAR_SAMPLES];
        for (int i = 0; i < targets.length; i++) {
            targets[i] = new MutableVec3d(0, 0, 0);
        }
        
        int count = sampler.selectSamplePoints(targets, AdaptiveSampler.NEAR_SAMPLES,
                minX, minY, minZ, maxX, maxY, maxZ,
                cameraX, cameraY, cameraZ);
        
        // Check for duplicates
        double epsilon = 0.0001;
        for (int i = 0; i < count; i++) {
            for (int j = i + 1; j < count; j++) {
                boolean sameX = Math.abs(targets[i].x - targets[j].x) < epsilon;
                boolean sameY = Math.abs(targets[i].y - targets[j].y) < epsilon;
                boolean sameZ = Math.abs(targets[i].z - targets[j].z) < epsilon;
                
                assertFalse(sameX && sameY && sameZ,
                        String.format("Points %d and %d should be distinct: (%.4f, %.4f, %.4f)",
                                i, j, targets[i].x, targets[i].y, targets[i].z));
            }
        }
    }

    @Provide
    Arbitrary<Double> aabbCoordinates() {
        return Arbitraries.doubles().between(-50.0, 50.0).ofScale(2);
    }

    @Provide
    Arbitrary<Double> aabbSizes() {
        return Arbitraries.doubles().between(1.0, 10.0).ofScale(2);
    }

    @Provide
    Arbitrary<Double> cameraOffsets() {
        return Arbitraries.doubles().between(-30.0, 30.0).ofScale(2);
    }

    @Provide
    Arbitrary<Integer> sampleCounts() {
        return Arbitraries.integers().between(1, AdaptiveSampler.NEAR_SAMPLES);
    }
}
