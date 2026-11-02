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
import java.util.Set;

public final class BedrockShieldPlayerAnimations {
    private static final String MAIN_HAND = "ce_main_hand_shield";
    private static final String OFF_HAND = "ce_off_hand_shield";
    private static final String MAIN_BLOCKING = "variable." + MAIN_HAND;
    private static final String OFF_BLOCKING = "variable." + OFF_HAND;
    private static final String BLOCKING = "(" + MAIN_BLOCKING + " || " + OFF_BLOCKING + ")";

    private BedrockShieldPlayerAnimations() {
    }

    static void write(Set<Key> shields, BedrockPlayerAnimationController controller, Path root) throws IOException {
        if (shields.isEmpty()) {
            return;
        }
        StringBuilder names = new StringBuilder();
        shields.stream().sorted(Comparator.comparing(Key::asString)).forEach(id -> names.append(", '").append(id.asString()).append('\''));
        String main = "query.blocking && !query.is_emerging && query.is_item_name_any('slot.weapon.mainhand'" + names + ")";
        String off = "query.blocking && query.is_emerging && query.is_item_name_any('slot.weapon.offhand'" + names + ")";
        for (String view : List.of("first_person", "third_person")) {
            JsonObject state = controller.state(view);
            // 视角切换或解除格挡时，当前状态在处理骨骼前重新计算双手条件。
            JsonObject variables = state.asMap().computeIfAbsent("variables", ignored -> new JsonObject()).getAsJsonObject();
            variables.add(MAIN_HAND, booleanVariable(main));
            variables.add(OFF_HAND, booleanVariable(off));
            updateBlockingAnimations(state.getAsJsonArray("animations"));
        }

        JsonObject file = GsonHelper.readJsonFromResource("/bedrock/player/shield.animation.json").getAsJsonObject();
        adaptHand(file, "main_hand", "right", MAIN_BLOCKING, -30);
        adaptHand(file, "off_hand", "left", OFF_BLOCKING, 30);
        Path animationPath = root.resolve("animations/craftengine/shared/shield_player.animation.json");
        Files.createDirectories(animationPath.getParent());
        GsonHelper.writeJsonFile(file, animationPath);
    }

    private static JsonObject booleanVariable(String condition) {
        JsonObject curve = new JsonObject();
        curve.addProperty("0.0", 0);
        curve.addProperty("1.0", 1);
        JsonObject variable = new JsonObject();
        variable.addProperty("input", condition);
        variable.add("remap_curve", curve);
        return variable;
    }

    private static void updateBlockingAnimations(JsonArray animations) {
        for (JsonElement value : animations) {
            if (!(value instanceof JsonObject animation)) {
                continue;
            }
            for (Map.Entry<String, JsonElement> entry : animation.entrySet()) {
                String original = "(" + entry.getValue().getAsString() + ")";
                switch (entry.getKey()) {
                    case "first_person_swap_item" -> entry.setValue(new JsonPrimitive(original + " || " + BLOCKING));
                    case "first_person_shield_block" -> entry.setValue(new JsonPrimitive(original + " && !" + BLOCKING));
                    case "shield_block_main_hand" -> entry.setValue(new JsonPrimitive("(" + original + " && !" + BLOCKING + ") || " + MAIN_BLOCKING));
                    case "shield_block_off_hand" -> entry.setValue(new JsonPrimitive("(" + original + " && !" + BLOCKING + ") || " + OFF_BLOCKING));
                }
            }
        }
    }

    private static void adaptHand(JsonObject file, String hand, String side, String condition, int yaw) {
        JsonObject bones = file.getAsJsonObject("animations").getAsJsonObject("animation.player.shield_block_" + hand).getAsJsonObject("bones");
        JsonArray rotation = bones.getAsJsonObject(side + "arm").getAsJsonArray("rotation");
        // Java 26.3 HumanoidModel.poseBlockingArm：手臂俯仰减半，再叠加格挡角度及受限头部朝向。
        rotation.set(0, new JsonPrimitive("(" + condition + ") ? this * -0.5 - 54 + math.clamp(query.target_x_rotation, -80, 25) : (" + rotation.get(0).getAsString() + ")"));
        rotation.set(1, new JsonPrimitive("(" + condition + ") ? " + yaw + " + math.clamp(query.target_y_rotation, -30, 30) - this : (" + rotation.get(1).getAsString() + ")"));
        rotation.set(2, new JsonPrimitive("(" + condition + ") ? 0 : (" + rotation.get(2).getAsString() + ")"));
        // Java display 已定义盾牌局部姿态，不能再叠加原版盾牌对持物挂点的偏移。
        for (JsonElement vector : bones.getAsJsonObject(side + "item").asMap().values()) {
            JsonArray values = vector.getAsJsonArray();
            for (int i = 0; i < values.size(); i++) {
                values.set(i, new JsonPrimitive("(" + condition + ") ? 0 : (" + values.get(i).getAsString() + ")"));
            }
        }
    }
}
