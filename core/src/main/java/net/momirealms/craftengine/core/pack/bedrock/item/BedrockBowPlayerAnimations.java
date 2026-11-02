package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
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

    static void write(Set<Key> bows, Set<Key> bowsUsingJavaPose, BedrockPlayerAnimationController controller, Path root) throws IOException {
        if (bows.isEmpty()) {
            return;
        }
        updateThirdPersonController(controller.state("third_person"), bows);
        if (!bowsUsingJavaPose.isEmpty()) {
            writeJavaBowPose(bowsUsingJavaPose, root);
        }
    }

    private static void updateThirdPersonController(BedrockPlayerAnimationState state, Set<Key> bows) {
        String usingBow = "(" + mainHandCondition(bows) + " && query.is_using_item && query.main_hand_item_use_duration > 0)";
        // 举弓时排除通用使用动作，避免手臂姿态叠加。
        state.updateCondition("third_person_bow_equipped",
                original -> "(" + original + ") || " + usingBow);
        state.updateConditionUnlessPresent("use_item_progress", "third_person_bow_equipped",
                original -> "(" + original + ") && !" + usingBow);
    }

    private static void writeJavaBowPose(Set<Key> bowsUsingJavaPose, Path root) throws IOException {
        JsonObject animationFile = GsonHelper.readJsonFromResource("/bedrock/player/bow.animation.json").getAsJsonObject();
        JsonArray rotation = animationFile.getAsJsonObject("animations")
                .getAsJsonObject("animation.player.bow_equipped")
                .getAsJsonObject("bones")
                .getAsJsonObject("rightitem")
                .getAsJsonArray("rotation");
        // Java display 已定义物品局部姿态，三维弓只复用双臂动作，不叠加原版挂点旋转。
        rotation.set(1, new JsonPrimitive(mainHandCondition(bowsUsingJavaPose) + " ? 0 : " + rotation.get(1).getAsString()));
        Path animationPath = root.resolve("animations/craftengine/shared/bow_player.animation.json");
        Files.createDirectories(animationPath.getParent());
        GsonHelper.writeJsonFile(animationFile, animationPath);
    }

    private static String mainHandCondition(Set<Key> bows) {
        StringBuilder names = new StringBuilder("query.is_item_name_any('slot.weapon.mainhand'");
        bows.stream()
                .sorted(Comparator.comparing(Key::asString))
                .forEach(id -> names.append(", '")
                        .append(id.asString())
                        .append('\''));
        return names.append(')').toString();
    }
}
