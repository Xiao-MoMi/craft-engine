package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.util.Key;

import java.util.List;

public record ItemDefinitionBinding(Key definitionId,
                                    boolean handAnimationOnSwap,
                                    boolean oversizedInGui,
                                    float swapAnimationScale,
                                    List<ItemBindingRule> rules) {

    public ItemDefinitionBinding {
        rules = List.copyOf(rules);
    }
}