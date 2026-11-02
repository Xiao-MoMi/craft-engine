package net.momirealms.craftengine.bedrock.geyser.item;

import org.cloudburstmc.protocol.bedrock.data.definitions.ItemDefinition;
import org.cloudburstmc.protocol.bedrock.data.inventory.ItemData;
import org.geysermc.geyser.registry.type.ItemMappings;

import java.util.HashMap;
import java.util.Map;

final class GeyserHeadItemViews {
    private final Map<String, ItemDefinition> head = new HashMap<>();
    private final Map<String, ItemDefinition> inventory = new HashMap<>();

    GeyserHeadItemViews(ItemMappings mappings, Map<String, String> identifiers) {
        identifiers.forEach((base, alias) -> {
            ItemDefinition baseItem = mappings.getDefinition(base);
            ItemDefinition headItem = mappings.getDefinition(alias);
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
        if (item == null || item.getDefinition() == null || definitions.isEmpty()) {
            return item;
        }
        ItemDefinition replacement = definitions.get(item.getDefinition().getIdentifier());
        return replacement == null ? item : item.toBuilder().definition(replacement).build();
    }
}
