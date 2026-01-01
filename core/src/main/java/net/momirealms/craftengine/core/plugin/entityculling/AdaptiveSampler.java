package net.momirealms.craftengine.core.plugin.entityculling;

import net.momirealms.craftengine.core.world.MutableVec3d;

/**
 * Adaptive sampler that determines the number of sample points based on distance.
 * Closer entities use more sample points for accurate visibility detection,
 * while distant entities use fewer samples for performance optimization.
 *
 */
public final class AdaptiveSampler {

    // Distance thresholds
    public static final double NEAR_DISTANCE = 16.0;
    public static final double MID_DISTANCE = 48.0;

    // Squared distance thresholds for faster comparison (avoid sqrt)
    public static final double NEAR_DISTANCE_SQ = NEAR_DISTANCE * NEAR_DISTANCE;
    public static final double MID_DISTANCE_SQ = MID_DISTANCE * MID_DISTANCE;

    // Sample counts for each distance range
    public static final int NEAR_SAMPLES = 7;
    public static final int MID_SAMPLES = 3;
    public static final int FAR_SAMPLES = 1;

    // Epsilon for floating point comparison
    private static final double EPSILON = 0.0001;

    /**
     * Gets the number of sample points to use based on squared distance.
     *
     * @param distanceSquared the squared distance from camera to entity
     * @return the number of sample points to use
     *
     */
    public int getSampleCount(double distanceSquared) {
        if (distanceSquared < NEAR_DISTANCE_SQ) {
            return NEAR_SAMPLES; // < 16 blocks: 7 samples
        } else if (distanceSquared < MID_DISTANCE_SQ) {
            return MID_SAMPLES; // 16-48 blocks: 3 samples
        } else {
            return FAR_SAMPLES; // >= 48 blocks: 1 sample (center only)
        }
    }

    /**
     * Selects sample points on the AABB based on camera-relative position.
     * Prioritizes corners and face centers that are facing the camera.
     * Ensures all selected points are distinct.
     *
     * @param targets array to store selected sample points (must have at least 'count' elements)
     * @param count number of sample points to select
     * @param minX minimum X of expanded AABB
     * @param minY minimum Y of expanded AABB
     * @param minZ minimum Z of expanded AABB
     * @param maxX maximum X of expanded AABB
     * @param maxY maximum Y of expanded AABB
     * @param maxZ maximum Z of expanded AABB
     * @param cameraX camera X position
     * @param cameraY camera Y position
     * @param cameraZ camera Z position
     * @return the actual number of sample points selected
     *
     */
    public int selectSamplePoints(
        MutableVec3d[] targets,
        int count,
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ,
        double cameraX,
        double cameraY,
        double cameraZ
    ) {
        if (!isValidInput(targets, count)) {
            return 0;
        }

        // Calculate center point
        double centerX = (minX + maxX) / 2.0;
        double centerY = (minY + maxY) / 2.0;
        double centerZ = (minZ + maxZ) / 2.0;

        // For FAR_SAMPLES (1), just use center
        if (count == 1) {
            targets[0].set(centerX, centerY, centerZ);
            return 1;
        }

        // Determine visible faces from camera position
        VisibleFaces faces = determineVisibleFaces(
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ,
            cameraX,
            cameraY,
            cameraZ
        );

        int selected = 0;

        // Always include center first
        targets[selected++].set(centerX, centerY, centerZ);

        if (selected >= count) {
            return selected;
        }

        // Add face centers for visible faces
        selected = addVisibleFaceCenters(
            targets,
            selected,
            count,
            faces,
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ,
            centerX,
            centerY,
            centerZ
        );

        if (selected >= count) {
            return selected;
        }

        // Add corners facing the camera
        selected = addCameraFacingCorners(
            targets,
            selected,
            count,
            faces,
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ
        );

        return selected;
    }

    /**
     * Validates input parameters.
     */
    private boolean isValidInput(MutableVec3d[] targets, int count) {
        return count > 0 && targets != null && targets.length > 0;
    }

    /**
     * Determines which faces of the AABB are visible from the camera position.
     */
    private VisibleFaces determineVisibleFaces(
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ,
        double cameraX,
        double cameraY,
        double cameraZ
    ) {
        return new VisibleFaces(
            cameraX < minX, // xPositive: Camera sees the minX face
            cameraX > maxX, // xNegative: Camera sees the maxX face
            cameraY < minY, // yPositive: Camera sees the minY face
            cameraY > maxY, // yNegative: Camera sees the maxY face
            cameraZ < minZ, // zPositive: Camera sees the minZ face
            cameraZ > maxZ // zNegative: Camera sees the maxZ face
        );
    }

