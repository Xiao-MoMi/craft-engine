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
import java.util.Set;

public final class BedrockBowPlayerAnimations {
    private BedrockBowPlayerAnimations() {
    }

    static void write(Set<Key> bows, Set<Key> javaBows, BedrockPlayerAnimationController controller, Path root) throws IOException {
        if (bows.isEmpty()) {
            return;
        }
        String active = "(" + condition(bows) + " && query.is_using_item && query.main_hand_item_use_duration > 0)";
        JsonArray animations = controller.animations("third_person");
        for (JsonElement value : animations) {
            if (!(value instanceof JsonObject animation)) {
                continue;
            }
            // 自定义 ID 不会命中原版 bow 判断；举弓时也要排除通用使用动作，避免手臂姿态叠加。
            if (animation.has("third_person_bow_equipped")) {
                String original = animation.get("third_person_bow_equipped").getAsString();
                animation.addProperty("third_person_bow_equipped", "(" + original + ") || " + active);
            } else if (animation.has("use_item_progress")) {
                String original = animation.get("use_item_progress").getAsString();
                animation.addProperty("use_item_progress", "(" + original + ") && !" + active);
            }
        }
        if (!javaBows.isEmpty()) {
            JsonObject file = GsonHelper.readJsonFromResource("/bedrock/player/bow.animation.json").getAsJsonObject();
            JsonArray rotation = file.getAsJsonObject("animations").getAsJsonObject("animation.player.bow_equipped")
                    .getAsJsonObject("bones").getAsJsonObject("rightitem").getAsJsonArray("rotation");
            // Java display 已定义物品局部姿态，三维弓只复用双臂动作，不叠加原版挂点旋转。
            rotation.set(1, new JsonPrimitive(condition(javaBows) + " ? 0 : " + rotation.get(1).getAsString()));
            Path animationPath = root.resolve("animations/craftengine/shared/bow_player.animation.json");
            Files.createDirectories(animationPath.getParent());
            GsonHelper.writeJsonFile(file, animationPath);
        }
    }

    private static String condition(Set<Key> bows) {
        StringBuilder names = new StringBuilder("query.is_item_name_any('slot.weapon.mainhand'");
        bows.stream().sorted(Comparator.comparing(Key::asString)).forEach(id -> names.append(", '").append(id.asString()).append('\''));
        return names.append(')').toString();
    }
}
