package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;

public final class BedrockCrossbowPlayerAnimations {
    private final Map<Key, Integer> chargeDurations = new LinkedHashMap<>();

    public void register(Key unloadedItem, int chargeTicks) {
        this.chargeDurations.put(unloadedItem, chargeTicks);
    }

    public void write(Path root) throws IOException {
        if (this.chargeDurations.isEmpty()) {
            return;
        }
        JsonObject controllers = GsonHelper.readJsonFromResource("/bedrock/player/root.animation_controllers.json").getAsJsonObject();
        JsonArray animations = controllers.getAsJsonObject("animation_controllers")
                .getAsJsonObject("controller.animation.player.root").getAsJsonObject("states")
                .getAsJsonObject("first_person").getAsJsonArray("animations");
        StringBuilder duration = new StringBuilder();
        for (Map.Entry<Key, Integer> entry : this.chargeDurations.entrySet()) {
            duration.append("query.is_item_name_any('slot.weapon.mainhand', '").append(entry.getKey().asString())
                    .append("') ? ").append(entry.getValue()).append(" : (");
        }
        duration.append('0').append(")".repeat(this.chargeDurations.size()));
        for (JsonElement animation : animations) {
            if (animation instanceof JsonObject entry && entry.has("first_person_crossbow_equipped")) {
                String vanilla = entry.get("first_person_crossbow_equipped").getAsString();
                // 在玩家骨骼上先执行原版装填姿态，attachable 再应用自身的 wield，避免变换顺序颠倒。
                entry.addProperty("first_person_crossbow_equipped",
                        "variable.ce_crossbow_charge_ticks = " + duration + "; "
                                + "variable.ce_crossbow_loading = variable.ce_crossbow_charge_ticks > 0 && query.is_using_item && query.main_hand_item_use_duration > 0; "
                                + "variable.ce_crossbow_remaining = variable.ce_crossbow_loading ? math.clamp(1 - (query.main_hand_item_max_duration - query.main_hand_item_use_duration) / variable.ce_crossbow_charge_ticks, 0, 1) : 0; "
                                + "return (" + vanilla + ") || variable.ce_crossbow_loading;");
                break;
            }
        }
        Path controllerPath = root.resolve("animation_controllers/craftengine_player.animation_controllers.json");
        Files.createDirectories(controllerPath.getParent());
        GsonHelper.writeJsonFile(controllers, controllerPath);

        JsonObject animationFile = GsonHelper.readJsonFromResource("/bedrock/player/crossbow.animation.json").getAsJsonObject();
        JsonObject bones = animationFile.getAsJsonObject("animations")
                .getAsJsonObject("animation.player.first_person.crossbow_equipped").getAsJsonObject("bones");
        // 自定义弩的通用使用时长不是装填时长；仅替换这类物品的进度，原版弩仍使用原来的查询。
        String remaining = "(variable.ce_crossbow_loading ? variable.ce_crossbow_remaining : variable.item_use_normalized)";
        JsonArray leftPosition = bones.getAsJsonObject("leftarm").getAsJsonArray("position");
        leftPosition.set(0, new JsonPrimitive("1.5 - " + remaining + " * 3.5"));
        leftPosition.set(2, new JsonPrimitive("8.25 - (1 - " + remaining + ")"));
        remaining = "(variable.ce_crossbow_loading ? variable.ce_crossbow_remaining : query.item_remaining_use_duration('main_hand', 1.0))";
        JsonObject item = bones.getAsJsonObject("rightitem");
        item.getAsJsonArray("position").set(2, new JsonPrimitive("2.5 - " + remaining + " * 1.5"));
        item.getAsJsonArray("scale").set(1, new JsonPrimitive("1.15 - " + remaining + " * 0.15"));
        Path animationPath = root.resolve("animations/craftengine_crossbow_player.animation.json");
        Files.createDirectories(animationPath.getParent());
        GsonHelper.writeJsonFile(animationFile, animationPath);
        try (InputStream notice = BedrockCrossbowPlayerAnimations.class.getResourceAsStream("/bedrock/player/NOTICE.txt")) {
            Files.copy(notice, root.resolve("craftengine_player_animation_NOTICE.txt"), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
