package net.momirealms.craftengine.bedrock.geyser.display;

import org.cloudburstmc.math.GenericMath;
import org.cloudburstmc.math.imaginary.Quaternionf;
import org.cloudburstmc.math.vector.Vector3f;

record DisplayPose(Vector3f translation, Quaternionf left, Vector3f scale, Quaternionf right) {
    static final DisplayPose IDENTITY = new DisplayPose(Vector3f.ZERO, Quaternionf.IDENTITY, Vector3f.ONE, Quaternionf.IDENTITY);

    DisplayPose interpolate(DisplayPose target, float fraction) {
        return new DisplayPose(GenericMath.lerp(this.translation, target.translation, fraction),
                rotation(this.left, target.left, fraction), GenericMath.lerp(this.scale, target.scale, fraction),
                rotation(this.right, target.right, fraction));
    }

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

    static Vector3f boneAngles(Quaternionf value) {
        if (value.lengthSquared() < 1e-12f) {
            return Vector3f.ZERO;
        }
        // 镜像 Java X 轴后，将左右四元数分别分解为基岩骨骼的 ZYX 旋转。非均匀缩放保留在两者之间。
        Quaternionf q = value.normalize();
        float x = q.getX(), y = -q.getY(), z = -q.getZ(), w = q.getW();
        double sinY = 2 * (w * y - z * x);
        if (Math.abs(sinY) >= 0.9999999) {
            return Vector3f.from(0, Math.copySign(90, sinY), Math.toDegrees(2 * Math.atan2(z, w)));
        }
        return Vector3f.from(Math.toDegrees(Math.atan2(2 * (w * x + y * z), 1 - 2 * (x * x + y * y))),
                Math.toDegrees(Math.asin(sinY)), Math.toDegrees(Math.atan2(2 * (w * z + x * y), 1 - 2 * (y * y + z * z))));
    }
}
