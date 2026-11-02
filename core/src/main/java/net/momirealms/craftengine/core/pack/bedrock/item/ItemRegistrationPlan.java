package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.util.Key;

import java.util.List;

public record ItemRegistrationPlan(List<Entry> entries, boolean hasErrors) {

    public ItemRegistrationPlan {
        entries = List.copyOf(entries);
    }

    public record Entry(Key bedrockId,  // craftengine:原始命名空间/物品路径
                        Key javaItemType,
                        Key definitionId,
                        ItemDefinitionBinding definition,
                        ItemRegistrationProperties properties,
                        ResolvedItemBinding source) {
    }
}
