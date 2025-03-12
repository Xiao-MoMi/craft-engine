package net.momirealms.craftengine.core.util;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public class QuaternionUtils {

    private QuaternionUtils() {}

    public static Quaternionf toQuaternionf(double yaw, double pitch, double roll) {
        double cy = Math.cos(yaw * 0.5);
        double sy = Math.sin(yaw * 0.5);
        double cp = Math.cos(pitch * 0.5);
        double sp = Math.sin(pitch * 0.5);
        double cr = Math.cos(roll * 0.5);
        double sr = Math.sin(roll * 0.5);
        double w = cy * cp * cr + sy * sp * sr;
        double x = cy * cp * sr - sy * sp * cr;
        double y = sy * cp * sr + cy * sp * cr;
        double z = sy * cp * cr - cy * sp * sr;
        return new Quaternionf(x, y, z, w);
    }

    public static Vector3f toEulerAngle(Quaternionf quaternionf) {
        float x = quaternionf.x;
        float y = quaternionf.y;
        float z = quaternionf.z;
        float w = quaternionf.w;
        float siny_cosp = 2 * (w * z + x * y);
        float cosy_cosp = 1 - 2 * (y * y + z * z);
        float yaw = (float) Math.atan2(siny_cosp, cosy_cosp);
        float sinp = 2 * (w * y - z * x);
        float pitch = (float) Math.asin(sinp);
        float sinr_cosp = 2 * (w * x + y * z);
        float cosr_cosp = 1 - 2 * (x * x + y * y);
        float roll = (float) Math.atan2(sinr_cosp, cosr_cosp);
        return new Vector3f(yaw, pitch, roll);
    }

    public static double quaternionToPitch(Quaternionf quaternionf) {
        return 2 * Math.atan2(quaternionf.y, quaternionf.w);
    }
}
