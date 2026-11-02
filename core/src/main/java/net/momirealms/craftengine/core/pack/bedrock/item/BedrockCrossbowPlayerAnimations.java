package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
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

import static net.momirealms.craftengine.bedrock.BedrockPackLayout.PLAYER_ANIMATION_NOTICE;

public final class BedrockCrossbowPlayerAnimations {
    private static final String USING_MAIN_HAND = "query.is_using_item && query.main_hand_item_use_duration > 0";
    private final Map<Key, Integer> unloadedChargeTicks = new LinkedHashMap<>();
    private final Set<Key> chargedCrossbows = new LinkedHashSet<>();
    private final Set<Key> crossbowsUsingJavaPose = new LinkedHashSet<>();

    void register2D(BedrockCrossbowStates.State state, int chargeTicks) {
        this.registerChargeState(state, chargeTicks);
    }

    void register3D(BedrockCrossbowStates.State state, int chargeTicks) {
        this.registerChargeState(state, chargeTicks);
        this.crossbowsUsingJavaPose.add(state.id());
    }

    private void registerChargeState(BedrockCrossbowStates.State state, int chargeTicks) {
        if (state.charged()) {
            this.chargedCrossbows.add(state.id());
        } else {
            this.unloadedChargeTicks.put(state.id(), chargeTicks);
        }
    }

    void write(BedrockPlayerAnimationController controller, Path root) throws IOException {
        if (this.unloadedChargeTicks.isEmpty()) {
            return;
        }
        this.updateFirstPersonController(controller.state("first_person"));
        this.updateThirdPersonController(controller.state("third_person"));
        writePlayerAnimationFile(root);
        copyAnimationNotice(root);
    }

    private void updateFirstPersonController(BedrockPlayerAnimationState state) {
        // 三维弩的第一人称姿态由 attachable 处理；其余弩先在玩家骨骼上应用装填动作。
        state.updateFirstCondition("first_person_crossbow_equipped",
                original -> this.loadingVariables(this.crossbowsUsingJavaPose) + "return (" + original + ") || variable.ce_crossbow_loading;");
    }

    private void updateThirdPersonController(BedrockPlayerAnimationState state) {
        String unloaded = mainHandCondition(this.unloadedChargeTicks.keySet());
        String charged = mainHandCondition(this.chargedCrossbows);
        state.updateCondition("crossbow_controller",
                original -> this.loadingVariables(Set.of()) + "variable.ce_crossbow_charged = " + charged + "; return " + original + ";");
        state.updateConditionUnlessPresent("use_item_progress", "crossbow_controller",
                original -> "(" + original + ") && !((" + unloaded + " && " + USING_MAIN_HAND + ") || " + charged + ")");
        state.appendAnimation("third_person_crossbow_equipped", "variable.ce_crossbow_loading");
        state.appendAnimation("crossbow_hold", "variable.ce_crossbow_charged");
    }

    private static void writePlayerAnimationFile(Path root) throws IOException {
        JsonObject animationFile = GsonHelper.readJsonFromResource("/bedrock/player/crossbow.animation.json").getAsJsonObject();
        JsonObject animations = animationFile.getAsJsonObject("animations");
        JsonObject firstPersonBones = animations
                .getAsJsonObject("animation.player.first_person.crossbow_equipped")
                .getAsJsonObject("bones");
        // 自定义弩的通用使用时长不是装填时长；仅替换这类物品的进度，原版弩仍使用原来的查询。
        String armProgress = "(variable.ce_crossbow_loading ? variable.ce_crossbow_remaining : variable.item_use_normalized)";
        JsonArray leftPosition = firstPersonBones.getAsJsonObject("leftarm").getAsJsonArray("position");
        leftPosition.set(0, new JsonPrimitive("1.5 - " + armProgress + " * 3.5"));
        leftPosition.set(2, new JsonPrimitive("8.25 - (1 - " + armProgress + ")"));

        String itemProgress = "(variable.ce_crossbow_loading ? variable.ce_crossbow_remaining : query.item_remaining_use_duration('main_hand', 1.0))";
        JsonObject rightItem = firstPersonBones.getAsJsonObject("rightitem");
        rightItem.getAsJsonArray("position").set(2, new JsonPrimitive("2.5 - " + itemProgress + " * 1.5"));
        rightItem.getAsJsonArray("scale").set(1, new JsonPrimitive("1.15 - " + itemProgress + " * 0.15"));

        JsonArray leftRotation = animations
                .getAsJsonObject("animation.player.crossbow_equipped")
                .getAsJsonObject("bones")
                .getAsJsonObject("leftarm")
                .getAsJsonArray("rotation");
        for (int i = 0; i < leftRotation.size(); i++) {
            leftRotation.set(i, new JsonPrimitive(leftRotation.get(i).getAsString().replace("variable.item_use_normalized", armProgress)));
        }
        Path animationPath = root.resolve("animations/craftengine/shared/crossbow_player.animation.json");
        Files.createDirectories(animationPath.getParent());
        GsonHelper.writeJsonFile(animationFile, animationPath);
    }

    private static void copyAnimationNotice(Path root) throws IOException {
        try (InputStream notice = BedrockCrossbowPlayerAnimations.class.getResourceAsStream("/bedrock/player/NOTICE.txt")) {
            Path noticePath = root.resolve(PLAYER_ANIMATION_NOTICE);
            Files.createDirectories(noticePath.getParent());
            Files.copy(notice, noticePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private String loadingVariables(Set<Key> excludedCrossbows) {
        return "variable.ce_crossbow_charge_ticks = " + this.chargeTicksExpression(excludedCrossbows) + "; "
                + "variable.ce_crossbow_loading = variable.ce_crossbow_charge_ticks > 0 && " + USING_MAIN_HAND + "; "
                + "variable.ce_crossbow_remaining = variable.ce_crossbow_loading ? math.clamp(1 - (query.main_hand_item_max_duration - query.main_hand_item_use_duration) / variable.ce_crossbow_charge_ticks, 0, 1) : 0; ";
    }

    private String chargeTicksExpression(Set<Key> excludedCrossbows) {
        StringBuilder duration = new StringBuilder();
        int count = 0;
        for (Map.Entry<Key, Integer> entry : this.unloadedChargeTicks.entrySet()) {
            if (excludedCrossbows.contains(entry.getKey())) {
                continue;
            }
            duration.append("query.is_item_name_any('slot.weapon.mainhand', '")
                    .append(entry.getKey().asString())
                    .append("') ? ")
                    .append(entry.getValue())
                    .append(" : (");
            count++;
        }
        return duration.append('0')
                .repeat(")", count)
                .toString();
    }

    private static String mainHandCondition(Set<Key> items) {
        if (items.isEmpty()) {
            return "0";
        }
        StringBuilder names = new StringBuilder("query.is_item_name_any('slot.weapon.mainhand'");
        for (Key item : items) {
            names.append(", '")
                    .append(item.asString())
                    .append('\'');
        }
        return names.append(')').toString();
    }
}
