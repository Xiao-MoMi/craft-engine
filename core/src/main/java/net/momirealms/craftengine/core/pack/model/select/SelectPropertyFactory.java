package net.momirealms.craftengine.core.pack.model.select;

import java.util.Map;

public interface SelectPropertyFactory {

    SelectProperty create(Map<String, Object> arguments);
}
