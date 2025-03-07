package net.momirealms.craftengine.core.loot.number;

import java.util.Map;

public interface NumberProviderFactory {

    NumberProvider create(Map<String, Object> arguments);
}
