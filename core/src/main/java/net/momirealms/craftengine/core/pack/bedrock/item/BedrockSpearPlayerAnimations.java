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
import java.util.Map;
import java.util.Set;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.PLAYER_ANIMATION_CONTROLLER;

public final class BedrockSpearPlayerAnimations {
    private BedrockSpearPlayerAnimations() {
    }

    public static void write(Set<Key> javaSpears, Path root) throws IOException {
        if (javaSpears.isEmpty()) {
            return;
        }
        StringBuilder condition = new StringBuilder("query.is_item_name_any('slot.weapon.mainhand'");
        javaSpears.stream().sorted(java.util.Comparator.comparing(Key::asString)).forEach(id -> condition.append(", '").append(id.asString()).append('\''));
        condition.append(')');
        Path path = root.resolve(PLAYER_ANIMATION_CONTROLLER);
        JsonObject controllers;
        if (Files.exists(path)) {
            controllers = GsonHelper.readJsonFromFile(path).getAsJsonObject();
        } else {
            controllers = GsonHelper.readJsonFromResource("/bedrock/player/root.animation_controllers.json").getAsJsonObject();
        }
        JsonArray animations = controllers.getAsJsonObject("animation_controllers")
                .getAsJsonObject("controller.animation.player.root").getAsJsonObject("states")
                .getAsJsonObject("first_person").getAsJsonArray("animations");
        for (JsonElement animation : animations) {
            if (animation instanceof JsonObject entry && entry.has("first_person_melee_spear_controller")) {
                // 三维长矛由 attachable 实现 Java 姿态，不能再叠加原版第一人称长矛变换。
                entry.addProperty("first_person_melee_spear_controller", "(" + entry.get("first_person_melee_spear_controller").getAsString() + ") && !(" + condition + ")");
            }
        }
        Files.createDirectories(path.getParent());
        GsonHelper.writeJsonFile(controllers, path);

        JsonObject file = GsonHelper.readJsonFromResource("/bedrock/spear/player.animation.json").getAsJsonObject();
        for (JsonElement value : file.getAsJsonObject("animations").asMap().values()) {
            JsonObject item = value.getAsJsonObject().getAsJsonObject("bones").getAsJsonObject("rightitem");
            // 保留玩家举矛手臂，但去掉三维长矛挂点上的额外偏移；原版和二维长矛不变。
            for (Map.Entry<String, JsonElement> transform : item.entrySet()) {
                JsonArray vector = transform.getValue().getAsJsonArray();
                for (int i = 0; i < vector.size(); i++) {
                    vector.set(i, new JsonPrimitive("(" + condition + ") ? 0 : (" + vector.get(i).getAsString() + ")"));
                }
            }
        }
        Path animationPath = root.resolve("animations/craftengine/shared/spear_player.animation.json");
        Files.createDirectories(animationPath.getParent());
        GsonHelper.writeJsonFile(file, animationPath);
    }
}
