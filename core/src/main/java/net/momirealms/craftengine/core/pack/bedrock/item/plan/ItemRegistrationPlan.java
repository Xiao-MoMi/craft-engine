package net.momirealms.craftengine.core.pack.bedrock.item.plan;

import java.util.List;

public record ItemRegistrationPlan(List<ResolvedItemBinding> explicitItems, List<ResolvedItemBinding> vanillaOverrides) {
    public ItemRegistrationPlan {
        explicitItems = List.copyOf(explicitItems);
        vanillaOverrides = List.copyOf(vanillaOverrides);
    }

    public ItemRegistrationPlan(List<ResolvedItemBinding> explicitItems) {
        this(explicitItems, List.of());
    }

    public int size() {
        return this.explicitItems.size() + this.vanillaOverrides.size();
    }
}
