package net.momirealms.craftengine.bedrock.geyser.item.component;

import org.cloudburstmc.nbt.NbtMap;
import org.geysermc.geyser.registry.type.ItemMappings;

public interface GeyserItemComponentPatch {

    NbtMap apply(NbtMap data, ItemMappings mappings);
}
