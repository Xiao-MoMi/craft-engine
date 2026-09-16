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
        Vector3f rotated = rotateOffset(offset);
        return new Vec3d(this.origin.x + rotated.x, this.origin.y + rotated.y, this.origin.z - rotated.z);
    }

    public WorldPosition position(Vector3f offset, float xRot, float yRot) {
        Vector3f rotated = rotateOffset(offset);
        return new WorldPosition(this.origin.world, this.origin.x + rotated.x, this.origin.y + rotated.y, this.origin.z - rotated.z, xRot, yRot);
    }

    public WorldPosition elementPosition(Vector3f offset, float xRot, float yRot) {
        return position(offset, this.origin.xRot + xRot, this.origin.yRot + yRot);
    }
}
