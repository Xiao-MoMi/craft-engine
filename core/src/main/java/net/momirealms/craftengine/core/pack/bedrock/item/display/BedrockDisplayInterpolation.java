package net.momirealms.craftengine.core.pack.bedrock.item.display;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.List;

final class BedrockDisplayInterpolation {
    private static final List<String> AXES = List.of("x", "y", "z");
    private static final List<String> QUATERNION_AXES = List.of("x", "y", "z", "w");

    private BedrockDisplayInterpolation() {
    }

    static JsonObject scripts(JsonArray animate) {
        JsonObject result = new JsonObject();
        result.add("animate", animate);
        JsonArray initialize = new JsonArray();
        initialize.add("v.ce_seen = -1; v.ce_start = 0; v.ce_duration = 0; v.ce_active = 0; v.ce_t = -1;");
        result.add("initialize", initialize);
        JsonArray update = new JsonArray();
        StringBuilder changed = new StringBuilder("v.ce_snap = v.ce_seen < 0 || math.mod(q.property('craftengine:interpolation'), 8) >= 4; ");
        // 中途换目标时先求旧动画在当前帧的姿态，避免从上一个目标或上一帧重新起步。
        changed.append("!v.ce_snap ? {").append(fraction()).append(sample()).append("}; ");
        changed.append("v.ce_snap || math.mod(q.property('craftengine:interpolation'), 4) > 0 ? {");
        for (String group : List.of("pos", "size", "left", "right")) {
            for (String axis : axes(group)) {
                String name = group + '_' + axis;
                changed.append("v.ce_from_").append(name).append(" = v.ce_snap ? q.property('craftengine:").append(name).append("') : v.ce_").append(name).append("; ");
                changed.append("v.ce_to_").append(name).append(" = q.property('craftengine:").append(name).append("'); ");
            }
        }
        changed.append("}; v.ce_snap ? {v.ce_active = 0;} : {math.mod(q.property('craftengine:interpolation'), 2) == 1 ? {v.ce_start = q.life_time + q.property('craftengine:delay'); v.ce_active = 1;};}; ");
        changed.append("v.ce_duration = q.property('craftengine:duration'); v.ce_t = -1; v.ce_seen = q.property('craftengine:interpolation');");
        update.add("v.ce_seen != q.property('craftengine:interpolation') ? {" + changed + "};");
        update.add(fraction());
        StringBuilder pose = new StringBuilder(sample());
        for (String group : List.of("left", "right")) {
            String q = "v.ce_" + group + '_';
            pose.append("v.ce_sy = math.clamp(2 * (").append(q).append("w * ").append(q).append("y - ").append(q).append("z * ").append(q).append("x), -1, 1); ");
            pose.append(q).append("rx = math.abs(v.ce_sy) >= 0.9999999 ? 0 : math.atan2(2 * (").append(q).append("w * ").append(q).append("x + ").append(q).append("y * ").append(q).append("z), 1 - 2 * (").append(q).append("x * ").append(q).append("x + ").append(q).append("y * ").append(q).append("y)); ");
            pose.append(q).append("ry = math.asin(v.ce_sy); ");
            pose.append(q).append("rz = math.abs(v.ce_sy) >= 0.9999999 ? 2 * math.atan2(").append(q).append("z, ").append(q).append("w) : math.atan2(2 * (").append(q).append("w * ").append(q).append("z + ").append(q).append("x * ").append(q).append("y), 1 - 2 * (").append(q).append("y * ").append(q).append("y + ").append(q).append("z * ").append(q).append("z)); ");
        }
        pose.append("v.ce_t = v.ce_fraction;");
        update.add("v.ce_t != v.ce_fraction ? {" + pose + "};");
        result.add("pre_animation", update);
        return result;
    }

    private static String fraction() {
        return "v.ce_fraction = !v.ce_active || v.ce_duration <= 0 ? 1 : math.clamp((q.life_time - v.ce_start) / v.ce_duration, 0, 1); ";
    }

    private static List<String> axes(String group) {
        return group.equals("left") || group.equals("right") ? QUATERNION_AXES : AXES;
    }

    private static String sample() {
        StringBuilder result = new StringBuilder();
        for (String group : List.of("pos", "size")) {
            for (String axis : AXES) {
                String name = group + '_' + axis;
                result.append("v.ce_").append(name).append(" = math.lerp(v.ce_from_").append(name).append(", v.ce_to_").append(name).append(", v.ce_fraction); ");
            }
        }
        for (String group : List.of("left", "right")) {
            result.append("v.ce_dot = ");
            for (String axis : QUATERNION_AXES) {
                if (!axis.equals("x")) {
                    result.append(" + ");
                }
                result.append("v.ce_from_").append(group).append('_').append(axis).append(" * v.ce_to_").append(group).append('_').append(axis);
            }
            result.append("; v.ce_sign = v.ce_dot < 0 ? -1 : 1; v.ce_dot = math.clamp(math.abs(v.ce_dot), 0, 1); v.ce_angle = math.acos(v.ce_dot); v.ce_sin = math.max(0.000001, math.sin(v.ce_angle)); v.ce_a = v.ce_dot > 0.9995 ? 1 - v.ce_fraction : math.sin((1 - v.ce_fraction) * v.ce_angle) / v.ce_sin; v.ce_b = (v.ce_dot > 0.9995 ? v.ce_fraction : math.sin(v.ce_fraction * v.ce_angle) / v.ce_sin) * v.ce_sign; ");
            for (String axis : QUATERNION_AXES) {
                String name = group + '_' + axis;
                result.append("v.ce_").append(name).append(" = v.ce_from_").append(name).append(" * v.ce_a + v.ce_to_").append(name).append(" * v.ce_b; ");
            }
            result.append("v.ce_length = math.sqrt(math.max(0.000000000001, ");
            for (String axis : QUATERNION_AXES) {
                if (!axis.equals("x")) {
                    result.append(" + ");
                }
                result.append("v.ce_").append(group).append('_').append(axis).append(" * v.ce_").append(group).append('_').append(axis);
            }
            result.append(")); ");
            for (String axis : QUATERNION_AXES) {
                result.append("v.ce_").append(group).append('_').append(axis).append(" = v.ce_").append(group).append('_').append(axis).append(" / v.ce_length; ");
            }
        }
        return result.toString();
    }
}
