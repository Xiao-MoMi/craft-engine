package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class BedrockSpearPlayerAnimations {
    private BedrockSpearPlayerAnimations() {
    }

    static void write(Map<Key, ItemRegistrationPlan.Entry> javaSpears, BedrockPlayerAnimationController controller, Path root) throws IOException {
        if (javaSpears.isEmpty()) {
            return;
        }
        List<ItemRegistrationPlan.Entry> entries = javaSpears.values().stream()
                .sorted(Comparator.comparing(entry -> entry.bedrockId().asString())).toList();
        JsonObject player = GsonHelper.readJsonFromResource("/bedrock/spear/java_player.json").getAsJsonObject();
        String firstPersonCondition = entries.stream().map(entry -> "'" + entry.bedrockId().asString() + "'")
                .collect(Collectors.joining(", ", "query.is_item_name_any('slot.weapon.mainhand', ", ")"));
        JsonArray firstPerson = controller.animations("first_person");
        for (JsonElement animation : firstPerson) {
            if (animation instanceof JsonObject entry && entry.has("first_person_melee_spear_controller")) {
                String vanilla = entry.get("first_person_melee_spear_controller").getAsString();
                entry.addProperty("first_person_melee_spear_controller", "(" + vanilla + ") && !(" + firstPersonCondition + ")");
            }
        }
        JsonArray thirdPerson = controller.animations("third_person");
        String setup = thirdPersonSetup(entries, player);
        for (JsonElement animation : thirdPerson) {
            if (animation instanceof JsonObject entry && entry.has("third_person_melee_spear_controller")) {
                String vanilla = entry.get("third_person_melee_spear_controller").getAsString();
                entry.addProperty("third_person_melee_spear_controller", setup + "return (" + vanilla + ") && !variable.ce_java_spear;");
            }
        }
        // 绕过原版 hold/use/attack 互斥状态，使突刺余下的动作能与举矛叠加。
        JsonObject hold = new JsonObject();
        hold.addProperty("third_person_melee_spear_hold", "variable.ce_java_spear");
        thirdPerson.add(hold);
        writeHoldAnimation(player, root);
    }

    private static String thirdPersonSetup(List<ItemRegistrationPlan.Entry> entries, JsonObject player) throws IOException {
        StringBuilder scripts = new StringBuilder("variable.ce_java_spear = 0; variable.ce_using_item = query.is_using_item && query.main_hand_item_use_duration > 0; ");
        for (ItemRegistrationPlan.Entry entry : entries) {
            scripts.append("query.is_item_name_any('slot.weapon.mainhand', '").append(entry.bedrockId().asString())
                    .append("') ? { variable.ce_java_spear = 1; ");
            for (JsonElement parameter : Bedrock3DSpearAttachables.parameters(entry)) {
                scripts.append(parameter.getAsString()).append(' ');
            }
            scripts.append("}; ");
        }
        JsonObject pose = GsonHelper.readJsonFromResource("/bedrock/spear/java_spear.json").getAsJsonObject();
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

    private static void writeHoldAnimation(JsonObject player, Path root) throws IOException {
        JsonObject file = GsonHelper.readJsonFromResource("/bedrock/spear/player.animation.json").getAsJsonObject();
        JsonObject animations = new JsonObject();
        JsonObject hold = file.getAsJsonObject("animations").getAsJsonObject("animation.humanoid.melee_spear_hold");
        JsonObject bones = hold.getAsJsonObject("bones");
        for (Map.Entry<String, JsonElement> bone : player.getAsJsonObject("bones").entrySet()) {
            JsonObject target = bones.getAsJsonObject(bone.getKey());
            if (target == null) {
                target = new JsonObject();
                bones.add(bone.getKey(), target);
            }
            for (Map.Entry<String, JsonElement> transform : bone.getValue().getAsJsonObject().entrySet()) {
                JsonArray original = target.getAsJsonArray(transform.getKey());
                JsonArray replacement = transform.getValue().getAsJsonArray();
                JsonArray vector = new JsonArray();
                for (int i = 0; i < replacement.size(); i++) {
                    String vanilla = original == null ? "0" : original.get(i).getAsString();
                    vector.add(new JsonPrimitive("variable.ce_java_spear ? (" + replacement.get(i).getAsString() + ") : (" + vanilla + ")"));
                }
                target.add(transform.getKey(), vector);
            }
        }
        animations.add("animation.humanoid.melee_spear_hold", hold);
        file.add("animations", animations);
        Path animationPath = root.resolve("animations/craftengine/shared/spear_player.animation.json");
        Files.createDirectories(animationPath.getParent());
        GsonHelper.writeJsonFile(file, animationPath);
    }
}
