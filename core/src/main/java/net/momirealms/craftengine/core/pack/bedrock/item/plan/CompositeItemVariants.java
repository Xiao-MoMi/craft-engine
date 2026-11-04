package net.momirealms.craftengine.core.pack.bedrock.item.plan;

import java.util.ArrayList;
import java.util.List;

final class CompositeItemVariants {
    private final List<List<ItemBindingRule>> layers;
    private final String path;
    private final List<ItemBindingRule> variants;
    private final List<ItemBindingRule> parts = new ArrayList<>();

    CompositeItemVariants(List<List<ItemBindingRule>> layers, String path, List<ItemBindingRule> variants) {
        this.layers = layers;
        this.path = path;
        this.variants = variants;
    }

    void expand(List<ItemBindingCondition> outerConditions) {
        List<ItemBindingCondition> conditions = ItemConditionIntersection.intersect(List.of(), outerConditions);
        if (conditions != null) {
            this.expand(0, conditions);
        }
    }

    @SuppressWarnings("DeconstructionCanBeUsed")
    private void expand(int index, List<ItemBindingCondition> conditions) {
        if (index == this.layers.size()) {
            this.variants.add(new ItemBindingRule(conditions, new ItemRenderTarget.Composite(this.parts), List.of(), this.path));
            return;
        }
        // 每层选择一个分支；同时显示的模型按原始顺序保留，条件冲突的组合不再展开。
        for (ItemBindingRule choice : this.layers.get(index)) {
            List<ItemBindingCondition> combined = ItemConditionIntersection.intersect(conditions, choice.conditions());
            if (combined == null) {
                continue;
            }
            int size = this.parts.size();
            if (choice.target() instanceof ItemRenderTarget.Composite composite) {
                this.parts.addAll(composite.parts());
            } else if (choice.target() != ItemRenderTarget.Builtin.EMPTY) {
                this.parts.add(choice.conditions().isEmpty() ? choice : new ItemBindingRule(List.of(), choice.target(), choice.transformations(), choice.sourcePath()));
            }
            this.expand(index + 1, combined);
            while (this.parts.size() > size) {
                this.parts.removeLast();
            }
        }
    }
}
