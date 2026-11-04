package net.momirealms.craftengine.core.pack.bedrock.item.equipment;

import com.google.gson.JsonArray;
import net.momirealms.craftengine.core.item.ItemKeys;
import net.momirealms.craftengine.core.pack.bedrock.item.BedrockItemGenerationContext;
import net.momirealms.craftengine.core.pack.bedrock.item.GeneratedItemVariant;
import net.momirealms.craftengine.core.pack.bedrock.item.animation.BedrockPlayerAnimations;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBehaviorDescription;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingCondition;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRegistrationPlan;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemIconRenderer;
import net.momirealms.craftengine.core.pack.bedrock.model.JavaModelResources;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

class BedrockEquipmentBehavior {
    static final BedrockEquipmentBehavior DEFAULT = new BedrockEquipmentBehavior();

    static List<? extends BedrockEquipmentBehavior> forItem(ItemRegistrationPlan.Entry entry, boolean hasViewModels) {
        Key type = entry.javaItemType();
        if (type.equals(ItemKeys.CROSSBOW) && entry.chargeDuration() != null && entry.chargeDuration().value() > 0) {
            return BedrockCrossbowBehavior.STATES;
        }
        if (type.equals(ItemKeys.BOW) && (hasViewModels || BedrockItemPresentation.hasUseConditions(entry.definition().rules()))) {
            return List.of(BedrockBowBehavior.INSTANCE);
        }
        if (type.equals(ItemKeys.SHIELD)) {
            return List.of(BedrockShieldBehavior.INSTANCE);
        }
        if (type.equals(ItemKeys.TRIDENT)) {
            return List.of(BedrockTridentBehavior.INSTANCE);
        }
        for (Key spear : ItemKeys.SPEARS) {
            if (type.equals(spear)) {
                return List.of(BedrockSpearBehavior.INSTANCE);
            }
        }
        return List.of(DEFAULT);
    }

    boolean accepts(ItemBindingRule rule) {
        return true;
    }

    boolean isEquipmentCondition(ItemBindingCondition condition) {
        return BedrockItemAttachables.isUseCondition(condition);
    }

    boolean matchesIdle(ItemBindingRule rule) {
        return BedrockItemAttachables.matchesIdle(rule);
    }

    String stageCondition(ItemBindingRule rule) {
        return BedrockItemAttachables.useCondition(rule);
    }

    List<ItemBindingCondition> stateConditions() {
        return List.of();
    }

    Key variantId(Key id) {
        return id;
    }

    @Nullable
    GeneratedItemVariant tryWriteSpecialized(BedrockItemPresentation plan, JavaModelResources resources, BedrockItemIconRenderer renderer,
                                     BedrockItemGenerationContext context) throws IOException {
        return null;
    }

    BedrockEquipmentPose pose(ItemRegistrationPlan.Entry entry) throws IOException {
        return BedrockEquipmentPose.withoutBones(useScripts(entry.useBehavior()));
    }

    void registerHandModel(BedrockPlayerAnimations animations, Key id, ItemRegistrationPlan.Entry entry) {
    }

    static JsonArray useScripts(ItemBehaviorDescription.Use use) {
        JsonArray scripts = new JsonArray();
        scripts.add("variable.ce_using_item = context.item_slot == 'main_hand' && query.is_using_item && query.main_hand_item_use_duration > 0;");
        scripts.add("variable.ce_use_ticks = variable.ce_using_item ? math.max(0, query.main_hand_item_max_duration - query.main_hand_item_use_duration) : 0;");
        scripts.add("variable.ce_remaining_ticks = variable.ce_using_item ? math.max(0, " + use.maximumDuration().value() + " - variable.ce_use_ticks) : 0;");
        return scripts;
    }
}
