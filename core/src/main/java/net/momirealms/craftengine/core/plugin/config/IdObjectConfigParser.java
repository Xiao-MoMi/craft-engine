package net.momirealms.craftengine.core.plugin.config;

import net.momirealms.craftengine.core.pack.CachedConfigSection;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.config.template.TemplateManager;
import net.momirealms.craftengine.core.plugin.locale.LocalizedException;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.nio.file.Path;
import java.util.Map;

public abstract class IdObjectConfigParser extends AbstractConfigParser {

    @Override
    protected void parseSection(CachedConfigSection cached) {
        for (Map.Entry<String, Object> configEntry : cached.config().entrySet()) {
            String key = configEntry.getKey();
            Key id = Key.withDefaultNamespace(key, cached.pack().namespace());
            String node = cached.prefix() + "." + key;
            ResourceConfigUtils.runCatching(
                    cached.filePath(),
                    node,
                    () -> parseObject(cached.pack(), cached.filePath(), node, id, TemplateManager.INSTANCE.applyTemplates(id, configEntry.getValue())),
                    () -> GsonHelper.get().toJson(configEntry.getValue())
            );
        }
    }

    protected abstract void parseObject(Pack pack, Path path, String node, Key id, Object object) throws LocalizedException;
}
