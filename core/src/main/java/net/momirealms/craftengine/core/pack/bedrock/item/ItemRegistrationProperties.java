package net.momirealms.craftengine.core.pack.bedrock.item;

import com.google.gson.JsonElement;
import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.util.Key;

import java.util.Map;

public record ItemRegistrationProperties(Map<Key, JsonElement> components) {

    public ItemRegistrationProperties {
        components = Map.copyOf(components);
    }

    public int maxStackSize() {
        return this.componentIntValue(DataComponentKeys.MAX_STACK_SIZE, 1);
    }

    public int maxDamage() {
        return this.componentIntValue(DataComponentKeys.MAX_DAMAGE, 0);
    }

    private int componentIntValue(Key key, int fallback) {
        JsonElement component = this.components.get(key);
        if (component == null || component.isJsonNull()) {
            return fallback;
        }
        return component.getAsInt();
    }
}
