package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import net.momirealms.craftengine.core.pack.model.definition.select.DisplayContextSelectProperty;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class BedrockSpearModels {
    private final ItemBindingRule inventory;
    private final List<ItemBindingRule> hands;
    private final List<String> conditions;

    private BedrockSpearModels(ItemBindingRule inventory, List<ItemBindingRule> hands, List<String> conditions) {
        this.inventory = inventory;
        this.hands = hands;
        this.conditions = conditions;
    }

    @Nullable
    static BedrockSpearModels collect(ItemRegistrationPlan.Entry entry) {
        ItemBindingRule inventory = null;
        List<ItemBindingRule> hands = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        for (ItemBindingRule rule : entry.definition().rules()) {
            if (!(rule.target() instanceof ItemRenderTarget.Model)) {
                return null;
            }
            if (matches(rule, "gui") && BedrockItemAttachables.matchesIdle(rule) && inventory == null) {
                inventory = rule;
            }
            StringBuilder contexts = new StringBuilder();
            for (String name : List.of("firstperson_righthand", "firstperson_lefthand", "thirdperson_righthand", "thirdperson_lefthand")) {
                if (matches(rule, name)) {
                    if (!contexts.isEmpty()) {
                        contexts.append(" || ");
                    }
                    contexts.append('(').append(name.startsWith("firstperson") ? "context.is_first_person" : "!context.is_first_person")
                            .append(" && context.item_slot == '").append(name.endsWith("righthand") ? "main_hand" : "off_hand").append("')");
                }
            }
            if (!contexts.isEmpty()) {
                hands.add(rule);
                conditions.add("(" + contexts + ") && (" + BedrockCrossbowStates.condition(rule) + ")");
            }
        }
        if (inventory == null || hands.isEmpty()) {
            return null;
        }
        List<ItemBindingCondition> base = base(inventory);
        for (ItemBindingRule rule : hands) {
            if (!base.equals(base(rule))) {
                return null;
            }
        }
        return new BedrockSpearModels(inventory, hands, conditions);
    }

    private static List<ItemBindingCondition> base(ItemBindingRule rule) {
        return rule.conditions().stream().filter(condition -> !(condition instanceof ItemBindingCondition.SelectMatch select
                && select.property() instanceof DisplayContextSelectProperty) && !BedrockItemAttachables.isUseCondition(condition)).toList();
    }

    private static boolean matches(ItemBindingRule rule, String context) {
        for (ItemBindingCondition condition : rule.conditions()) {
            if (condition instanceof ItemBindingCondition.SelectMatch select && select.property() instanceof DisplayContextSelectProperty
                    && !select.matches(new JsonPrimitive(context))) {
                return false;
            }
        }
        return true;
    }

    ItemBindingRule inventory() {
        return this.inventory;
    }

    List<ItemBindingRule> hands() {
        return this.hands;
    }

    String condition(int stage) {
        return this.conditions.get(stage);
    }

    ItemBindingRule registration() {
        return new ItemBindingRule(base(this.inventory), this.inventory.target(), this.inventory.transformations(), this.inventory.sourcePath());
    }

    JsonArray scripts(ItemBehaviorDescription behavior) {
        JsonArray scripts = new JsonArray();
        scripts.add("variable.ce_using_item = context.item_slot == 'main_hand' && query.is_using_item && query.main_hand_item_use_duration > 0;");
        scripts.add("variable.ce_use_ticks = variable.ce_using_item ? math.max(0, query.main_hand_item_max_duration - query.main_hand_item_use_duration) : 0;");
        scripts.add("variable.ce_remaining_ticks = variable.ce_using_item ? math.max(0, " + behavior.use().maximumDuration().value() + " - variable.ce_use_ticks) : 0;");
        String selection = "-1";
        for (int i = this.hands.size() - 1; i >= 0; i--) {
            selection = "(" + this.conditions.get(i) + ") ? " + i + " : (" + selection + ")";
        }
        scripts.add("variable.ce_stage = " + selection + ";");
        return scripts;
    }
}
