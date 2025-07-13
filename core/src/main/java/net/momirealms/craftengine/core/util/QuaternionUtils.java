package net.momirealms.craftengine.core.util;

import org.joml.Quaternionf;

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

    public static Quaternionf toQuaternionf(float yaw, float pitch, float roll) {
        float cy = MCUtils.cos(yaw * 0.5f);
        float sy = MCUtils.sin(yaw * 0.5f);
        float cp = MCUtils.cos(pitch * 0.5f);
        float sp = MCUtils.sin(pitch * 0.5f);
        float cr = MCUtils.cos(roll * 0.5f);
        float sr = MCUtils.sin(roll * 0.5f);
        float w = cr * cp * cy + sr * sp * sy;
        float x = sr * cp * cy - cr * sp * sy;
        float y = cr * sp * cy + sr * cp * sy;
        float z = cr * cp * sy - sr * sp * cy;
        return new Quaternionf(x, y, z, w);
    }
}
