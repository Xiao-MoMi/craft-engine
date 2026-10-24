package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public record ItemRegistrationProperties(Map<Key, JsonElement> components) {

    public ItemRegistrationProperties {
        components = Map.copyOf(components);
    }
}
