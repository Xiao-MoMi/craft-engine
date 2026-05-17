package net.momirealms.craftengine.core.plugin.config;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.pack.CachedConfigSection;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.PendingConfigValue;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.template.TemplateManager;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.nio.file.Path;
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
                Key id = Key.withDefaultNamespace(key, cachedMajorSection.pack().namespace());
                Path filePath = cachedMajorSection.path();
                String currentNode = config.assemblePath(key);
                if (this.checkDuplicated() && this.isDuplicate(id, filePath, currentNode)) {
                    continue;
                }
                try {
                    ConfigValue configValue = new ConfigValue(currentNode, createConfigValue(id, config.getValue(key)));
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

    protected Object createConfigValue(final Key id, final ConfigValue value) {
        return TemplateManager.INSTANCE.applyTemplates(id, value);
    }

    @Override
    public void clearConfigs() {
        super.clearConfigs();
        this.pendingConfigValues.clear();
    }

    protected abstract void parseValue(Pack pack, Path filePath, Key id, ConfigValue value);
}
