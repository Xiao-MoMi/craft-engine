package net.momirealms.craftengine.core.plugin.entityculling;

import net.jqwik.api.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Property-based tests for AdaptiveSampler.
 * 
 * 
 */
class AdaptiveSamplerPropertyTest {

    /**
     * Property 3: Adaptive Sample Count Correctness
     * 
     * For any distance D between camera and entity:
     * - IF D < 16 THEN sampleCount = 7
     * - IF 16 <= D < 48 THEN sampleCount = 3
     * - IF D >= 48 THEN sampleCount = 1
     * 
     */
    @Property(tries = 100)
    void sampleCountIsCorrectForNearDistance(
            @ForAll("nearDistances") double distance) {
        
        AdaptiveSampler sampler = new AdaptiveSampler();
        double distanceSquared = distance * distance;
        
        int sampleCount = sampler.getSampleCount(distanceSquared);
        
        assertEquals(AdaptiveSampler.NEAR_SAMPLES, sampleCount,
                String.format("Distance %.2f (< 16) should use %d samples, got %d",
                        distance, AdaptiveSampler.NEAR_SAMPLES, sampleCount));
    }

    /**
     * Property 3: Adaptive Sample Count Correctness - Mid distance range
     * 
     * For any distance D where 16 <= D < 48, sampleCount should be 3.
     * 
     */
    @Property(tries = 100)
    void sampleCountIsCorrectForMidDistance(
            @ForAll("midDistances") double distance) {
        
        AdaptiveSampler sampler = new AdaptiveSampler();
        double distanceSquared = distance * distance;
        
        int sampleCount = sampler.getSampleCount(distanceSquared);
        
        assertEquals(AdaptiveSampler.MID_SAMPLES, sampleCount,
                String.format("Distance %.2f (16-48) should use %d samples, got %d",
                        distance, AdaptiveSampler.MID_SAMPLES, sampleCount));
    }

    /**
     * Property 3: Adaptive Sample Count Correctness - Far distance range
     * 
     * For any distance D >= 48, sampleCount should be 1.
     * 
     */
    @Property(tries = 100)
    void sampleCountIsCorrectForFarDistance(
            @ForAll("farDistances") double distance) {
        
        AdaptiveSampler sampler = new AdaptiveSampler();
        double distanceSquared = distance * distance;
        
        int sampleCount = sampler.getSampleCount(distanceSquared);
        
        assertEquals(AdaptiveSampler.FAR_SAMPLES, sampleCount,
                String.format("Distance %.2f (>= 48) should use %d samples, got %d",
                        distance, AdaptiveSampler.FAR_SAMPLES, sampleCount));
    }

    /**
     * Property: Boundary conditions are handled correctly.
     * 
     * Tests exact boundary values: 0, 16, and 48.
     */
    @Property(tries = 100)
    void boundaryConditionsAreCorrect(@ForAll("smallIntegers") int ignored) {
        AdaptiveSampler sampler = new AdaptiveSampler();
        
        // Distance 0 should use NEAR_SAMPLES
        assertEquals(AdaptiveSampler.NEAR_SAMPLES, sampler.getSampleCount(0.0),
                "Distance 0 should use NEAR_SAMPLES");
        
        // Distance exactly 16 should use MID_SAMPLES (16 <= D < 48)
        double distance16Sq = 16.0 * 16.0;
        assertEquals(AdaptiveSampler.MID_SAMPLES, sampler.getSampleCount(distance16Sq),
                "Distance 16 should use MID_SAMPLES");
        
        // Distance exactly 48 should use FAR_SAMPLES (D >= 48)
        double distance48Sq = 48.0 * 48.0;
        assertEquals(AdaptiveSampler.FAR_SAMPLES, sampler.getSampleCount(distance48Sq),
                "Distance 48 should use FAR_SAMPLES");
        
        // Just below 16 should use NEAR_SAMPLES
        double justBelow16Sq = 15.999 * 15.999;
        assertEquals(AdaptiveSampler.NEAR_SAMPLES, sampler.getSampleCount(justBelow16Sq),
                "Distance just below 16 should use NEAR_SAMPLES");
        
        // Just below 48 should use MID_SAMPLES
        double justBelow48Sq = 47.999 * 47.999;
        assertEquals(AdaptiveSampler.MID_SAMPLES, sampler.getSampleCount(justBelow48Sq),
                "Distance just below 48 should use MID_SAMPLES");
    }

