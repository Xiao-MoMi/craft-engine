package net.momirealms.craftengine.core.item.customdata;

import net.momirealms.craftengine.core.block.BlockDefinition;
import net.momirealms.craftengine.core.block.property.Property;
import net.momirealms.craftengine.core.util.Key;

import java.util.HashMap;
import java.util.Map;

public final class BlockDebugStickData {
    public final Map<Key, String> properties;

    public BlockDebugStickData() {
        this.properties = new HashMap<>();
    }

    public BlockDebugStickData(Map<Key, String> properties) {
        this.properties = properties;
    }

    public Property<?> getProperty(final BlockDefinition definition) {
        String propertyName = this.properties.get(definition.id());
        Property<?> property = propertyName == null ? null : definition.getProperty(propertyName);
        if (property == null) {
            property = definition.properties().iterator().next();
        }
        return property;
    }

    public void setProperty(final BlockDefinition definition, final Property<?> property) {
        this.properties.put(definition.id(), property.name());
    }
}
