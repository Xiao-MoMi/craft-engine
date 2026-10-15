package net.momirealms.craftengine.core.pack.bedrock.model;

import net.momirealms.craftengine.core.util.VectorUtils;
import org.joml.Vector3f;

import java.util.List;

final class BedrockModelTransform {

    private BedrockModelTransform() {
    }

    static Vector3f position(Vector3f position) {
        return new Vector3f(8 - position.x(), position.y(), position.z() - 8);
    }

    static Vector3f scaleAround(Vector3f point, Vector3f pivot, Vector3f scale) {
        return new Vector3f(pivot.x() + (point.x() - pivot.x()) * scale.x(),
                pivot.y() + (point.y() - pivot.y()) * scale.y(),
                pivot.z() + (point.z() - pivot.z()) * scale.z());
    }

    static Vector3f rescale(Vector3f angles) {
        // Match Blockbench's cube preview scale, baking it before rotation because geometry has no rescale flag.
        float x = rescalingFactor(angles.x());
        float y = rescalingFactor(angles.y());
        float z = rescalingFactor(angles.z());
        return new Vector3f(y * z, x * z, x * y);
    }

    private static float rescalingFactor(float angle) {
        angle = Math.abs(angle);
        if (angle > 45) {
            angle = 90 - angle;
        }
        return (float) (1 / Math.cos(Math.toRadians(angle)));
    }

    static BedrockGeometry.Bounds bounds(List<BedrockGeometry.Cube> cubes) {
        double radius = 0;
        double minY = Double.POSITIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (BedrockGeometry.Cube cube : cubes) {
            BedrockGeometry.Rotation rotation = cube.rotation();
            Vector3f pivot = rotation == null ? VectorUtils.ZERO : rotation.pivot();
            Vector3f angles = rotation == null ? VectorUtils.ZERO : rotation.angles();
            // Undo the codec's signs to compute bounds in Blockbench's ZYX preview coordinate system.
            double cosX = Math.cos(Math.toRadians(-angles.x()));
            double sinX = Math.sin(Math.toRadians(-angles.x()));
            double cosY = Math.cos(Math.toRadians(-angles.y()));
            double sinY = Math.sin(Math.toRadians(-angles.y()));
            double cosZ = Math.cos(Math.toRadians(angles.z()));
            double sinZ = Math.sin(Math.toRadians(angles.z()));
            for (int corner = 0; corner < 8; corner++) {
                double x = -(cube.origin().x() + ((corner & 1) == 0 ? 0 : cube.size().x())) + pivot.x();
                double y = cube.origin().y() + ((corner & 2) == 0 ? 0 : cube.size().y()) - pivot.y();
                double z = cube.origin().z() + ((corner & 4) == 0 ? 0 : cube.size().z()) - pivot.z();
                double rotatedY = y * cosX - z * sinX;
                double rotatedZ = y * sinX + z * cosX;
                double rotatedX = x * cosY + rotatedZ * sinY;
                z = -x * sinY + rotatedZ * cosY + pivot.z();
                x = rotatedX * cosZ - rotatedY * sinZ - pivot.x();
                y = rotatedX * sinZ + rotatedY * cosZ + pivot.y();
                radius = Math.max(radius, Math.max(Math.abs(x), Math.abs(z)));
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
            }
        }
        if (cubes.isEmpty()) {
            return new BedrockGeometry.Bounds(0, 0, VectorUtils.ZERO);
        }
        float bottom = (float) Math.floor(minY / 16);
        float top = (float) Math.ceil(maxY / 16);
        return new BedrockGeometry.Bounds((float) Math.ceil(radius * 2 / 16), top - bottom, new Vector3f(0, (bottom + top) / 2, 0));
    }
}
