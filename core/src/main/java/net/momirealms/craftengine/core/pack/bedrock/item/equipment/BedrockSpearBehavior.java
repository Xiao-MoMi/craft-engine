package net.momirealms.craftengine.core.pack.bedrock.item.equipment;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.momirealms.craftengine.core.entity.display.ItemDisplayContext;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemGenerationContext;
import net.momirealms.craftengine.core.pack.bedrock.item.GeneratedItemVariant;
import net.momirealms.craftengine.core.pack.bedrock.item.animation.BedrockPlayerAnimations;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBehaviorDescription;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRegistrationPlan;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemIconRenderer;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.ImageUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.VectorUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class BedrockSpearBehavior extends BedrockEquipmentBehavior {
    static final BedrockSpearBehavior INSTANCE = new BedrockSpearBehavior();

    private BedrockSpearBehavior() {
    }

    @Override
    GeneratedItemVariant tryWriteSpecialized(BedrockItemPresentation plan,
                                             JavaModelResources resources,
                                             BedrockItemIconRenderer renderer,
                                             BedrockItemGenerationContext context) throws IOException {
        ItemBindingRule inventory = plan.registration();
        if (!inventory.transformations().isEmpty() || !(inventory.target() instanceof ItemRenderTarget.Model target)) {
            return null;
        }
        BufferedImage icon = renderer.render(target);
        if (icon == null) {
            return null;
        }
        Map<ItemBindingRule, StringBuilder> hands = new LinkedHashMap<>();
        for (ItemDisplayContext view : List.of(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND, ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, ItemDisplayContext.THIRD_PERSON_LEFT_HAND)) {
            for (ItemBindingRule rule : plan.variant().rules().get(view)) {
                if (rule.target() == ItemRenderTarget.Builtin.EMPTY) {
                    continue;
                }
                if (!(rule.target() instanceof ItemRenderTarget.Model) || !rule.transformations().isEmpty()) {
                    return null;
                }
                StringBuilder views = hands.computeIfAbsent(rule, ignored -> new StringBuilder());
                if (!views.isEmpty()) {
                    views.append(" || ");
                }
                views.append('(').append(BedrockDisplayContextItems.equippedView(view).condition).append(')');
            }
        }
        if (hands.isEmpty()) {
            return null;
        }
        List<BufferedImage> stages = new ArrayList<>(hands.size());
        List<String> conditions = new ArrayList<>(hands.size());
        for (Map.Entry<ItemBindingRule, StringBuilder> hand : hands.entrySet()) {
            BufferedImage image = renderer.render((ItemRenderTarget.Model) hand.getKey().target());
            if (image == null) {
                return null;
            }
            stages.add(image);
            conditions.add("(" + hand.getValue() + ") && (" + this.stageCondition(hand.getKey()) + ")");
        }
        String path = "textures/" + plan.id().namespace() + '/' + plan.id().value() + "/icon";
        ImageUtils.writePng(icon, context.root().resolve(path + ".png"));
        Bedrock2DSpearAttachables.write(plan, conditions, stages, context.root());
        return GeneratedItemVariant.icon(plan.id(), inventory, path, true);
    }

    @Override
    BedrockEquipmentPose pose(ItemRegistrationPlan.Entry entry) throws IOException {
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
        JsonArray scripts = useScripts(behavior.use());
        // 挥手进度属于玩家实体，attachable 需从持有者读取。
        scripts.add("variable.attack_time = context.owning_entity->variable.attack_time;");
        poseScripts(entry).forEach(scripts::add);
        scripts.add("variable.ce_spear_side = context.item_slot == 'off_hand' ? -1 : 1;");
        scripts.add("variable.ce_spear_time = variable.ce_spear_use ? math.max(0, variable.ce_use_ticks + query.frame_alpha - (context.is_first_person ? 1 : 0)) : 0;");
        scripts.add("variable.ce_spear_hit_ticks = context.owning_entity->query.ticks_since_last_kinetic_weapon_hit;");
        JsonObject source = GsonHelper.readJsonFromResource("/bedrock/item/spear/handheld.pose.json").getAsJsonObject();
        // Java 缓动公式在资源中显式计算，不能假定 Molang 同名函数与 Java 等价。
        scripts.addAll(source.getAsJsonArray("pre_animation"));
        JsonArray animate = source.getAsJsonArray("animate");
        return new BedrockEquipmentPose(scripts, bones, source.getAsJsonObject("animations"), animate, BedrockEquipmentPose.HANDS);
    }

    @Override
    void registerHandModel(BedrockPlayerAnimations animations, Key id, ItemRegistrationPlan.Entry entry) {
        animations.registerSpearUsingJavaPose(id, poseScripts(entry));
    }

    private static List<String> poseScripts(ItemRegistrationPlan.Entry entry) {
        List<String> scripts = new ArrayList<>(7);
        ItemBehaviorDescription behavior = entry.clientBehavior();
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
