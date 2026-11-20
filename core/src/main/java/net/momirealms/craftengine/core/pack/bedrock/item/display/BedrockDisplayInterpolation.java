package net.momirealms.craftengine.core.pack.bedrock.item.display;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.bedrock.BedrockDisplayStyle;

import java.util.List;
import java.util.StringJoiner;

final class BedrockDisplayInterpolation {
    private static final List<String> AXES = List.of("x", "y", "z");
    private static final List<String> QUATERNION_AXES = List.of("x", "y", "z", "w");
    private static final List<String> ROTATION_GROUPS = List.of("left", "right");
    private static final String FRACTION = "v.ce_fraction = !v.ce_active || v.ce_duration <= 0 ? 1 : math.clamp((q.life_time - v.ce_start) / v.ce_duration, 0, 1); ";
    private static final List<String> PRE_ANIMATION = buildPreAnimation();

    private BedrockDisplayInterpolation() {
    }

    static JsonObject scripts(JsonArray animate) {
        JsonObject result = new JsonObject();
        result.add("animate", animate);
        JsonArray initialize = new JsonArray();
        initialize.add("v.ce_seen = -1; v.ce_start = 0; v.ce_duration = 0; v.ce_active = 0; v.ce_t = -1;");
        result.add("initialize", initialize);
        JsonArray update = new JsonArray(PRE_ANIMATION.size());
        PRE_ANIMATION.forEach(update::add);
        result.add("pre_animation", update);
        return result;
    }

    private static List<String> buildPreAnimation() {
        String sample = sample();
        StringBuilder changed = new StringBuilder("v.ce_snap = v.ce_seen < 0 || math.mod(q.property('ce:interpolation'), 8) >= 4; ");
        // 中途换目标时先求旧动画在当前帧的姿态，避免从上一个目标或上一帧重新起步。
        changed.append("!v.ce_snap ? {").append(FRACTION).append("v.ce_t != v.ce_fraction ? {").append(sample).append("}; }; ");
        changed.append("v.ce_snap || math.mod(q.property('ce:interpolation'), 4) > 0 ? {");
        for (String group : List.of("pos", "size", "left", "right")) {
            for (String axis : axes(group)) {
                String name = group + '_' + axis;
                changed.append("v.ce_from_").append(name).append(" = v.ce_snap ? q.property('ce:").append(name).append("') : v.ce_").append(name).append("; ");
                changed.append("v.ce_to_").append(name).append(" = q.property('ce:").append(name).append("'); ");
            }
        }
        for (String group : ROTATION_GROUPS) {
            changed.append(prepareRotation(group));
        }
        changed.append("}; v.ce_snap ? {v.ce_active = 0;} : {math.mod(q.property('ce:interpolation'), 2) == 1 ? {v.ce_start = q.life_time + q.property('ce:delay'); v.ce_active = 1;};}; ");
        changed.append("v.ce_duration = q.property('ce:duration'); v.ce_t = -1; v.ce_seen = q.property('ce:interpolation');");
        StringBuilder pose = new StringBuilder(sample);
        for (String group : ROTATION_GROUPS) {
            String q = "v.ce_" + group + '_';
            pose.append(q).append("moving || ").append(q).append("refresh ? {");
            pose.append("v.ce_sy = math.clamp(2 * (").append(q).append("w * ").append(q).append("y - ").append(q).append("z * ").append(q).append("x), -1, 1); ");
            pose.append(q).append("rx = math.abs(v.ce_sy) >= 0.9999999 ? 0 : math.atan2(2 * (").append(q).append("w * ").append(q).append("x + ").append(q).append("y * ").append(q).append("z), 1 - 2 * (").append(q).append("x * ").append(q).append("x + ").append(q).append("y * ").append(q).append("y)); ");
            pose.append(q).append("ry = math.asin(v.ce_sy); ");
            pose.append(q).append("rz = math.abs(v.ce_sy) >= 0.9999999 ? 2 * math.atan2(").append(q).append("z, ").append(q).append("w) : math.atan2(2 * (").append(q).append("w * ").append(q).append("z + ").append(q).append("x * ").append(q).append("y), 1 - 2 * (").append(q).append("y * ").append(q).append("y + ").append(q).append("z * ").append(q).append("z)); ");
            pose.append(q).append("refresh = 0; }; ");
        }
        pose.append("v.ce_t = v.ce_fraction;");
        return List.of(
                "v.ce_style = q.property('" + BedrockDisplayStyle.PROPERTY + "'); "
                        + "v.ce_billboard = math.mod(v.ce_style, " + BedrockDisplayStyle.HIDDEN_FLAG + "); "
                        + "v.ce_hidden = math.mod(v.ce_style, " + BedrockDisplayStyle.HELD_ITEM_STRIDE + ") >= " + BedrockDisplayStyle.HIDDEN_FLAG + "; "
                        + "v.ce_held_type = math.floor(v.ce_style / " + BedrockDisplayStyle.HELD_ITEM_STRIDE + ");",
                "v.ce_seen != q.property('ce:interpolation') ? {" + changed + "};",
                FRACTION,
                "v.ce_t != v.ce_fraction ? {" + pose + "};"
        );
    }

    private static List<String> axes(String group) {
        return group.equals("left") || group.equals("right") ? QUATERNION_AXES : AXES;
    }

    private static String prepareRotation(String group) {
        String q = "v.ce_" + group + '_';
        StringJoiner differences = new StringJoiner(" || ");
        StringJoiner dot = new StringJoiner(" + ");
        for (String axis : QUATERNION_AXES) {
            String from = "v.ce_from_" + group + '_' + axis;
            String to = "v.ce_to_" + group + '_' + axis;
            differences.add(from + " != " + to);
            dot.add(from + " * " + to);
        }
        // 切换目标时旧段可能只采样了四元数。若旧段在旋转，新段即使静止也要刷新一次欧拉角。
        return q + "refresh = v.ce_snap ? 1 : " + q + "moving; "
                + q + "moving = " + differences + "; "
                + q + "moving || " + q + "refresh ? {"
                + "v.ce_dot = " + dot + "; "
                + q + "sign = v.ce_dot < 0 ? -1 : 1; v.ce_dot = math.clamp(math.abs(v.ce_dot), 0, 1); "
                + q + "linear = v.ce_dot > 0.9995; "
                + "!" + q + "linear ? {" + q + "angle = math.acos(v.ce_dot); "
                + q + "sin = math.max(0.000001, math.sin(" + q + "angle)); }; }; ";
    }

    private static String sample() {
        StringBuilder result = new StringBuilder();
        for (String group : List.of("pos", "size")) {
            for (String axis : AXES) {
                String name = group + '_' + axis;
                result.append("v.ce_").append(name).append(" = math.lerp(v.ce_from_").append(name).append(", v.ce_to_").append(name).append(", v.ce_fraction); ");
            }
        }
        for (String group : ROTATION_GROUPS) {
            String q = "v.ce_" + group + '_';
            result.append(q).append("moving || ").append(q).append("refresh ? {");
            result.append("v.ce_a = ").append(q).append("linear ? 1 - v.ce_fraction : math.sin((1 - v.ce_fraction) * ").append(q).append("angle) / ").append(q).append("sin; ");
            result.append("v.ce_b = (").append(q).append("linear ? v.ce_fraction : math.sin(v.ce_fraction * ").append(q).append("angle) / ").append(q).append("sin) * ").append(q).append("sign; ");
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
            result.append("}; ");
        }
        return result.toString();
    }
}
