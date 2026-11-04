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
import java.util.Set;

public final class BedrockShieldPlayerAnimations {
    private static final String MAIN_HAND = "ce_main_hand_shield";
    private static final String OFF_HAND = "ce_off_hand_shield";
    private static final String MAIN_BLOCKING = "variable." + MAIN_HAND;
    private static final String OFF_BLOCKING = "variable." + OFF_HAND;
    private static final String BLOCKING = "(" + MAIN_BLOCKING + " || " + OFF_BLOCKING + ")";

    private BedrockShieldPlayerAnimations() {
    }

    static void write(Set<Key> shieldsUsingJavaPose, BedrockPlayerAnimationController controller, Path root) throws IOException {
        if (shieldsUsingJavaPose.isEmpty()) {
            return;
        }
        updateBlockingControllers(controller, shieldsUsingJavaPose);
        writeBlockingPoses(root);
    }

    private static void updateBlockingControllers(BedrockPlayerAnimationController controller, Set<Key> shieldsUsingJavaPose) throws IOException {
        String names = BedrockPlayerAnimations.sortedItemArguments(shieldsUsingJavaPose);
        String mainHandCondition = "query.blocking && !query.is_emerging && query.is_item_name_any('slot.weapon.mainhand'" + names + ")";
        String offHandCondition = "query.blocking && query.is_emerging && query.is_item_name_any('slot.weapon.offhand'" + names + ")";
        for (String view : List.of("first_person", "third_person")) {
            BedrockPlayerAnimationState state = controller.state(view);
            // 切换视角或解除格挡时，在当前视角重新计算双手的格挡条件。
            state.setBooleanVariable(MAIN_HAND, mainHandCondition);
            state.setBooleanVariable(OFF_HAND, offHandCondition);
            updateBlockingAnimations(state);
        }
    }

    private static void writeBlockingPoses(Path root) throws IOException {
        JsonObject animationFile = GsonHelper.readJsonFromResource("/bedrock/player/shield.animation.json").getAsJsonObject();
        adaptHand(animationFile, "main_hand", "right", MAIN_BLOCKING, -30);
        adaptHand(animationFile, "off_hand", "left", OFF_BLOCKING, 30);
        Path animationPath = root.resolve("animations/craftengine/shared/shield_player.animation.json");
        Files.createDirectories(animationPath.getParent());
        GsonHelper.writeJsonFile(animationFile, animationPath);
    }

    private static void updateBlockingAnimations(BedrockPlayerAnimationState state) {
        state.updateCondition("first_person_swap_item",
                original -> "(" + original + ") || " + BLOCKING);
        state.updateCondition("first_person_shield_block",
                original -> "(" + original + ") && !" + BLOCKING);
        state.updateCondition("shield_block_main_hand",
                original -> "((" + original + ") && !" + BLOCKING + ") || " + MAIN_BLOCKING);
        state.updateCondition("shield_block_off_hand",
                original -> "((" + original + ") && !" + BLOCKING + ") || " + OFF_BLOCKING);
    }

    private static void adaptHand(JsonObject animationFile, String hand, String side, String condition, int yaw) {
        JsonObject bones = animationFile.getAsJsonObject("animations")
                .getAsJsonObject("animation.player.shield_block_" + hand)
                .getAsJsonObject("bones");
        JsonArray rotation = bones.getAsJsonObject(side + "arm").getAsJsonArray("rotation");
        // Java 26.3 HumanoidModel.poseBlockingArm：手臂俯仰减半，再叠加格挡角度及受限头部朝向。
        rotation.set(0, new JsonPrimitive("(" + condition + ") ? this * -0.5 - 54 + math.clamp(query.target_x_rotation, -80, 25) : ("
                + rotation.get(0).getAsString() + ")"));
        rotation.set(1, new JsonPrimitive("(" + condition + ") ? " + yaw + " + math.clamp(query.target_y_rotation, -30, 30) - this : ("
                + rotation.get(1).getAsString() + ")"));
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
