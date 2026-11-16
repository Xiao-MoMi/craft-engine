package net.momirealms.craftengine.core.pack.bedrock.item.equipment;

import com.google.gson.JsonArray;
import net.momirealms.craftengine.core.pack.bedrock.item.animation.BedrockPlayerAnimations;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingCondition;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ResolvedItemBinding;
import net.momirealms.craftengine.core.pack.model.definition.condition.UsingItemConditionProperty;
import net.momirealms.craftengine.core.util.Key;

final class BedrockShieldRenderStrategy extends BedrockItemRenderStrategy {
    static final BedrockShieldRenderStrategy INSTANCE = new BedrockShieldRenderStrategy();

    private BedrockShieldRenderStrategy() {
    }

    @Override
    boolean isEquipmentCondition(ItemBindingCondition condition) {
        return condition instanceof ItemBindingCondition.BooleanMatch match && match.property() instanceof UsingItemConditionProperty;
    }

    @SuppressWarnings("DeconstructionCanBeUsed")
    @Override
    String stageCondition(ItemBindingRule rule) {
        StringBuilder conditionText = new StringBuilder("1");
        for (ItemBindingCondition condition : rule.conditions()) {
            if (condition instanceof ItemBindingCondition.BooleanMatch match && match.property() instanceof UsingItemConditionProperty) {
                conditionText.append(match.expected() ? " && variable.ce_using_item" : " && !variable.ce_using_item");
            }
        }
        return conditionText.toString();
    }

    @Override
    BedrockEquipmentPose pose(ResolvedItemBinding entry) {
        JsonArray scripts = new JsonArray();
        // Geyser 用 EMERGING 区分是哪只手在格挡。
        scripts.add("variable.ce_using_item = query.blocking && ((context.item_slot == 'main_hand' && !query.is_emerging) || (context.item_slot == 'off_hand' && query.is_emerging));");
        return BedrockEquipmentPose.withoutBones(scripts);
    }

    @Override
    void registerHandModel(BedrockPlayerAnimations animations, Key id, ResolvedItemBinding entry) {
        animations.registerShieldUsingJavaPose(id);
    }
}
