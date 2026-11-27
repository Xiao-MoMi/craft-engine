package net.momirealms.craftengine.bedrock.geyser.item.component;

import com.google.gson.JsonElement;

@FunctionalInterface
public interface GeyserItemComponentReader {

    GeyserItemComponentPatch read(JsonElement value, int sourceVersion, String itemType);
}
