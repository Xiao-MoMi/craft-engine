package net.momirealms.craftengine.core.plugin.config;

import net.momirealms.craftengine.core.pack.CachedConfigSection;
import net.momirealms.craftengine.core.plugin.config.lifecycle.LoadingStage;

import java.util.List;
import java.util.function.Consumer;

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

    void setErrorHandler(Consumer<ResourceException> errorHandler);

    default int count() {
        return -1;
    }

    default boolean silentIfNotExists() {
        return true;
    }

    default boolean async() {
        return false;
    }
}
