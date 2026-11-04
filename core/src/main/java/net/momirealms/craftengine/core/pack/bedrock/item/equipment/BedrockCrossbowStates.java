package net.momirealms.craftengine.core.pack.bedrock.item.equipment;

import com.google.gson.JsonArray;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBehaviorDescription;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingCondition;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.model.definition.condition.UsingItemConditionProperty;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.CrossBowPullingRangeDispatchProperty;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.UseDurationRangeDispatchProperty;
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

    @SuppressWarnings("DeconstructionCanBeUsed")
    static String condition(ItemBindingRule rule) {
        StringBuilder expression = new StringBuilder("1");
        for (ItemBindingCondition condition : rule.conditions()) {
            if (condition instanceof ItemBindingCondition.BooleanMatch match && match.property() instanceof UsingItemConditionProperty) {
                expression.append(match.expected() ? " && variable.ce_using_item" : " && !variable.ce_using_item");
            } else if (condition instanceof ItemBindingCondition.NumericRange range) {
                String value;
                if (range.property() instanceof CrossBowPullingRangeDispatchProperty) {
                    value = "variable.ce_pull";
                } else if (range.property() instanceof UseDurationRangeDispatchProperty duration) {
                    value = duration.remaining() ? "variable.ce_remaining_ticks" : "variable.ce_use_ticks";
                } else {
                    continue;
                }
                value = "(" + value + " * " + range.scale() + ")";
                if (range.minInclusive() != null) {
                    expression.append(" && ").append(value).append(" >= ").append(range.minInclusive());
                }
                if (range.maxExclusive() != null) {
                    expression.append(" && ").append(value).append(" < ").append(range.maxExclusive());
                }
            }
        }
        return expression.toString();
    }

    static JsonArray scripts(State state, ItemBehaviorDescription.Use use) {
        JsonArray scripts = useScripts(state.charged(), use);
        scripts.add(BedrockItemAttachables.stageSelection(state.rules(), BedrockCrossbowStates::condition));
        return scripts;
    }

    static JsonArray useScripts(boolean charged, ItemBehaviorDescription.Use use) {
        int chargeTicks = use.chargeDuration().value();
        JsonArray scripts = new JsonArray();
        scripts.add("variable.ce_using_item = " + (charged ? "0" : "context.item_slot == 'main_hand' && query.is_using_item && query.main_hand_item_use_duration > 0") + ";");
        scripts.add("variable.ce_use_ticks = variable.ce_using_item ? math.max(0, query.main_hand_item_max_duration - query.main_hand_item_use_duration) : 0;");
        scripts.add("variable.ce_remaining_ticks = variable.ce_using_item ? math.max(0, " + use.maximumDuration().value() + " - variable.ce_use_ticks) : 0;");
        // 当前只支持普通弩；附魔修正后的装填时长需要后续运行时同步。
        scripts.add("variable.ce_pull = variable.ce_using_item ? variable.ce_use_ticks / " + chargeTicks + " : 0;");
        scripts.add("variable.ce_charge_amount = variable.ce_using_item ? math.clamp((variable.ce_use_ticks + query.frame_alpha - 1) / 10, 0, 1) : 0;");
        return scripts;
    }

    record State(Key id, boolean charged, List<ItemBindingRule> rules, int idle, ItemBindingRule registration) {
    }
}
