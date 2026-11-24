package net.momirealms.craftengine.bedrock.geyser.item.component;

import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.registry.type.ItemMappings;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

public final class GeyserItemComponentPatchCache {
    private final Map<String, GeyserItemComponentPatch> patches;
    private final Map<String, GeyserItemText> itemText;
    private final boolean hasRecipeLore;
    private final Map<ItemMappings, Map<String, UnaryOperator<NbtMap>>> byMappings = new IdentityHashMap<>();

    public GeyserItemComponentPatchCache(Map<String, GeyserItemComponentPatch> patches, Map<String, GeyserItemText> itemText) {
        this.patches = Map.copyOf(patches);
        this.itemText = Map.copyOf(itemText);
        this.hasRecipeLore = this.itemText.values().stream().anyMatch(GeyserItemText::hasLore);
    }

    Map<String, GeyserItemText> itemText() {
        return this.itemText;
    }

    boolean hasRecipeLore() {
        return this.hasRecipeLore;
    }

    synchronized Map<String, UnaryOperator<NbtMap>> forMappings(ItemMappings mappings) {
        return this.byMappings.computeIfAbsent(mappings, this::bind);
    }

    private Map<String, UnaryOperator<NbtMap>> bind(ItemMappings mappings) {
        Map<String, UnaryOperator<NbtMap>> bound = HashMap.newHashMap(this.patches.size());
        this.patches.forEach((identifier, patch) -> {
            Map<NbtMap, NbtMap> results = new ConcurrentHashMap<>();
            UnaryOperator<NbtMap> apply = original -> patch.apply(original, mappings);
            bound.put(identifier, original -> results.computeIfAbsent(original, apply));
        });
        return Map.copyOf(bound);
    }
}
