package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.pack.model.definition.special.HeadSpecialModel;
import net.momirealms.craftengine.core.pack.model.definition.special.PlayerHeadSpecialModel;
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

    ItemDefinitionBinding withHeadSkin(Key skin) {
        List<ItemBindingRule> updated = bindHeadSkin(this.rules, skin);
        return updated == this.rules ? this : new ItemDefinitionBinding(this.definitionId, this.handAnimationOnSwap,
                this.oversizedInGui, this.swapAnimationScale, updated);
    }

    private static List<ItemBindingRule> bindHeadSkin(List<ItemBindingRule> rules, Key skin) {
        List<ItemBindingRule> result = rules;
        for (int index = 0; index < rules.size(); index++) {
            ItemBindingRule rule = rules.get(index);
            ItemRenderTarget target = rule.target();
            if (target instanceof ItemRenderTarget.Special special && (special.model() instanceof PlayerHeadSpecialModel
                    || special.model() instanceof HeadSpecialModel head && head.kind().equals("player") && head.texture() == null)) {
                target = new ItemRenderTarget.Model(special.base(), List.of(), skin);
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
