package net.momirealms.craftengine.core.pack.bedrock.item.plan;

import net.momirealms.craftengine.core.util.Key;

import java.util.ArrayList;
import java.util.List;

public record ItemDefinitionBinding(Key definitionId,
                                    boolean handAnimationOnSwap,
                                    boolean oversizedInGui,
                                    float swapAnimationScale,
                                    List<ItemBindingRule> rules /* 全部变体分支 */) {

    public ItemDefinitionBinding {
        rules = List.copyOf(rules);
    }

    public boolean hasPlayerHead() {
        for (ItemBindingRule rule : this.rules) {
            if (rule.target().hasPlayerHead()) {
                return true;
            }
        }
        return false;
    }

    ItemDefinitionBinding withHeadSkin(Key skin) {
        List<ItemBindingRule> updated = bindHeadSkin(this.rules, skin);
        return updated == this.rules ? this : new ItemDefinitionBinding(this.definitionId, this.handAnimationOnSwap, this.oversizedInGui, this.swapAnimationScale, updated);
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
