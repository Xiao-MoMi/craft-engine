/*
 * Copyright (C) <2025> <XiaoMoMi>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
