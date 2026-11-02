package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.VectorUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class Bedrock3DSpearAttachables {
    private Bedrock3DSpearAttachables() {
    }

    static void write(ItemRegistrationPlan.Entry entry,
                      BedrockHeldItemModels models,
                      List<Bedrock3DItemAttachables.Stage> stages,
                      List<Bedrock3DItemAttachables.Material> materials,
                      List<String> poseScripts,
                      Path root) throws IOException {
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
        ItemBehaviorDescription behavior = entry.clientBehavior();
        JsonArray scripts = models.scripts(behavior);
        // 挥手进度属于玩家实体，attachable 需从持有者读取。
        scripts.add("variable.attack_time = context.owning_entity->variable.attack_time;");
        poseScripts.forEach(scripts::add);
        scripts.add("variable.ce_spear_side = context.item_slot == 'off_hand' ? -1 : 1;");
        scripts.add("variable.ce_spear_time = variable.ce_spear_use ? math.max(0, variable.ce_use_ticks + query.frame_alpha - (context.is_first_person ? 1 : 0)) : 0;");
        scripts.add("variable.ce_spear_hit_ticks = context.owning_entity->query.ticks_since_last_kinetic_weapon_hit;");
        JsonObject source = GsonHelper.readJsonFromResource("/bedrock/spear/java_spear.json").getAsJsonObject();
        // Java 缓动公式在资源中显式计算，不能假定 Molang 同名函数与 Java 等价。
        scripts.addAll(source.getAsJsonArray("pre_animation"));
        JsonArray animate = source.getAsJsonArray("animate");
        Bedrock3DItemAttachables.write(entry.bedrockId(), stages, materials, scripts, bones, source.getAsJsonObject("animations"), animate, root);
    }
}
