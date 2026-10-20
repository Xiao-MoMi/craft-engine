package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.pack.model.definition.Transformation;

import java.util.List;

public record ItemBindingRule(List<ItemBindingCondition> conditions,
                              ItemRenderTarget target,
                              List<Transformation> transformations,
                              String sourcePath) {

    public ItemBindingRule {
        // 必须复制，因为这个list会变
        conditions = List.copyOf(conditions);
        transformations = List.copyOf(transformations);
    }
}