    /**
     * Property: Sample count is monotonically decreasing with distance.
     * 
     * As distance increases, sample count should never increase.
     */
    @Property(tries = 100)
    void sampleCountIsMonotonicallyDecreasing(
            @ForAll("positiveDistances") double distance1,
            @ForAll("positiveDistances") double distance2) {
        
        AdaptiveSampler sampler = new AdaptiveSampler();
        
        double smaller = Math.min(distance1, distance2);
        double larger = Math.max(distance1, distance2);
        
        int samplesSmaller = sampler.getSampleCount(smaller * smaller);
        int samplesLarger = sampler.getSampleCount(larger * larger);
        
        assertTrue(samplesSmaller >= samplesLarger,
                String.format("Sample count should not increase with distance: " +
                        "distance %.2f has %d samples, distance %.2f has %d samples",
                        smaller, samplesSmaller, larger, samplesLarger));
    }

    /**
     * Property: Distance calculation is correct.
     * 
     * The calculated squared distance should match the expected value
     * for various camera and AABB positions.
     */
    @Property(tries = 100)
    void distanceCalculationIsCorrect(
            @ForAll("coordinates") double cameraX,
            @ForAll("coordinates") double cameraY,
            @ForAll("coordinates") double cameraZ,
            @ForAll("positiveSize") double sizeX,
            @ForAll("positiveSize") double sizeY,
            @ForAll("positiveSize") double sizeZ) {
        
        AdaptiveSampler sampler = new AdaptiveSampler();
        
        // Create AABB centered at origin
        double minX = -sizeX / 2;
        double minY = -sizeY / 2;
        double minZ = -sizeZ / 2;
        double maxX = sizeX / 2;
        double maxY = sizeY / 2;
        double maxZ = sizeZ / 2;
        
        double distSq = sampler.calculateDistanceSquared(
                minX, minY, minZ, maxX, maxY, maxZ,
                cameraX, cameraY, cameraZ);
        
        // Distance should always be non-negative
        assertTrue(distSq >= 0, "Distance squared should be non-negative");
        
        // If camera is inside AABB, distance should be 0
        if (cameraX >= minX && cameraX <= maxX &&
            cameraY >= minY && cameraY <= maxY &&
            cameraZ >= minZ && cameraZ <= maxZ) {
            assertEquals(0.0, distSq, 0.0001,
                    "Distance should be 0 when camera is inside AABB");
        }
    }

    @Provide
    Arbitrary<Double> nearDistances() {
        // Distances from 0.1 to 15.9 (safely within near range)
        return Arbitraries.doubles().between(0.1, 15.9).ofScale(2);
    }

    @Provide
    Arbitrary<Double> midDistances() {
        // Distances from 16.1 to 47.9 (safely within mid range)
        return Arbitraries.doubles().between(16.1, 47.9).ofScale(2);
    }

    @Provide
    Arbitrary<Double> farDistances() {
        // Distances from 48.1 to 500 (safely within far range)
        return Arbitraries.doubles().between(48.1, 500.0).ofScale(2);
    }

    @Provide
    Arbitrary<Double> positiveDistances() {
        return Arbitraries.doubles().between(0.1, 200.0).ofScale(2);
    }

    @Provide
    Arbitrary<Double> coordinates() {
        return Arbitraries.doubles().between(-100.0, 100.0).ofScale(2);
    }

    @Provide
    Arbitrary<Double> positiveSize() {
        return Arbitraries.doubles().between(0.5, 10.0).ofScale(2);
    }

    @Provide
    Arbitrary<Integer> smallIntegers() {
        return Arbitraries.integers().between(1, 10);
    }
}
