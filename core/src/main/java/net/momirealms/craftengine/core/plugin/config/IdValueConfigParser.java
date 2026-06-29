package net.momirealms.craftengine.core.plugin.config;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.pack.CachedConfigSection;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.PendingConfigValue;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.template.ArgumentString;
import net.momirealms.craftengine.core.plugin.config.template.TemplateManager;
import net.momirealms.craftengine.core.plugin.config.template.argument.PlainStringTemplateArgument;
import net.momirealms.craftengine.core.plugin.config.template.argument.TemplateArgument;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

public abstract class IdValueConfigParser extends IdConfigParser {
    protected final ObjectArrayList<PendingConfigValue> pendingConfigValues = new ObjectArrayList<>(32);

    public synchronized void addPendingConfigValue(PendingConfigValue value) {
        this.pendingConfigValues.add(value);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void loadAll() {
        Object[] elements = this.configStorage.elements();
        for (int i = 0, size = this.configStorage.size(); i < size; i++) {
            CachedConfigSection cachedMajorSection = (CachedConfigSection) elements[i];
            ConfigSection config = cachedMajorSection.config();
            for (String key : config.keySet()) {
                ConfigValue value = config.getValue(key);
                if (value == null) continue; // fixme 评估这种做法是否合适，但是直接把 null 传递下去必然会创建一个 ConfigValue 中的 value 是 null 的对象
                if (cachedMajorSection.hasArguments() && key.contains("$")) {
                    key = ArgumentString.preParse(config.path(), key).get(config.path(), cachedMajorSection.arguments).toString();
                }
                Key id = Key.withDefaultNamespace(key, cachedMajorSection.pack().namespace());
                Path filePath = cachedMajorSection.path();
                String currentNode = config.assemblePath(key);
                if (this.checkDuplicated() && this.isDuplicate(id, filePath, currentNode)) {
                    continue;
                }
                try {
                    ConfigValue configValue = new ConfigValue(currentNode, createConfigValue(id, value, cachedMajorSection.arguments));
                    this.pendingConfigValues.add(new PendingConfigValue(cachedMajorSection.pack(), filePath, id, configValue));
                } catch (KnownResourceException e) {
                    error(e, filePath);
                }
            }
        }
        int size = this.pendingConfigValues.size();
        Object[] pendingElements = this.pendingConfigValues.elements();
        CountDownLatch latch = new CountDownLatch(size);
        Executor executor = async() ? CraftEngine.instance().scheduler().async() : Runnable::run;
        for (int i = 0; i < size; i++) {
            PendingConfigValue pending = (PendingConfigValue) pendingElements[i];
            ResourceConfigUtils.runCatching(
                    pending.path(),
                    pending.value().path(),
                    executor,
                    () -> {
                        try {
                            parseValue(pending.pack(), pending.path(), pending.id(), pending.value());
                        } finally {
                            latch.countDown();
                        }
                    },
                    super.errorHandler
            );
        }
        try {
            latch.await(); // 等待所有任务完成
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected Object createConfigValue(final Key id, final ConfigValue value, Map<String, TemplateArgument> argumentMap) {
        if (argumentMap == null) {
            return TemplateManager.INSTANCE.applyTemplates(value, Map.of(
                    "__NAMESPACE__", PlainStringTemplateArgument.plain(id.namespace()),
                    "__ID__", PlainStringTemplateArgument.plain(id.value())
            ));
        } else {
            return TemplateManager.INSTANCE.applyTemplates(value, MiscUtils.init(argumentMap, map -> {
                map.put("__NAMESPACE__", PlainStringTemplateArgument.plain(id.namespace()));
                map.put("__ID__", PlainStringTemplateArgument.plain(id.value()));
            }));
        }
    }

    @Override
    public void clearConfigs() {
        super.clearConfigs();
        this.pendingConfigValues.clear();
    }

    protected abstract void parseValue(Pack pack, Path filePath, Key id, ConfigValue value);
}
