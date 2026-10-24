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
        // Java 第三人称先应用突刺，再应用使用姿态；两组独立骨骼允许动作同时存在。
        for (String name : List.of("stab_move", "stab_xp", "stab_x", "stab_xu", "stab_feedback", "move", "xp", "x", "xu", "yp", "y", "yu", "feedback")) {
            JsonObject bone = new JsonObject();
            bone.addProperty("name", "ce_spear_" + name);
            bone.addProperty("parent", parent);
            bone.add("pivot", VectorUtils.toJson(0, 8, 0));
            bones.add(bone);
            parent = "ce_spear_" + name;
        }
        ItemBehaviorDescription behavior = entry.source().clientBehavior();
        JsonArray scripts = models.scripts(behavior);
        // 挥手进度属于玩家实体，attachable 需从持有者读取。
        scripts.add("variable.attack_time = context.owning_entity->variable.attack_time;");
        scripts.addAll(parameters(entry));
        scripts.add("variable.ce_spear_side = context.item_slot == 'off_hand' ? -1 : 1;");
        scripts.add("variable.ce_spear_time = variable.ce_spear_use ? math.max(0, variable.ce_use_ticks + query.frame_alpha - (context.is_first_person ? 1 : 0)) : 0;");
        scripts.add("variable.ce_spear_hit_ticks = context.owning_entity->query.ticks_since_last_kinetic_weapon_hit;");
        JsonObject source = GsonHelper.readJsonFromResource("/bedrock/spear/java_spear.json").getAsJsonObject();
        // Java 缓动公式在资源中显式计算，不能假定 Molang 同名函数与 Java 等价。
        scripts.addAll(source.getAsJsonArray("pre_animation"));
        JsonArray animate = source.getAsJsonArray("animate");
        Bedrock3DItemAttachables.write(entry.bedrockId(), stages, materials, scripts, bones, source.getAsJsonObject("animations"), animate, root);
    }

    static JsonArray parameters(ItemRegistrationPlan.Entry entry) {
        JsonArray scripts = new JsonArray();
        ItemBehaviorDescription behavior = entry.source().clientBehavior();
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
        scripts.add("variable.ce_spear_attack = " + (behavior.attack().animation() == ItemBehaviorDescription.SwingAnimation.STAB ? "variable.attack_time > 0" : "0") + ";");
        return scripts;
    }
}
