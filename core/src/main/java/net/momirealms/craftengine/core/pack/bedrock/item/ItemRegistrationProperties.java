package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public record ItemRegistrationProperties(Map<Key, JsonElement> components, @Nullable JsonObject kineticWeapon) {

    public ItemRegistrationProperties(Map<Key, JsonElement> components) {
        this(components, null);
    }

    public ItemRegistrationProperties {
        components = Map.copyOf(components);
    }
}
