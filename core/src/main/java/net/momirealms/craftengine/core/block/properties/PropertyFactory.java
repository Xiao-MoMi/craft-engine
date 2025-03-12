package net.momirealms.craftengine.core.block.properties;

import java.util.Map;

public interface PropertyFactory {

    Property<?> create(String name, Map<String, Object> arguments);
}
