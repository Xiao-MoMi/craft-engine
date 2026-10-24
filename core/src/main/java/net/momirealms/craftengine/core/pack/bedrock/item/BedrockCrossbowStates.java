package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.pack.model.definition.condition.UsingItemConditionProperty;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.CrossBowPullingRangeDispatchProperty;
import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.UseDurationRangeDispatchProperty;
import net.momirealms.craftengine.core.pack.model.definition.select.ChargeTypeSelectProperty;
import net.momirealms.craftengine.core.util.Key;

import java.util.ArrayList;
import java.util.List;

final class BedrockCrossbowStates {
    private static final List<String> CHARGES = List.of("none", "arrow", "rocket");

    private BedrockCrossbowStates() {
    }

    static List<State> collect(ItemRegistrationPlan.Entry entry) {
        List<State> states = new ArrayList<>(3);
        for (String charge : CHARGES) {
            List<ItemBindingRule> rules = new ArrayList<>();
            for (ItemBindingRule rule : entry.definition().rules()) {
                boolean matches = true;
                for (ItemBindingCondition condition : rule.conditions()) {
                    if (condition instanceof ItemBindingCondition.SelectMatch select && select.property() instanceof ChargeTypeSelectProperty
                            && !select.matches(new JsonPrimitive(charge))) {
                        matches = false;
                        break;
                    }
                }
                if (matches) {
                    rules.add(rule);
                }
            }
            int idle = -1;
            for (int i = 0; i < rules.size(); i++) {
                if (matchesIdle(rules.get(i))) {
                    idle = i;
                    break;
                }
            }
            if (idle == -1) {
                return List.of();
            }
            List<ItemBindingCondition> base = rules.get(idle).conditions().stream().filter(condition -> !isState(condition)).toList();
            for (ItemBindingRule rule : rules) {
                if (!(rule.target() instanceof ItemRenderTarget.Model)
                        || !base.equals(rule.conditions().stream().filter(condition -> !isState(condition)).toList())) {
                    return List.of();
                }
            }
            // 装填过程由 attachable 切换，弹药状态保留为注册谓词，使背包和掉落同步变化。
            List<ItemBindingCondition> conditions = new ArrayList<>(base);
            if (charge.equals("none")) {
                conditions.add(new ItemBindingCondition.SelectMatch(ChargeTypeSelectProperty.INSTANCE,
                        List.of(new JsonPrimitive("arrow"), new JsonPrimitive("rocket")), true));
            } else {
                conditions.add(new ItemBindingCondition.SelectMatch(ChargeTypeSelectProperty.INSTANCE, List.of(new JsonPrimitive(charge)), false));
            }
            ItemBindingRule source = rules.get(idle);
            ItemBindingRule registration = new ItemBindingRule(conditions, source.target(), source.transformations(), source.sourcePath());
            Key id = Key.of(entry.bedrockId().namespace(), entry.bedrockId().value() + '_' + charge);
            states.add(new State(id, !charge.equals("none"), rules, idle, registration));
        }
        return states;
    }

    private static boolean isState(ItemBindingCondition condition) {
        return BedrockItemAttachables.isUseCondition(condition)
                || (condition instanceof ItemBindingCondition.NumericRange range && range.property() instanceof CrossBowPullingRangeDispatchProperty)
                || (condition instanceof ItemBindingCondition.SelectMatch select && select.property() instanceof ChargeTypeSelectProperty);
    }

    private static boolean matchesIdle(ItemBindingRule rule) {
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
        int chargeTicks = use.chargeDuration().value();
        JsonArray scripts = new JsonArray();
        scripts.add("variable.ce_using_item = " + (state.charged() ? "0" : "context.item_slot == 'main_hand' && query.is_using_item && query.main_hand_item_use_duration > 0") + ";");
        scripts.add("variable.ce_use_ticks = variable.ce_using_item ? math.max(0, query.main_hand_item_max_duration - query.main_hand_item_use_duration) : 0;");
        scripts.add("variable.ce_remaining_ticks = variable.ce_using_item ? math.max(0, " + use.maximumDuration().value() + " - variable.ce_use_ticks) : 0;");
        // 当前只支持普通弩；附魔修正后的装填时长需要后续运行时同步。
        scripts.add("variable.ce_pull = variable.ce_using_item ? variable.ce_use_ticks / " + chargeTicks + " : 0;");
        scripts.add("variable.ce_charge_amount = variable.ce_using_item ? math.clamp((variable.ce_use_ticks + query.frame_alpha - 1) / 10, 0, 1) : 0;");
        String selection = "-1";
        for (int i = state.rules().size() - 1; i >= 0; i--) {
            selection = "(" + condition(state.rules().get(i)) + ") ? " + i + " : (" + selection + ")";
        }
        scripts.add("variable.ce_stage = " + selection + ";");
        return scripts;
    }

    record State(Key id, boolean charged, List<ItemBindingRule> rules, int idle, ItemBindingRule registration) {
    }
}
