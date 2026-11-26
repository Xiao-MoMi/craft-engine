package net.momirealms.craftengine.core.pack.bedrock.item.render;

import com.google.gson.JsonArray;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBehaviorDescription;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingCondition;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.CrossBowPullingRangeDispatchProperty;
import net.momirealms.craftengine.core.util.Key;

import java.util.List;

final class BedrockCrossbowStates {
    private BedrockCrossbowStates() {
    }

    static boolean matchesIdle(ItemBindingRule rule) {
        if (!BedrockItemAttachables.matchesIdle(rule)) {
            return false;
        }
        for (ItemBindingCondition condition : rule.conditions()) {
            if (condition instanceof ItemBindingCondition.NumericRange range && range.property() instanceof CrossBowPullingRangeDispatchProperty && !range.matches(0)) {
                return false;
            }
        }
        return true;
    }

    static String condition(ItemBindingRule rule) {
        return BedrockItemAttachables.useCondition(rule, true);
    }

    static JsonArray scripts(State state, ItemBehaviorDescription.Use use) {
        JsonArray scripts = useScripts(state.charged(), use);
        scripts.add(BedrockItemAttachables.stageSelection(state.rules(), BedrockCrossbowStates::condition));
        return scripts;
    }

    static JsonArray useScripts(boolean charged, ItemBehaviorDescription.Use use) {
        int chargeTicks = use.chargeDuration().value();
        JsonArray scripts = BedrockItemUseScripts.create(use.maximumDuration().value(), !charged);
        // 当前只支持普通弩；附魔修正后的装填时长需要后续运行时同步。
        scripts.add("variable.ce_pull = variable.ce_using_item ? variable.ce_use_ticks / " + chargeTicks + " : 0;");
        scripts.add(BedrockItemRenderStrategy.CHARGE_AMOUNT_SCRIPT);
        return scripts;
    }

    record State(Key id, boolean charged, List<ItemBindingRule> rules, int idle, List<ItemBindingCondition> conditions) {
    }
}
