package net.momirealms.craftengine.core.pack.bedrock.item.equipment;

import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemBindingRule;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemDefinitionBinding;
import net.momirealms.craftengine.core.pack.bedrock.item.plan.ItemRenderTarget;
import net.momirealms.craftengine.core.pack.bedrock.item.visual.BedrockItemIconRenderer;

import java.io.IOException;
import java.util.List;

enum ItemRenderType {
    ITEM, BLOCK, UNKNOWN;

    static ItemRenderType classify(ItemDefinitionBinding definition, BedrockItemIconRenderer renderer) throws IOException {
        return classify(definition.rules(), renderer);
    }

    private static ItemRenderType classify(List<ItemBindingRule> rules, BedrockItemIconRenderer renderer) throws IOException {
        boolean block = false;
        boolean unknown = false;
        // 任意分支含模型变换或无法渲染成图标，整个定义都使用展示方块。
        for (ItemBindingRule rule : rules) {
            block |= !rule.transformations().isEmpty();
            switch (rule.target()) {
                case ItemRenderTarget.Model model -> block |= !renderer.canRender(model);
                case ItemRenderTarget.Special ignored -> block = true;
                case ItemRenderTarget.Composite composite -> {
                    ItemRenderType type = classify(composite.parts(), renderer);
                    block |= type == BLOCK;
                    unknown |= type == UNKNOWN;
                }
                case ItemRenderTarget.Builtin builtin -> unknown |= builtin != ItemRenderTarget.Builtin.EMPTY;
            }
        }
        return block ? BLOCK : (unknown ? UNKNOWN : ITEM);
    }
}
