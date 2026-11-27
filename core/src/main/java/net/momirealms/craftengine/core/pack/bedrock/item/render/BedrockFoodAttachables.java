package net.momirealms.craftengine.core.pack.bedrock.item.render;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.VectorUtils;

final class BedrockFoodAttachables {
    private static final String YAW = "ce_eat_yaw";
    private static final String PITCH = "ce_eat_pitch";
    private static final String ROLL = "ce_eat_roll";
    private static final String ACTIVE = "context.is_first_person && variable.ce_using_item";

    private BedrockFoodAttachables() {
    }

    static BedrockAttachablePose eatPose(JsonArray scripts, int duration) {
        scripts.add(ACTIVE + " ? { "
                + "variable.ce_eat_time = math.clamp(variable.ce_remaining_ticks + 1 - query.frame_alpha, 0, " + duration + "); "
                + "variable.ce_eat_fraction = variable.ce_eat_time / " + duration + "; "
                + "variable.ce_eat_amount = 1 - math.pow(variable.ce_eat_fraction, 27); "
                + "variable.ce_eat_bob = variable.ce_eat_fraction < 0.8 ? math.abs(math.cos(variable.ce_eat_time * 45)) * 0.1 : 0; };");

        JsonArray bones = new JsonArray();
        bones.add(bone(YAW, "ce_slot"));
        bones.add(bone(PITCH, YAW));
        bones.add(bone(ROLL, PITCH));

        JsonObject yaw = GsonHelper.createObject("rotation", motion(1, "-90 * variable.ce_eat_amount"));
        JsonArray position = new JsonArray();
        position.add("-9.6 * variable.ce_eat_amount");
        position.add("16 * (variable.ce_eat_bob - 0.5 * variable.ce_eat_amount)");
        position.add(0);
        yaw.add("position", position);

        JsonObject animatedBones = new JsonObject();
        animatedBones.add(YAW, yaw);
        animatedBones.add(PITCH, GsonHelper.createObject("rotation", motion(0, "-10 * variable.ce_eat_amount")));
        animatedBones.add(ROLL, GsonHelper.createObject("rotation", motion(2, "30 * variable.ce_eat_amount")));
        JsonObject animation = new JsonObject();
        animation.addProperty("loop", true);
        animation.add("bones", animatedBones);
        JsonArray animate = new JsonArray();
        JsonObject whenEating = new JsonObject();
        whenEating.addProperty("java_eat", ACTIVE);
        animate.add(whenEating);
        return new BedrockAttachablePose(scripts, bones, GsonHelper.createObject("java_eat", animation), animate, BedrockAttachablePose.FIRST_PERSON_MAIN);
    }

    private static JsonObject bone(String name, String parent) {
        JsonObject bone = new JsonObject();
        bone.addProperty("name", name);
        bone.addProperty("parent", parent);
        // Pivot at the camera origin in ce_slot space because Java applies eating before the (0.56, -0.52, -0.72) hand translation.
        bone.add("pivot", VectorUtils.toJson(8.96f, 16.32f, 11.52f));
        return bone;
    }

    private static JsonArray motion(int axis, String expression) {
        JsonArray values = VectorUtils.toJson(0, 0, 0);
        values.set(axis, new JsonPrimitive(expression));
        return values;
    }
}
