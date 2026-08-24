package net.momirealms.craftengine.core.pack;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

@ApiStatus.Internal
public interface PackEventDispatcher {

    void onCache(@NotNull PackCacheData cacheData);

    void onInject(@NotNull PackInjection injection);

    void onGenerate(@NotNull Path packFolder, @NotNull Path zipFile);
}
