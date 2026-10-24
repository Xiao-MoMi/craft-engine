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
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.PLAYER_ANIMATION_CONTROLLER;
import static net.momirealms.craftengine.bedrock.BedrockPackLayout.PLAYER_ANIMATION_NOTICE;

public final class BedrockCrossbowPlayerAnimations {
    private final Map<Key, Integer> chargeDurations = new LinkedHashMap<>();
    private final Set<Key> chargedItems = new LinkedHashSet<>();
    private final Set<Key> javaItems = new LinkedHashSet<>();

    public void register(Key item, boolean charged, int chargeTicks, boolean javaDisplay) {
        if (charged) {
            this.chargedItems.add(item);
        } else {
            this.chargeDurations.put(item, chargeTicks);
        }
        if (javaDisplay) {
            this.javaItems.add(item);
        }
    }

    public void write(Path root) throws IOException {
        if (this.chargeDurations.isEmpty()) {
            return;
        }
        Path controllerPath = root.resolve(PLAYER_ANIMATION_CONTROLLER);
        JsonObject controllers;
        if (Files.exists(controllerPath)) {
            controllers = GsonHelper.readJsonFromFile(controllerPath).getAsJsonObject();
        } else {
            controllers = GsonHelper.readJsonFromResource("/bedrock/player/root.animation_controllers.json").getAsJsonObject();
        }
        JsonObject states = controllers.getAsJsonObject("animation_controllers")
                .getAsJsonObject("controller.animation.player.root").getAsJsonObject("states");
        JsonArray animations = states.getAsJsonObject("first_person").getAsJsonArray("animations");
        for (JsonElement animation : animations) {
            if (animation instanceof JsonObject entry && entry.has("first_person_crossbow_equipped")) {
                String vanilla = entry.get("first_person_crossbow_equipped").getAsString();
                // 在玩家骨骼上先执行原版装填姿态，attachable 再应用自身的 wield，避免变换顺序颠倒。
                entry.addProperty("first_person_crossbow_equipped",
                        "variable.ce_crossbow_charge_ticks = " + this.duration(true) + "; "
                                + "variable.ce_crossbow_loading = variable.ce_crossbow_charge_ticks > 0 && query.is_using_item && query.main_hand_item_use_duration > 0; "
                                + "variable.ce_crossbow_remaining = variable.ce_crossbow_loading ? math.clamp(1 - (query.main_hand_item_max_duration - query.main_hand_item_use_duration) / variable.ce_crossbow_charge_ticks, 0, 1) : 0; "
                                + "return (" + vanilla + ") || variable.ce_crossbow_loading;");
                break;
            }
        }
        animations = states.getAsJsonObject("third_person").getAsJsonArray("animations");
        String unloaded = condition(this.chargeDurations.keySet());
        String charged = condition(this.chargedItems);
        for (JsonElement animation : animations) {
            if (!(animation instanceof JsonObject entry)) {
                continue;
            }
            if (entry.has("crossbow_controller")) {
                String vanilla = entry.get("crossbow_controller").getAsString();
                // 弹药由已注册的物品分支判断，装填进度按 Java 装填时长计算，不依赖原版物品名。
                entry.addProperty("crossbow_controller",
                        "variable.ce_crossbow_charge_ticks = " + this.duration(false) + "; "
                                + "variable.ce_crossbow_loading = variable.ce_crossbow_charge_ticks > 0 && query.is_using_item && query.main_hand_item_use_duration > 0; "
                                + "variable.ce_crossbow_remaining = variable.ce_crossbow_loading ? math.clamp(1 - (query.main_hand_item_max_duration - query.main_hand_item_use_duration) / variable.ce_crossbow_charge_ticks, 0, 1) : 0; "
                                + "variable.ce_crossbow_charged = " + charged + "; return " + vanilla + ";");
            } else if (entry.has("use_item_progress")) {
                String vanilla = entry.get("use_item_progress").getAsString();
                entry.addProperty("use_item_progress", "(" + vanilla + ") && !((" + unloaded
                        + " && query.is_using_item && query.main_hand_item_use_duration > 0) || " + charged + ")");
            }
        }
        JsonObject loadingAnimation = new JsonObject();
        loadingAnimation.addProperty("third_person_crossbow_equipped", "variable.ce_crossbow_loading");
        animations.add(loadingAnimation);
        JsonObject holdAnimation = new JsonObject();
        holdAnimation.addProperty("crossbow_hold", "variable.ce_crossbow_charged");
        animations.add(holdAnimation);
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
        JsonArray leftRotation = animationFile.getAsJsonObject("animations")
                .getAsJsonObject("animation.player.crossbow_equipped").getAsJsonObject("bones")
                .getAsJsonObject("leftarm").getAsJsonArray("rotation");
        remaining = "(variable.ce_crossbow_loading ? variable.ce_crossbow_remaining : variable.item_use_normalized)";
        for (int i = 0; i < leftRotation.size(); i++) {
            leftRotation.set(i, new JsonPrimitive(leftRotation.get(i).getAsString().replace("variable.item_use_normalized", remaining)));
        }
        Path animationPath = root.resolve("animations/craftengine/shared/crossbow_player.animation.json");
        Files.createDirectories(animationPath.getParent());
        GsonHelper.writeJsonFile(animationFile, animationPath);
        try (InputStream notice = BedrockCrossbowPlayerAnimations.class.getResourceAsStream("/bedrock/player/NOTICE.txt")) {
            Path noticePath = root.resolve(PLAYER_ANIMATION_NOTICE);
            Files.createDirectories(noticePath.getParent());
            Files.copy(notice, noticePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String duration(boolean firstPerson) {
        StringBuilder duration = new StringBuilder();
        int count = 0;
        for (Map.Entry<Key, Integer> entry : this.chargeDurations.entrySet()) {
            if (firstPerson && this.javaItems.contains(entry.getKey())) {
                continue;
            }
            duration.append("query.is_item_name_any('slot.weapon.mainhand', '").append(entry.getKey().asString())
                    .append("') ? ").append(entry.getValue()).append(" : (");
            count++;
        }
        return duration.append('0').append(")".repeat(count)).toString();
    }

    private static String condition(Set<Key> items) {
        if (items.isEmpty()) {
            return "0";
        }
        StringBuilder names = new StringBuilder("query.is_item_name_any('slot.weapon.mainhand'");
        for (Key item : items) {
            names.append(", '").append(item.asString()).append('\'');
        }
        return names.append(')').toString();
    }
}
