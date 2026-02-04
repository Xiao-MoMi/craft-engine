package net.momirealms.craftengine.core.plugin.config;

import net.momirealms.craftengine.core.pack.CachedConfigSection;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.template.TemplateManager;
import net.momirealms.craftengine.core.plugin.locale.LocalizedException;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.nio.file.Path;
import java.util.Map;

import static net.momirealms.craftengine.core.util.MiscUtils.castToMap;

public abstract class IdSectionConfigParser extends AbstractConfigParser {

    @Override
    protected void parseSection(CachedConfigSection cached) {
        for (Map.Entry<String, Object> configEntry : cached.config().entrySet()) {
            String key = configEntry.getKey();
            Object value = configEntry.getValue();
            Key id = Key.withDefaultNamespace(key, cached.pack().namespace());
            if (!(value instanceof Map<?, ?> section)) {
                TranslationManager.instance().log("warning.config.structure.not_section",
                        cached.filePath().toString(), cached.prefix() + "." + key, value == null ? "null" : value.getClass().getSimpleName());
                continue;
            }
            Map<String, Object> config = castToMap(section, false);
            String node = cached.prefix() + "." + key;
            if ((boolean) config.getOrDefault("debug", false)) {
                if (!ResourceConfigUtils.runCatching(
                        cached.filePath(),
                        node,
                        () -> CraftEngine.instance().logger().info(GsonHelper.get().toJson(TemplateManager.INSTANCE.applyTemplates(id, config))),
                        () -> GsonHelper.get().toJson(section)
                )) {
                    // 发生异常
                    continue;
                }
            }
            if (!(boolean) config.getOrDefault("enable", true)) {
                continue;
            }
            ResourceConfigUtils.runCatching(
                    cached.filePath(),
                    node,
                    () -> parseSection(cached.pack(), cached.filePath(), node, id, MiscUtils.castToMap(TemplateManager.INSTANCE.applyTemplates(id, config), false)),
                    () -> GsonHelper.get().toJson(section)
            );
        }
    }

    protected abstract void parseSection(Pack pack, Path path, String node, Key id, Map<String, Object> section) throws LocalizedException;
}
