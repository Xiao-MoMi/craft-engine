package net.momirealms.craftengine.core.pack.bedrock.item.plan;

import net.momirealms.craftengine.core.util.Key;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public record ItemRegistrationPlan(List<ResolvedItemBinding> explicitItems, List<ResolvedItemBinding> vanillaOverrides, Map<Key, Set<Key>> entityModels) {
    public ItemRegistrationPlan {
        Map<Key, Set<Key>> models = new LinkedHashMap<>();
        entityModels.forEach((item, owners) -> {
            Set<Key> sorted = new TreeSet<>(Comparator.comparing(Key::asString));
            sorted.addAll(owners);
            models.put(item, Collections.unmodifiableSet(sorted));
        });
        entityModels = Map.copyOf(models);
        explicitItems = List.copyOf(explicitItems);
        vanillaOverrides = List.copyOf(vanillaOverrides);
    }

    public ItemRegistrationPlan(List<ResolvedItemBinding> explicitItems) {
        this(explicitItems, List.of(), Map.of());
    }

    public int size() {
        return this.explicitItems.size() + this.vanillaOverrides.size();
    }
}
