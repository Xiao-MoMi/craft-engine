package net.momirealms.craftengine.core.pack.bedrock.item.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.pack.bedrock.item.render.BedrockItemAttachables;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.FORMAT_VERSION;
import static net.momirealms.craftengine.core.pack.bedrock.BedrockFormatVersions.ANIMATION;

public final class BedrockSwingPlayerAnimations {
    private BedrockSwingPlayerAnimations() {
    }

    static void write(Set<Key> itemsUsingJavaSwing, BedrockPlayerAnimationController controller, Path root) throws IOException {
        if (itemsUsingJavaSwing.isEmpty()) {
            return;
        }
        String condition = "query.is_item_name_any('slot.weapon.mainhand'" + BedrockPlayerAnimations.sortedItemArguments(itemsUsingJavaSwing) + ")";
        String setup = "variable.ce_java_swing = " + condition + Templates.POSE_SCRIPT;
        BedrockPlayerAnimationState firstPerson = controller.state("first_person");
        firstPerson.updateCondition("first_person_attack_controller", original -> setup + original + ';');
        Path animationPath = root.resolve("animations/ce/shared/swing_player.animation.json");
        Files.createDirectories(animationPath.getParent());
        GsonHelper.writeJsonFile(Templates.ATTACK_ANIMATION, animationPath);
    }

    private static String poseScript(JsonObject source) {
        StringBuilder scripts = new StringBuilder("; variable.ce_java_swing ? { ");
        for (JsonElement expression : source.getAsJsonArray("pre_animation")) {
            scripts.append(expression.getAsString()).append(' ');
        }

        appendArmTransform(scripts);
        for (JsonElement expression : source.getAsJsonArray("pose")) {
            scripts.append(expression.getAsString()).append(' ');
        }
        return scripts.append("}; return ").toString();
    }

    private static void appendArmTransform(StringBuilder scripts) {
        // A(t) = A(0) * K * S * C * JavaSwing(t) * C * inverse(S) * inverse(K)。
        // 在玩家自身的渲染上下文计算，避免 owning_entity 的 public 变量快照只按 tick 更新。
        float radians = (float) (Math.PI / 180);
        Matrix4f arm = new Matrix4f().translation(8.5f, 12, 12).rotateZYX(115 * radians, -45 * radians, 95 * radians);
        Matrix4f mount = new Matrix4f().translation(-1, 7, 0);
        Matrix4f slot = BedrockItemAttachables.firstPersonSlotMatrix();
        Matrix4f coordinates = new Matrix4f().scaling(-1, -1, 1);
        Matrix4f left = new Matrix4f(arm).mul(mount).mul(slot).mul(coordinates);
        Matrix4f right = new Matrix4f(coordinates).mul(new Matrix4f(slot).invert()).mul(new Matrix4f(mount).invert());
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 4; column++) {
                scripts.append("variable.ce_swing_m")
                        .append(row)
                        .append(column)
                        .append(" = ")
                        .append(matrixElement(left, right, row, column))
                        .append("; ");
            }
        }
    }

    private static JsonObject attackAnimation(JsonObject rightArm) {
        JsonObject animationFile = GsonHelper.readJsonFromResource("/bedrock/player/attack.animation.json").getAsJsonObject();
        animationFile.addProperty(FORMAT_VERSION, ANIMATION);
        JsonObject armBone = animationFile.getAsJsonObject("animations")
                .getAsJsonObject("animation.player.first_person.attack_rotation")
                .getAsJsonObject("bones")
                .getAsJsonObject("rightarm");
        for (String channel : new String[]{"position", "rotation"}) {
            JsonArray vanilla = armBone.getAsJsonArray(channel);
            JsonArray replacement = rightArm.getAsJsonArray(channel);
            for (int i = 0; i < vanilla.size(); i++) {
                vanilla.set(i, new JsonPrimitive("variable.ce_java_swing ? (" + replacement.get(i).getAsString() + ") : (" + vanilla.get(i).getAsString() + ")"));
            }
        }
        return animationFile;
    }

    private static final class Templates {
        private static final String POSE_SCRIPT;
        private static final JsonObject ATTACK_ANIMATION;

        static {
            JsonObject source = GsonHelper.readJsonFromResource("/bedrock/player/swing.pose.json").getAsJsonObject();
            POSE_SCRIPT = poseScript(source);
            ATTACK_ANIMATION = attackAnimation(source.getAsJsonObject("rightarm"));
        }
    }

    private static String matrixElement(Matrix4fc left, Matrix4fc right, int row, int column) {
        StringBuilder expression = new StringBuilder(BigDecimal.valueOf(left.get(3, row) * right.get(column, 3)).toPlainString());
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 4; j++) {
                float coefficient = left.get(i, row) * right.get(column, j);
                if (Math.abs(coefficient) < 0.000001f) {
                    continue;
                }
                expression.append(" + (")
                        .append(BigDecimal.valueOf(coefficient).toPlainString())
                        .append(") * variable.ce_swing_j")
                        .append(i)
                        .append(j);
            }
        }
        return expression.toString();
    }
}
