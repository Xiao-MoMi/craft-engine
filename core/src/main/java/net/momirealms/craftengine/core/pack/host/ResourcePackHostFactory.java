package net.momirealms.craftengine.core.pack.host;

import net.momirealms.craftengine.core.plugin.config.ConfigSection;
import net.momirealms.craftengine.core.plugin.config.KnownResourceException;

public interface ResourcePackHostFactory<T extends ResourcePackHost> {

    T create(ConfigSection section);

    default String getNonNullEnvironmentVariable(ConfigSection section, String name) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            throw new KnownResourceException("host.missing_environment_variable", section.path(), name);
        }
        return value;
    }
}
