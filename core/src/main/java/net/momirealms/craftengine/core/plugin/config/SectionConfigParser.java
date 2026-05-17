package net.momirealms.craftengine.core.plugin.config;

import net.momirealms.craftengine.core.pack.CachedConfigSection;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ResourceConfigUtils;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

public abstract class SectionConfigParser extends AbstractConfigParser {

    @Override
    public void loadAll() {
        Object[] elements = this.configStorage.elements();
        if (async()) {
            int size = this.configStorage.size();
            Executor virtual = CraftEngine.instance().scheduler().async();
            CountDownLatch latch = new CountDownLatch(size);
            for (int i = 0; i < size; i++) {
                int index = i;
                virtual.execute(() -> {
                    try {
                        parseSection((CachedConfigSection) elements[index]);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            try {
                latch.await(); // 等待所有任务完成
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        } else {
            for (int i = 0, size = this.configStorage.size(); i < size; i++) {
                parseSection((CachedConfigSection) elements[i]);
            }
        }
    }

    protected void parseSection(CachedConfigSection cached) {
        Path path = cached.path();
        ConfigSection config = cached.config();
        ResourceConfigUtils.runCatching(
                path,
                config.path(),
                () -> parseSection(cached.pack(), path, cached.config()),
                super.errorHandler
        );
    }

    protected abstract void parseSection(Pack pack, Path path, ConfigSection section);
}
