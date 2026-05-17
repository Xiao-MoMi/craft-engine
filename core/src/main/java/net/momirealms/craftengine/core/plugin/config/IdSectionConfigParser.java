package net.momirealms.craftengine.core.plugin.config;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.momirealms.craftengine.core.pack.CachedConfigSection;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.pack.PendingConfigSection;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.config.template.TemplateManager;
import net.momirealms.craftengine.core.util.GsonHelper;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

public abstract class IdSectionConfigParser extends IdConfigParser {
    protected final ObjectArrayList<PendingConfigSection> pendingConfigSections = new ObjectArrayList<>(32);

    public synchronized void addPendingConfigSection(PendingConfigSection section) {
        this.pendingConfigSections.add(section);
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public void loadAll() {
        Object[] elements = this.configStorage.elements();
        for (int i = 0, size = this.configStorage.size(); i < size; i++) {
            CachedConfigSection cachedMajorSection = (CachedConfigSection) elements[i];
            ConfigSection config = cachedMajorSection.config;
            for (String key : config.keySet()) {
                Key id = Key.withDefaultNamespace(key, cachedMajorSection.pack.namespace());
                Path filePath = cachedMajorSection.path();
                String currentNode = config.assemblePath(key);
                if (this.checkDuplicated() && this.isDuplicate(id, filePath, currentNode)) {
                    continue;
                }
                Object value;
                try {
                    value = TemplateManager.INSTANCE.applyTemplates(id, config.getValue(key));
                } catch (KnownResourceException e) {
                    error(e, filePath);
                    continue;
                }
                if (!(value instanceof Map<?, ?> section)) {
                    error(new KnownResourceException(filePath, ConfigConstants.PARSE_SECTION_FAILED, currentNode, value != null ? value.getClass().getSimpleName() : "null"));
                    continue;
                }
                try {
                    ConfigSection innerSection = ConfigSection.of(currentNode, MiscUtils.castToMap(section));
                    if (!innerSection.getBoolean("enable", true)) {
                        return;
                    }
                    if (innerSection.getBoolean("debug")) {
                        CraftEngine.instance().logger().info(GsonHelper.get().toJson(section));
                    }
                    this.pendingConfigSections.add(new PendingConfigSection(cachedMajorSection.pack, filePath, id, innerSection));
                } catch (KnownResourceException e) {
                    error(e, filePath);
                }
            }
        }
        int size = this.pendingConfigSections.size();
        Object[] pendingElements = this.pendingConfigSections.elements();
        CountDownLatch latch = new CountDownLatch(size);
        Executor executor = async() ? CraftEngine.instance().scheduler().async() : Runnable::run;
        for (int i = 0; i < size; i++) {
            PendingConfigSection pending = (PendingConfigSection) pendingElements[i];
            ResourceConfigUtils.runCatching(
                    pending.path,
                    pending.section.path(),
                    executor,
                    () -> {
                        try {
                            parseSection(pending.pack, pending.path, pending.id, pending.section);
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

    @Override
    public void clearConfigs() {
        super.clearConfigs();
        this.pendingConfigSections.clear();
    }

    protected abstract void parseSection(@NotNull Pack pack, @NotNull Path path, @NotNull Key id, @NotNull ConfigSection section);
}
