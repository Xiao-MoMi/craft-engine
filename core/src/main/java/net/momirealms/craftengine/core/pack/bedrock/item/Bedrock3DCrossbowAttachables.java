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

public final class Bedrock3DCrossbowAttachables {
    private static final String USE_X = "ce_crossbow_use_x";
    private static final String USE_Y = "ce_crossbow_use_y";
    private static final String USE_Z = "ce_crossbow_use_z";
    private static final String CHARGE = "ce_crossbow_charge";
    private static final String AIM = "ce_crossbow_aim";

    private Bedrock3DCrossbowAttachables() {
    }

    public static void write(Key id, List<Stage> stages, List<Material> materials, BedrockCrossbowStates.State state, ItemBehaviorDescription.Use use, Path root) throws IOException {
        JsonArray bones = new JsonArray();
        bones.add(useBone(USE_X, "ce_slot"));
        bones.add(useBone(USE_Y, USE_X));
        bones.add(useBone(USE_Z, USE_Y));
        bones.add(useBone(CHARGE, USE_Z));
        bones.add(useBone(AIM, CHARGE));
        JsonObject animations = new JsonObject();
        animations.add("java_crossbow_use", useAnimation());
        JsonArray animate = new JsonArray();
        JsonObject using = new JsonObject();
        using.addProperty("java_crossbow_use", "context.is_first_person && variable.ce_using_item");
        animate.add(using);
        JsonArray preAnimation = BedrockCrossbowStates.scripts(state, use);
        // Java 26.3 的弩姿态线性蓄力；模型条件不截断进度，姿态仅截断上界。
        preAnimation.add("variable.ce_crossbow_time = variable.ce_using_item ? variable.ce_use_ticks + query.frame_alpha - 1 : 0;");
        preAnimation.add("variable.ce_crossbow_power = math.min(1, variable.ce_crossbow_time / " + use.chargeDuration().value() + ");");
        preAnimation.add("variable.ce_crossbow_side = context.item_slot == 'off_hand' ? -1 : 1;");
        preAnimation.add("variable.ce_crossbow_shake = variable.ce_crossbow_power > 0.1 ? math.sin((variable.ce_crossbow_time - 0.1) * 1.3 * 180 / math.pi) * (variable.ce_crossbow_power - 0.1) * 0.004 * 16 : 0;");
        if (state.charged()) {
            animations.add("java_crossbow_charged", chargedAnimation());
            JsonObject charged = new JsonObject();
            charged.addProperty("java_crossbow_charged", "context.is_first_person && context.item_slot == 'main_hand' && variable.attack_time < 0.001");
            animate.add(charged);
        }
        Bedrock3DItemAttachables.write(id, stages, materials, preAnimation, bones, animations, animate, root);
    }

    private static JsonObject useBone(String name, String parent) {
        JsonObject bone = new JsonObject();
        bone.addProperty("name", name);
        bone.addProperty("parent", parent);
        bone.add("pivot", VectorUtils.toJson(0, 8, 0));
        return bone;
    }

    private static JsonObject chargedAnimation() {
        // Java 对主手已装填弩额外平移、转向；挥动时不叠加这一持弩姿态。
        JsonObject x = new JsonObject();
        x.add("position", VectorUtils.toJson(0.641864f * 16, 0, 0));
        JsonObject y = new JsonObject();
        y.add("rotation", VectorUtils.toJson(0, -10, 0));
        JsonObject bones = new JsonObject();
        bones.add(USE_X, x);
        bones.add(USE_Y, y);
        JsonObject animation = new JsonObject();
        animation.addProperty("loop", true);
        animation.add("bones", bones);
        return animation;
    }

    private static JsonObject useAnimation() {
        // 与 Java 客户端顺序一致：举弩 XYZ 旋转、抖动与拉伸、最后 Y 旋转。
        JsonObject x = new JsonObject();
        JsonArray position = new JsonArray();
        position.add("variable.ce_crossbow_side * 0.4785682 * 16");
        position.add(-0.094387f * 16);
        position.add(0.05731531f * 16);
        x.add("position", position);
        x.add("rotation", VectorUtils.toJson(11.935f, 0, 0));
        JsonObject y = new JsonObject();
        JsonArray rotationY = new JsonArray();
        rotationY.add(0);
        rotationY.add("variable.ce_crossbow_side * -65.3");
        rotationY.add(0);
        y.add("rotation", rotationY);
        JsonObject z = new JsonObject();
        JsonArray rotationZ = new JsonArray();
        rotationZ.add(0);
        rotationZ.add(0);
        rotationZ.add("variable.ce_crossbow_side * -9.785");
        z.add("rotation", rotationZ);
        JsonObject charge = new JsonObject();
        JsonArray movement = new JsonArray();
        movement.add(0);
        movement.add("variable.ce_crossbow_shake");
        movement.add("variable.ce_crossbow_power * 0.04 * 16");
        charge.add("position", movement);
        JsonArray scale = new JsonArray();
        scale.add(1);
        scale.add(1);
        scale.add("1 + variable.ce_crossbow_power * 0.2");
        charge.add("scale", scale);
        JsonObject aim = new JsonObject();
        JsonArray rotationAim = new JsonArray();
        rotationAim.add(0);
        rotationAim.add("variable.ce_crossbow_side * 45");
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
