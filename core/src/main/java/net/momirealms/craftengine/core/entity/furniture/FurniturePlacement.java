package net.momirealms.craftengine.core.entity.furniture;

import net.momirealms.craftengine.core.util.QuaternionUtils;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.WorldPosition;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class FurniturePlacement {
    public final WorldPosition origin;
    private final Quaternionf rotation;

    public FurniturePlacement(WorldPosition origin) {
        this.origin = origin;
        this.rotation = origin.yRot % 90 == 0 ? null : QuaternionUtils.toQuaternionf(0f, (float) Math.toRadians(180 - origin.yRot), 0f).conjugate();
    }

    public Vector3f rotateOffset(Vector3f offset) {
        return this.rotation == null ? Furniture.rotateHitboxOffset(this.origin.yRot, offset) : this.rotation.transform(new Vector3f(offset));
    }

    public Vec3d relativePosition(Vector3f offset) {
        return this.relativePosition(offset.x, offset.y, offset.z);
    }

    public Vec3d relativePosition(float x, float y, float z) {
        if (this.rotation == null) {
            int yaw = (int) ((this.origin.yRot % 360 + 360) % 360);
            double worldY = this.origin.y + y;
            return switch (yaw) {
                case 90 -> new Vec3d(this.origin.x - z, worldY, this.origin.z - x);
                case 180 -> new Vec3d(this.origin.x + x, worldY, this.origin.z - z);
                case 270 -> new Vec3d(this.origin.x + z, worldY, this.origin.z + x);
                default -> new Vec3d(this.origin.x - x, worldY, this.origin.z + z);
            };
        }
        // Use the normalized coefficients of the same Y-axis quaternion as rotateOffset.
        float yy = this.rotation.y * this.rotation.y;
        float ww = this.rotation.w * this.rotation.w;
        float inverseNorm = 1f / (yy + ww);
        float cos = (ww - yy) * inverseNorm;
        float sin = (2f * this.rotation.y * this.rotation.w) * inverseNorm;
        float rotatedX = org.joml.Math.fma(cos, x, sin * z);
        float rotatedY = ((yy + ww) * inverseNorm) * y;
        float rotatedZ = org.joml.Math.fma(-sin, x, cos * z);
        return new Vec3d(this.origin.x + rotatedX, this.origin.y + rotatedY, this.origin.z - rotatedZ);
    }

    public WorldPosition position(Vector3f offset, float xRot, float yRot) {
        Vector3f rotated = rotateOffset(offset);
        return new WorldPosition(this.origin.world, this.origin.x + rotated.x, this.origin.y + rotated.y, this.origin.z - rotated.z, xRot, yRot);
    }

    public WorldPosition elementPosition(Vector3f offset, float xRot, float yRot) {
        return position(offset, this.origin.xRot + xRot, this.origin.yRot + yRot);
    }
}
