package net.momirealms.craftengine.core.world.collision;

import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.world.BlockPos;
import net.momirealms.craftengine.core.world.EntityHitResult;
import net.momirealms.craftengine.core.world.Position;
import net.momirealms.craftengine.core.world.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AABB {
    private static final double EPSILON = 1.0E-7;
    public final double minX;
    public final double minY;
    public final double minZ;
    public final double maxX;
    public final double maxY;
    public final double maxZ;

    public AABB(double x1, double y1, double z1, double x2, double y2, double z2) {
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public AABB(Vec3d pos1, Vec3d pos2) {
        this.minX = Math.min(pos1.x, pos2.x);
        this.minY = Math.min(pos1.y, pos2.y);
        this.minZ = Math.min(pos1.z, pos2.z);
        this.maxX = Math.max(pos1.x, pos2.x);
        this.maxY = Math.max(pos1.y, pos2.y);
        this.maxZ = Math.max(pos1.z, pos2.z);
    }

    public AABB move(BlockPos pos) {
        return new AABB(
                this.minX + pos.x + 0.5,
                this.minY + pos.y + 0.5,
                this.minZ + pos.z + 0.5,
                this.maxX + pos.x + 0.5,
                this.maxY + pos.y + 0.5,
                this.maxZ + pos.z + 0.5
        );
    }

    public AABB(BlockPos pos) {
        this(pos.x(), pos.y(), pos.z(), pos.x() + 1, pos.y() + 1, pos.z() + 1);
    }

    public double distanceToSqr(Vec3d vec) {
        double x = Math.max(Math.max(this.minX - vec.x, vec.x - this.maxX), 0.0F);
        double y = Math.max(Math.max(this.minY - vec.y, vec.y - this.maxY), 0.0F);
        double z = Math.max(Math.max(this.minZ - vec.z, vec.z - this.maxZ), 0.0F);
        return x * x + y * y + z * z;
    }

    public static AABB makeBoundingBox(Position pos, double width, double height) {
        return new AABB(
            pos.x() - width / 2,
            pos.y(),
            pos.z() - width / 2,
            pos.x() + width / 2,
            pos.y() + height,
            pos.z() + width / 2
        );
    }

    public static AABB makeBoundingBox(Vector3f pos, double width, double height) {
        return new AABB(
                pos.x - width / 2,
                pos.y,
                pos.z - width / 2,
                pos.x + width / 2,
                pos.y + height,
                pos.z + width / 2
        );
    }

    public Optional<EntityHitResult> clip(Vec3d min, Vec3d max) {
        double[] traceDistance = {1.0};
        double deltaX = max.x - min.x;
        double deltaY = max.y - min.y;
        double deltaZ = max.z - min.z;

        Direction direction = calculateCollisionDirection(min, traceDistance, deltaX, deltaY, deltaZ);
        return direction != null
                ? Optional.of(new EntityHitResult(direction, min.add(traceDistance[0] * deltaX, traceDistance[0] * deltaY, traceDistance[0] * deltaZ)))
                : Optional.empty();
    }

    private Direction calculateCollisionDirection(Vec3d intersectingVector, double[] traceDistance, double deltaX, double deltaY, double deltaZ) {
        Direction direction = null;

        // Check each axis for potential collision
        direction = checkAxis(deltaX, deltaY, deltaZ, Direction.WEST, Direction.EAST,
                minX, maxX, intersectingVector.x, intersectingVector.y, intersectingVector.z,
                minY, maxY, minZ, maxZ, traceDistance, direction);

        direction = checkAxis(deltaY, deltaZ, deltaX, Direction.DOWN, Direction.UP,
                minY, maxY, intersectingVector.y, intersectingVector.z, intersectingVector.x,
                minZ, maxZ, minX, maxX, traceDistance, direction);

        direction = checkAxis(deltaZ, deltaX, deltaY, Direction.NORTH, Direction.SOUTH,
                minZ, maxZ, intersectingVector.z, intersectingVector.x, intersectingVector.y,
                minX, maxX, minY, maxY, traceDistance, direction);

        return direction;
    }

    private Direction checkAxis(double primaryDelta, double secondary1Delta, double secondary2Delta,
                                Direction positiveDir, Direction negativeDir,
                                double positiveFace, double negativeFace,
                                double startPrimary, double startSecondary1, double startSecondary2,
                                double secondary1Min, double secondary1Max,
                                double secondary2Min, double secondary2Max,
                                double[] traceDistance, @Nullable Direction currentDir) {
        if (primaryDelta > EPSILON) {
            return checkFace(traceDistance, currentDir, positiveFace,
                    primaryDelta, secondary1Delta, secondary2Delta,
                    secondary1Min, secondary1Max, secondary2Min, secondary2Max,
                    positiveDir, startPrimary, startSecondary1, startSecondary2);
        } else if (primaryDelta < -EPSILON) {
            return checkFace(traceDistance, currentDir, negativeFace,
                    primaryDelta, secondary1Delta, secondary2Delta,
                    secondary1Min, secondary1Max, secondary2Min, secondary2Max,
                    negativeDir, startPrimary, startSecondary1, startSecondary2);
        }
        return currentDir;
    }

    private static Direction checkFace(double[] traceDistance, @Nullable Direction currentDir,
                                       double facePosition,
                                       double primaryDelta, double secondary1Delta, double secondary2Delta,
                                       double secondary1Min, double secondary1Max,
                                       double secondary2Min, double secondary2Max,
                                       Direction direction,
                                       double startPrimary, double startSecondary1, double startSecondary2) {
        double d = (facePosition - startPrimary) / primaryDelta;
        if (d <= 0.0 || d >= traceDistance[0]) {
            return currentDir;
        }

        double secondary1 = startSecondary1 + d * secondary1Delta;
        double secondary2 = startSecondary2 + d * secondary2Delta;

        if (isWithinBounds(secondary1, secondary1Min, secondary1Max) &&
                isWithinBounds(secondary2, secondary2Min, secondary2Max)) {
            traceDistance[0] = d;
            return direction;
        }
        return currentDir;
    }

    private static boolean isWithinBounds(double value, double min, double max) {
        return (value >= min - EPSILON) && (value <= max + EPSILON);
    }

    public List<Vec3d> getEdgePoints(double interval) {
        List<Vec3d> points = new ArrayList<>();

        // AABB的8个顶点
        Vec3d[] vertices = {
                new Vec3d(minX, minY, minZ), // 0
                new Vec3d(maxX, minY, minZ), // 1
                new Vec3d(minX, maxY, minZ), // 2
                new Vec3d(maxX, maxY, minZ), // 3
                new Vec3d(minX, minY, maxZ), // 4
                new Vec3d(maxX, minY, maxZ), // 5
                new Vec3d(minX, maxY, maxZ), // 6
                new Vec3d(maxX, maxY, maxZ)  // 7
        };

        // 12条边的定义（连接哪两个顶点）
        int[][] edges = {
                {0, 1}, // 底部X边（前）
                {1, 3}, // 底部Y边（右）
                {3, 2}, // 底部X边（后）
                {2, 0}, // 底部Y边（左）

                {4, 5}, // 顶部X边（前）
                {5, 7}, // 顶部Y边（右）
                {7, 6}, // 顶部X边（后）
                {6, 4}, // 顶部Y边（左）

                {0, 4}, // Z边（左下前）
                {1, 5}, // Z边（右下前）
                {2, 6}, // Z边（左后上）
                {3, 7}  // Z边（右后上）
        };

        for (int[] edge : edges) {
            Vec3d start = vertices[edge[0]];
            Vec3d end = vertices[edge[1]];
            points.addAll(sampleLine(start, end, interval));
        }

        return points;
    }

    private List<Vec3d> sampleLine(Vec3d start, Vec3d end, double interval) {
        List<Vec3d> points = new ArrayList<>();

        // 计算线段长度
        double dx = end.x - start.x;
        double dy = end.y - start.y;
        double dz = end.z - start.z;
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);

        // 计算采样点数（去掉终点避免重复）
        int numPoints = (int) Math.floor(length / interval);

        // 如果线段太短，至少返回起点
        if (numPoints <= 0) {
            points.add(start);
            return points;
        }

        // 按间隔采样
        for (int i = 0; i <= numPoints; i++) {
            double t = (double) i / numPoints;
            double x = start.x + dx * t;
            double y = start.y + dy * t;
            double z = start.z + dz * t;
            points.add(new Vec3d(x, y, z));
        }

        return points;
    }

    @Override
    public String toString() {
        return "AABB{" +
                "minX=" + minX +
                ", minY=" + minY +
                ", minZ=" + minZ +
                ", maxX=" + maxX +
                ", maxY=" + maxY +
                ", maxZ=" + maxZ +
                '}';
    }
}
