package net.momirealms.craftengine.bedrock.geyser.item;

import net.momirealms.craftengine.bedrock.geyser.proxy.registry.type.ItemMappingsProxy;
import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.registry.type.ItemMappings;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class GeyserHeadItemViews {
    private final Map<String, ItemDefinition> head = new HashMap<>();
    private final Map<String, ItemDefinition> inventory = new HashMap<>();

    GeyserHeadItemViews(ItemMappings mappings, Map<String, String> identifiers) {
        if (identifiers.isEmpty()) {
            return;
        }
        Set<String> needed = new HashSet<>(identifiers.keySet());
        needed.addAll(identifiers.values());
        Map<String, ItemDefinition> definitions = HashMap.newHashMap(needed.size());
        Map<?, ?> byId = (Map<?, ?>) ItemMappingsProxy.INSTANCE.itemDefinitions(mappings);
        for (Object entry : byId.values()) {
            ItemDefinition definition = (ItemDefinition) entry;
            String id = definition.getIdentifier();
            if (needed.contains(id)) {
                definitions.put(id, definition);
                if (definitions.size() == needed.size()) {
                    break;
                }
            }
        }
        identifiers.forEach((base, alias) -> {
            ItemDefinition baseItem = definitions.get(base);
            ItemDefinition headItem = definitions.get(alias);
            if (baseItem != null && headItem != null) {
                this.head.put(base, headItem);
                this.inventory.put(alias, baseItem);
            }
        });
    }

    ItemData equipped(ItemData item) {
        return replace(item, this.head);
    }

    ItemData unequipped(ItemData item) {
        return replace(item, this.inventory);
    }

    private static ItemData replace(ItemData item, Map<String, ItemDefinition> definitions) {
        if (item == null || definitions.isEmpty()) {
            return item;
        }
        ItemDefinition replacement = definitions.get(item.getDefinition().getIdentifier());
        return replacement == null ? item : item.toBuilder().definition(replacement).build();
    }
}
