package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.util.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public record ItemRegistrationProperties(Map<Key, JsonElement> components, @Nullable Key headTexture) {

    public ItemRegistrationProperties(Map<Key, JsonElement> components) {
        this(components, null);
    }

    public ItemRegistrationProperties {
        components = Map.copyOf(components);
    }
}
