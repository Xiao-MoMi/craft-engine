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
    private BedrockShieldPlayerAnimations() {
    }

    public static void write(Set<Key> shields, Path root) throws IOException {
        if (shields.isEmpty()) {
            return;
        }
        StringBuilder names = new StringBuilder();
        shields.stream().sorted(Comparator.comparing(Key::asString)).forEach(id -> names.append(", '").append(id.asString()).append('\''));
        String main = "query.blocking && !query.is_emerging && query.is_item_name_any('slot.weapon.mainhand'" + names + ")";
        String off = "query.blocking && query.is_emerging && query.is_item_name_any('slot.weapon.offhand'" + names + ")";
        String active = "((" + main + ") || (" + off + "))";
        Path controllerPath = root.resolve("animation_controllers/craftengine_player.animation_controllers.json");
        JsonObject controllers;
        if (Files.exists(controllerPath)) {
            controllers = GsonHelper.readJsonFromFile(controllerPath).getAsJsonObject();
        } else {
            controllers = GsonHelper.readJsonFromResource("/bedrock/player/root.animation_controllers.json").getAsJsonObject();
        }
        JsonObject states = controllers.getAsJsonObject("animation_controllers")
                .getAsJsonObject("controller.animation.player.root").getAsJsonObject("states");
        for (String state : List.of("first_person", "third_person")) {
            for (JsonElement value : states.getAsJsonObject(state).getAsJsonArray("animations")) {
                if (!(value instanceof JsonObject animation)) {
                    continue;
                }
                for (Map.Entry<String, JsonElement> entry : animation.entrySet()) {
                    String original = entry.getValue().getAsString();
                    switch (entry.getKey()) {
                        case "first_person_swap_item" -> entry.setValue(new JsonPrimitive("(" + original + ") || " + active));
                        case "first_person_shield_block" -> entry.setValue(new JsonPrimitive("(" + original + ") && !" + active));
                        case "shield_block_main_hand" -> entry.setValue(new JsonPrimitive("((" + original + ") && !" + active + ") || (" + main + ")"));
                        case "shield_block_off_hand" -> entry.setValue(new JsonPrimitive("((" + original + ") && !" + active + ") || (" + off + ")"));
                    }
                }
            }
        }
        Files.createDirectories(controllerPath.getParent());
        GsonHelper.writeJsonFile(controllers, controllerPath);

        JsonObject file = GsonHelper.readJsonFromResource("/bedrock/player/shield.animation.json").getAsJsonObject();
        adaptHand(file, "main_hand", "right", main, -30);
        adaptHand(file, "off_hand", "left", off, 30);
        Path animationPath = root.resolve("animations/craftengine_shield_player.animation.json");
        Files.createDirectories(animationPath.getParent());
        GsonHelper.writeJsonFile(file, animationPath);
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
