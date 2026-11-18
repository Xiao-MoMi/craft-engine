package net.momirealms.craftengine.core.pack.bedrock.item.plan;

import net.momirealms.craftengine.core.pack.model.definition.rangedisptach.CustomModelDataRangeDispatchProperty;
import net.momirealms.craftengine.core.util.Key;

import java.util.ArrayList;
import java.util.List;

public record ItemModelDefinitionBinding(Key definitionId,
                                         boolean handAnimationOnSwap,
                                         boolean oversizedInGui,
                                         float swapAnimationScale,
                                         List<ItemBindingRule> rules,
                                         List<ItemBindingCondition> identityConditions) {
    private static final CustomModelDataRangeDispatchProperty MODEL_DATA_ID = new CustomModelDataRangeDispatchProperty(0);

    public ItemModelDefinitionBinding(Key definitionId, boolean handAnimationOnSwap, boolean oversizedInGui, float swapAnimationScale, List<ItemBindingRule> rules) {
        this(definitionId, handAnimationOnSwap, oversizedInGui, swapAnimationScale, rules, List.of());
    }

    public ItemModelDefinitionBinding {
        rules = List.copyOf(rules);
        identityConditions = List.copyOf(identityConditions);
    }

    public boolean hasPlayerHead() {
        for (ItemBindingRule rule : this.rules) {
            if (rule.target().hasPlayerHead()) {
                return true;
            }
        }
        return false;
    }

    ItemModelDefinitionBinding withHeadSkin(Key skin) {
        List<ItemBindingRule> updated = bindHeadSkin(this.rules, skin);
        return updated == this.rules ? this : new ItemModelDefinitionBinding(this.definitionId, this.handAnimationOnSwap, this.oversizedInGui, this.swapAnimationScale, updated, this.identityConditions);
    }

    ItemModelDefinitionBinding withModelDataId(int id) {
        List<ItemBindingRule> reachable = new ArrayList<>();
        for (ItemBindingRule rule : this.rules) {
            if (rule.hasCondition(condition -> condition instanceof ItemBindingCondition.NumericRange range
                    && MODEL_DATA_ID.equals(range.property()) && !range.matches(id))) {
                continue;
            }
            List<ItemBindingCondition> remaining = rule.conditions().stream()
                    .filter(condition -> !(condition instanceof ItemBindingCondition.NumericRange range && MODEL_DATA_ID.equals(range.property())))
                    .toList();
            reachable.add(new ItemBindingRule(remaining, rule.target(), rule.transformations(), rule.sourcePath()));
        }
        // Geyser 的区间是左闭右开，相邻 float 之间没有其他可匹配的模型值。
        ItemBindingCondition exact = new ItemBindingCondition.NumericRange(MODEL_DATA_ID, 1F, (float) id, Math.nextUp((float) id));
        return new ItemModelDefinitionBinding(this.definitionId, this.handAnimationOnSwap, this.oversizedInGui, this.swapAnimationScale, reachable, List.of(exact));
    }

    @SuppressWarnings("DeconstructionCanBeUsed")
    private static List<ItemBindingRule> bindHeadSkin(List<ItemBindingRule> rules, Key skin) {
        List<ItemBindingRule> result = rules;
        for (int index = 0; index < rules.size(); index++) {
            ItemBindingRule rule = rules.get(index);
            ItemRenderTarget target = rule.target();
            if (target instanceof ItemRenderTarget.Model model && model.headSkin() != null && !skin.equals(model.headSkin())) {
                target = new ItemRenderTarget.Model(model.model(), model.tints(), skin);
            } else if (target instanceof ItemRenderTarget.Composite composite) {
                List<ItemBindingRule> parts = bindHeadSkin(composite.parts(), skin);
                if (parts != composite.parts()) {
                    target = new ItemRenderTarget.Composite(parts);
                }
            }
            if (target != rule.target()) {
                if (result == rules) {
                    result = new ArrayList<>(rules);
                }
                result.set(index, new ItemBindingRule(rule.conditions(), target, rule.transformations(), rule.sourcePath()));
            }
        }
        return result;
    }
}
