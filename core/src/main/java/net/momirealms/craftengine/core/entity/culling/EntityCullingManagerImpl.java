package net.momirealms.craftengine.core.entity.culling;

import net.momirealms.craftengine.core.plugin.config.Config;

import java.util.ArrayList;
import java.util.List;

public final class EntityCullingManagerImpl implements EntityCullingManager {
    private final List<EntityCullingThread> threads = new ArrayList<>();

    public EntityCullingManagerImpl() {}

    @Override
    public void load() {
        if (Config.enableEntityCulling()) {
            int threads = Math.min(64, Math.max(Config.entityCullingThreads(), 1));
            for (int i = 0; i < threads; i++) {
                EntityCullingThread thread = new EntityCullingThread(i, threads);
                this.threads.add(thread);
                thread.start();
            }
        }
    }

    @Override
    public void unload() {
        if (!this.threads.isEmpty()) {
            for (EntityCullingThread thread : this.threads) {
                thread.stop();
            }
            this.threads.clear();
        }
    }
}
