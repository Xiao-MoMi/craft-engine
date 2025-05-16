package net.momirealms.craftengine.core.plugin.context.number;

import java.util.Map;

public interface NumberProviderFactory {

    NumberProvider create(Map<String, Object> args);
}
