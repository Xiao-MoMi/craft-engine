package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
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
        JsonElement component = entry.properties().components().get(DataComponentKeys.KINETIC_WEAPON);
        JsonObject kinetic = component == null || component.isJsonNull() ? null : component.getAsJsonObject();
        scripts.add("variable.ce_spear_delay = " + (kinetic == null || !kinetic.has("delay_ticks") ? 0 : kinetic.get("delay_ticks").getAsInt()) + ";");
        for (String phase : List.of("dismount", "knockback", "damage")) {
            int duration = 0;
            if (kinetic != null && kinetic.has(phase + "_conditions")) {
                duration = kinetic.getAsJsonObject(phase + "_conditions").get("max_duration_ticks").getAsInt();
            }
            scripts.add("variable.ce_spear_" + phase + " = " + duration + ";");
        }
        scripts.add("variable.ce_spear_forward = " + (kinetic == null || !kinetic.has("forward_movement") ? 0 : kinetic.get("forward_movement").getAsFloat()) + ";");
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
