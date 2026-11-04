package net.momirealms.craftengine.core.pack.bedrock.item.animation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

public final class BedrockSpearPlayerAnimations {
    private BedrockSpearPlayerAnimations() {
    }

    static void write(SortedMap<Key, List<String>> spearPoseScriptsByItem, BedrockPlayerAnimationController controller, Path root) throws IOException {
        if (spearPoseScriptsByItem.isEmpty()) {
            return;
        }
        JsonObject player = GsonHelper.readJsonFromResource("/bedrock/player/spear.pose.json").getAsJsonObject();
        updateFirstPersonController(controller.state("first_person"), spearPoseScriptsByItem.keySet());
        updateThirdPersonController(controller.state("third_person"), spearPoseScriptsByItem, player);
        writeHoldAnimation(player, root);
    }

    private static void updateFirstPersonController(BedrockPlayerAnimationState state, Set<Key> spearIds) {
        StringBuilder firstPersonCondition = new StringBuilder("query.is_item_name_any('slot.weapon.mainhand'");
        for (Key id : spearIds) {
            firstPersonCondition.append(", '").append(id.asString()).append('\'');
        }
        firstPersonCondition.append(')');
        state.updateCondition("first_person_melee_spear_controller", original -> "(" + original + ") && !(" + firstPersonCondition + ")");
    }

    private static void updateThirdPersonController(BedrockPlayerAnimationState state, Map<Key, List<String>> spearPoseScriptsByItem,
                                                    JsonObject player) throws IOException {
        String setup = thirdPersonSetup(spearPoseScriptsByItem, player);
        state.updateCondition("third_person_melee_spear_controller",
                original -> setup + "return (" + original + ") && !variable.ce_java_spear;");
        // 绕过原版 hold/use/attack 互斥状态，使突刺余下的动作能与举矛叠加。
        state.appendAnimation("third_person_melee_spear_hold", "variable.ce_java_spear");
    }

    private static String thirdPersonSetup(Map<Key, List<String>> spearPoseScriptsByItem, JsonObject player) throws IOException {
        StringBuilder scripts = new StringBuilder("variable.ce_java_spear = 0; variable.ce_using_item = query.is_using_item && query.main_hand_item_use_duration > 0; ");
        spearPoseScriptsByItem.forEach((id, poseScripts) -> appendSpearParameters(scripts, id, poseScripts));
        JsonObject pose = GsonHelper.readJsonFromResource("/bedrock/item/spear/handheld.pose.json").getAsJsonObject();
        // 第三人称 getTicksUsingItem(partialTicks) 不减一；手臂和物品复用同一组阶段公式。
        scripts.append("variable.ce_java_spear ? { variable.ce_spear_time = variable.ce_spear_use ? math.max(0, query.main_hand_item_max_duration - query.main_hand_item_use_duration + query.frame_alpha) : 0; ")
                .append("variable.ce_spear_hit_ticks = query.ticks_since_last_kinetic_weapon_hit; ");
        for (JsonElement expression : pose.getAsJsonArray("pre_animation")) {
            scripts.append(expression.getAsString()).append(' ');
        }
        // 基岩手臂是 body 的子骨骼，先求 inverse(parent) * JavaArm，再分解成本地 ZYX 旋转。
        for (JsonElement expression : player.getAsJsonArray("pre_animation")) {
            scripts.append(expression.getAsString()).append(' ');
        }
        return scripts.append("}; ").toString();
    }

    private static void appendSpearParameters(StringBuilder scripts, Key id, List<String> poseScripts) {
        scripts.append("query.is_item_name_any('slot.weapon.mainhand', '")
                .append(id.asString())
                .append("') ? { variable.ce_java_spear = 1; ");
        for (String parameter : poseScripts) {
            scripts.append(parameter).append(' ');
        }
        scripts.append("}; ");
    }

    private static void writeHoldAnimation(JsonObject player, Path root) throws IOException {
        JsonObject animationFile = GsonHelper.readJsonFromResource("/bedrock/player/spear.animation.json").getAsJsonObject();
        JsonObject animations = new JsonObject();
        JsonObject hold = animationFile.getAsJsonObject("animations")
                .getAsJsonObject("animation.humanoid.melee_spear_hold");
        JsonObject bones = hold.getAsJsonObject("bones");
        for (Map.Entry<String, JsonElement> bone : player.getAsJsonObject("bones").entrySet()) {
            JsonObject target = bones.asMap()
                    .computeIfAbsent(bone.getKey(), ignored -> new JsonObject())
                    .getAsJsonObject();
            applyConditionalPose(target, bone.getValue().getAsJsonObject());
        }
        animations.add("animation.humanoid.melee_spear_hold", hold);
        animationFile.add("animations", animations);
        Path animationPath = root.resolve("animations/craftengine/shared/spear_player.animation.json");
        Files.createDirectories(animationPath.getParent());
        GsonHelper.writeJsonFile(animationFile, animationPath);
    }

    private static void applyConditionalPose(JsonObject target, JsonObject pose) {
        for (Map.Entry<String, JsonElement> channel : pose.entrySet()) {
            JsonArray original = target.getAsJsonArray(channel.getKey());
            JsonArray replacement = channel.getValue().getAsJsonArray();
            JsonArray vector = new JsonArray();
            for (int i = 0; i < replacement.size(); i++) {
                String vanilla = original == null ? "0" : original.get(i).getAsString();
                vector.add(new JsonPrimitive("variable.ce_java_spear ? (" + replacement.get(i).getAsString() + ") : (" + vanilla + ")"));
            }
            target.add(channel.getKey(), vector);
        }
    }
}
