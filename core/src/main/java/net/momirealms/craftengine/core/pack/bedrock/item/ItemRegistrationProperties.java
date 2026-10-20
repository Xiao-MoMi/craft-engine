package net.momirealms.craftengine.core.pack.bedrock.item;

import net.momirealms.craftengine.core.item.component.DataComponentKeys;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MinecraftVersion;

import java.util.Map;

public record ItemRegistrationProperties(Map<Key, ItemBehaviorDescription.Component> components) {

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
        ItemBehaviorDescription.Component component = this.components.get(key);
        if (component == null) {
            return fallback;
        }
        return switch (component.source()) {
            case DEFAULT, OVERRIDE -> component.value().getAsInt();
            case ABSENT, REMOVED -> fallback;
        };
    }
}
