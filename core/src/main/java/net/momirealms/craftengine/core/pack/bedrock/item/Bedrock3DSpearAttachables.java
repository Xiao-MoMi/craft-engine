package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.VectorUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class Bedrock3DSpearAttachables {
    private Bedrock3DSpearAttachables() {
    }

    static void write(ItemRegistrationPlan.Entry entry, BedrockSpearModels models, List<Bedrock3DItemAttachables.Stage> stages,
                      List<Bedrock3DItemAttachables.Material> materials, Path root) throws IOException {
        JsonArray bones = new JsonArray();
        String parent = "ce_slot";
        // rotateAround 拆成支点平移、旋转、反向平移，保持 Java 的 X/Y 顺序及左右手支点。
        for (String name : List.of("move", "xp", "x", "xu", "yp", "y", "yu", "feedback")) {
            JsonObject bone = new JsonObject();
            bone.addProperty("name", "ce_spear_" + name);
            bone.addProperty("parent", parent);
            bone.add("pivot", VectorUtils.toJson(0, 8, 0));
            bones.add(bone);
            parent = "ce_spear_" + name;
        }
        ItemBehaviorDescription behavior = entry.source().clientBehavior();
        JsonArray scripts = models.scripts(behavior);
        ItemBehaviorDescription.Kinetic kinetic = behavior.kinetic();
        scripts.add("variable.ce_spear_delay = " + (kinetic == null ? 0 : kinetic.delayTicks()) + ";");
        scripts.add("variable.ce_spear_dismount = " + (kinetic == null || kinetic.dismount() == null ? 0 : kinetic.dismount().durationTicks()) + ";");
        scripts.add("variable.ce_spear_knockback = " + (kinetic == null || kinetic.knockback() == null ? 0 : kinetic.knockback().durationTicks()) + ";");
        scripts.add("variable.ce_spear_damage = " + (kinetic == null || kinetic.damage() == null ? 0 : kinetic.damage().durationTicks()) + ";");
        scripts.add("variable.ce_spear_forward = " + (kinetic == null ? 0 : kinetic.forwardMovement()) + ";");
        scripts.add("variable.ce_spear_use = " + (kinetic != null && behavior.use().animation() == ItemBehaviorDescription.UseAnimation.SPEAR ? "variable.ce_using_item" : "0") + ";");
        scripts.add("variable.ce_spear_attack = " + (behavior.attack().animation() == ItemBehaviorDescription.SwingAnimation.STAB ? "!variable.ce_using_item && variable.attack_time > 0" : "0") + ";");
        JsonObject source = GsonHelper.readJsonFromResource("/bedrock/spear/java_spear.json").getAsJsonObject();
        scripts.addAll(source.getAsJsonArray("pre_animation"));
        JsonArray animate = new JsonArray();
        for (JsonElement value : source.getAsJsonArray("animate")) {
            animate.add(value);
        }
        Bedrock3DItemAttachables.write(entry.bedrockId(), stages, materials, scripts, bones, source.getAsJsonObject("animations"), animate, root);
    }
}
