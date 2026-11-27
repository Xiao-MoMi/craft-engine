package net.momirealms.craftengine.bedrock.geyser.entity.display;

import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector3f;

public record DisplayPose(Vector3f translation, Quaternionf left, Vector3f scale, Quaternionf right) {
    static final DisplayPose IDENTITY = new DisplayPose(Vector3f.ZERO, Quaternionf.IDENTITY, Vector3f.ONE, Quaternionf.IDENTITY);

    private static Quaternionf rotation(Quaternionf first, Quaternionf second, float fraction) {
        if (first.equals(second)) {
            return first;
        }
        if (first.lengthSquared() < 1e-12f || second.lengthSquared() < 1e-12f) {
            return GenericMath.lerp(first, second, fraction);
        }
        double length = Math.sqrt(first.lengthSquared()) * (1 - fraction) + Math.sqrt(second.lengthSquared()) * fraction;
        return GenericMath.slerp(first.normalize(), second.normalize(), fraction).mul(length);
    }

    DisplayPose interpolate(DisplayPose target, float fraction) {
        return new DisplayPose(
                GenericMath.lerp(this.translation, target.translation, fraction),
                rotation(this.left, target.left, fraction),
                GenericMath.lerp(this.scale, target.scale, fraction),
                rotation(this.right, target.right, fraction)
        );
    }
}
