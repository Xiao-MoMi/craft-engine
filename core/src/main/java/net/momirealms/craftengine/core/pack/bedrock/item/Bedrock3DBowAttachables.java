package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.pack.bedrock.item.Bedrock3DItemAttachables.Material;
import net.momirealms.craftengine.core.pack.bedrock.item.Bedrock3DItemAttachables.Stage;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VectorUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class Bedrock3DBowAttachables {
    private static final String USE_X = "ce_bow_use_x";
    private static final String USE_Y = "ce_bow_use_y";
    private static final String USE_Z = "ce_bow_use_z";
    private static final String CHARGE = "ce_bow_charge";
    private static final String AIM = "ce_bow_aim";

    private Bedrock3DBowAttachables() {
    }

    public static void write(Key id, List<Stage> stages, List<Material> materials, int maximumUseTicks, Path root) throws IOException {
        JsonArray bones = new JsonArray();
        bones.add(useBone(USE_X, "ce_slot"));
        bones.add(useBone(USE_Y, USE_X));
        bones.add(useBone(USE_Z, USE_Y));
        bones.add(useBone(CHARGE, USE_Z));
        bones.add(useBone(AIM, CHARGE));
        JsonObject animations = new JsonObject();
        animations.add("java_bow_use", useAnimation());
        JsonArray animate = new JsonArray();
        JsonObject using = new JsonObject();
        using.addProperty("java_bow_use", "context.is_first_person && variable.ce_using_item");
        animate.add(using);
        JsonArray preAnimation = BedrockItemAttachables.bowScripts(stages.stream().map(Stage::condition).toList(), maximumUseTicks);
        // Java 26.3 FirstPersonHandsAndItemsRenderer：使用时长包含帧插值，弓在 20 tick 达到满蓄力。
        preAnimation.add("variable.ce_bow_time = variable.ce_using_item ? variable.ce_use_ticks + query.frame_alpha - 1 : 0;");
        preAnimation.add("variable.ce_bow_power = variable.ce_bow_time / 20;");
        preAnimation.add("variable.ce_bow_power = math.min(1, (variable.ce_bow_power * variable.ce_bow_power + variable.ce_bow_power * 2) / 3);");
        preAnimation.add("variable.ce_bow_side = context.item_slot == 'off_hand' ? -1 : 1;");
        // Java sin 使用弧度，Molang 使用角度；平移由方块单位转为模型单位。
        preAnimation.add("variable.ce_bow_shake = variable.ce_bow_power > 0.1 ? math.sin((variable.ce_bow_time - 0.1) * 1.3 * 180 / math.pi) * (variable.ce_bow_power - 0.1) * 0.004 * 16 : 0;");
        Bedrock3DItemAttachables.write(id, stages, materials, preAnimation, bones, animations, animate, root);
    }

    private static JsonObject useBone(String name, String parent) {
        JsonObject bone = new JsonObject();
        bone.addProperty("name", name);
        bone.addProperty("parent", parent);
        bone.add("pivot", VectorUtils.toJson(0, 8, 0));
        return bone;
    }

    private static JsonObject useAnimation() {
        JsonObject x = new JsonObject();
        JsonArray position = new JsonArray();
        position.add("variable.ce_bow_side * 0.2785682 * 16");
        position.add(0.18344387f * 16);
        position.add(0.15731531f * 16);
        x.add("position", position);
        x.add("rotation", VectorUtils.toJson(13.935f, 0, 0));
        JsonObject y = new JsonObject();
        JsonArray rotationY = new JsonArray();
        rotationY.add(0);
        rotationY.add("variable.ce_bow_side * -35.3");
        rotationY.add(0);
        y.add("rotation", rotationY);
        JsonObject z = new JsonObject();
        JsonArray rotationZ = new JsonArray();
        rotationZ.add(0);
        rotationZ.add(0);
        rotationZ.add("variable.ce_bow_side * -9.785");
        z.add("rotation", rotationZ);
        JsonObject charge = new JsonObject();
        JsonArray movement = new JsonArray();
        movement.add(0);
        movement.add("variable.ce_bow_shake");
        movement.add("variable.ce_bow_power * 0.04 * 16");
        charge.add("position", movement);
        JsonArray scale = new JsonArray();
        scale.add(1);
        scale.add(1);
        scale.add("1 + variable.ce_bow_power * 0.2");
        charge.add("scale", scale);
        JsonObject aim = new JsonObject();
        JsonArray rotationAim = new JsonArray();
        rotationAim.add(0);
        rotationAim.add("variable.ce_bow_side * 45");
        rotationAim.add(0);
        aim.add("rotation", rotationAim);
        JsonObject bones = new JsonObject();
        bones.add(USE_X, x);
        bones.add(USE_Y, y);
        bones.add(USE_Z, z);
        bones.add(CHARGE, charge);
        bones.add(AIM, aim);
        JsonObject animation = new JsonObject();
        animation.addProperty("loop", true);
        animation.add("bones", bones);
        return animation;
    }

}