    /**
     * Adds face centers for visible faces to the targets array.
     *
     * @return the updated selected count
     */
    private int addVisibleFaceCenters(
        MutableVec3d[] targets,
        int selected,
        int count,
        VisibleFaces faces,
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ,
        double centerX,
        double centerY,
        double centerZ
    ) {
        if (selected < count && faces.xPositive) {
            targets[selected++].set(minX, centerY, centerZ);
        }
        if (selected < count && faces.xNegative) {
            targets[selected++].set(maxX, centerY, centerZ);
        }
        if (selected < count && faces.yPositive) {
            targets[selected++].set(centerX, minY, centerZ);
        }
        if (selected < count && faces.yNegative) {
            targets[selected++].set(centerX, maxY, centerZ);
        }
        if (selected < count && faces.zPositive) {
            targets[selected++].set(centerX, centerY, minZ);
        }
        if (selected < count && faces.zNegative) {
            targets[selected++].set(centerX, centerY, maxZ);
        }

        return selected;
    }

    /**
     * Adds corners facing the camera to the targets array.
     *
     * @return the updated selected count
     */
    private int addCameraFacingCorners(
        MutableVec3d[] targets,
        int selected,
        int count,
        VisibleFaces faces,
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ
    ) {
        // Select coordinates for camera-facing corner
        double cornerX = faces.xPositive
            ? minX
            : (faces.xNegative ? maxX : minX);
        double cornerY = faces.yPositive
            ? minY
            : (faces.yNegative ? maxY : minY);
        double cornerZ = faces.zPositive
            ? minZ
            : (faces.zNegative ? maxZ : minZ);

        // Add first corner
        if (
            selected < count &&
            !isDuplicate(targets, selected, cornerX, cornerY, cornerZ)
        ) {
            targets[selected++].set(cornerX, cornerY, cornerZ);
        }

        // Define additional corners to try
        double[][] additionalCorners = buildAdditionalCorners(
            faces,
            minX,
            minY,
            minZ,
            maxX,
            maxY,
            maxZ
        );

        for (double[] corner : additionalCorners) {
            if (selected >= count) {
                break;
            }
            if (
                !isDuplicate(targets, selected, corner[0], corner[1], corner[2])
            ) {
                targets[selected++].set(corner[0], corner[1], corner[2]);
            }
        }

        return selected;
    }

    /**
     * Builds an array of additional corner coordinates to try.
     */
    private double[][] buildAdditionalCorners(
        VisibleFaces faces,
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ
    ) {
        return new double[][] {
            {
                faces.xPositive ? minX : maxX,
                faces.yPositive ? minY : maxY,
                faces.zPositive ? minZ : maxZ,
            },
            {
                faces.xPositive ? minX : maxX,
                faces.yPositive ? minY : maxY,
                faces.zNegative ? maxZ : minZ,
            },
            {
                faces.xPositive ? minX : maxX,
                faces.yNegative ? maxY : minY,
                faces.zPositive ? minZ : maxZ,
            },
            {
                faces.xNegative ? maxX : minX,
                faces.yPositive ? minY : maxY,
                faces.zPositive ? minZ : maxZ,
            },
        };
    }

    /**
     * Checks if a point already exists in the targets array.
     */
    private boolean isDuplicate(
        MutableVec3d[] targets,
        int count,
        double x,
        double y,
        double z
    ) {
        for (int i = 0; i < count; i++) {
            if (
                Math.abs(targets[i].x - x) < EPSILON &&
                Math.abs(targets[i].y - y) < EPSILON &&
                Math.abs(targets[i].z - z) < EPSILON
            ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Calculates the squared distance from camera to the nearest point on the AABB.
     *
     * @param minX minimum X of AABB
     * @param minY minimum Y of AABB
     * @param minZ minimum Z of AABB
     * @param maxX maximum X of AABB
     * @param maxY maximum Y of AABB
     * @param maxZ maximum Z of AABB
     * @param cameraX camera X position
     * @param cameraY camera Y position
     * @param cameraZ camera Z position
     * @return squared distance from camera to nearest point on AABB
     */
    public double calculateDistanceSquared(
        double minX,
        double minY,
        double minZ,
        double maxX,
        double maxY,
        double maxZ,
        double cameraX,
        double cameraY,
        double cameraZ
    ) {
        double dx = calculateAxisDistance(cameraX, minX, maxX);
        double dy = calculateAxisDistance(cameraY, minY, maxY);
        double dz = calculateAxisDistance(cameraZ, minZ, maxZ);

        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Calculates the distance along a single axis from a point to a range.
     */
    private double calculateAxisDistance(double point, double min, double max) {
        if (point < min) {
            return min - point;
        } else if (point > max) {
            return point - max;
        }
        return 0;
    }

    /**
     * Record to hold visible face information.
     */
    private record VisibleFaces(
        boolean xPositive,
        boolean xNegative,
        boolean yPositive,
        boolean yNegative,
        boolean zPositive,
        boolean zNegative
    ) {}
}
