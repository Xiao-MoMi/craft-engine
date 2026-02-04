package net.momirealms.craftengine.core.plugin.config;

import net.momirealms.craftengine.core.pack.CachedConfigSection;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;

import java.util.List;

public interface ConfigParser {

    String[] sectionId();

    LoadingStage loadingStage();

    default List<LoadingStage> dependencies() {
        return List.of();
    }

    default void postProcess() {
    }

    default void preProcess() {
    }

    void addConfig(CachedConfigSection section);

    void loadAll();

    void clearConfigs();

    default int count() {
        return -1;
    }

    default boolean silentIfNotExists() {
        return true;
    }
}
