package net.momirealms.craftengine.core.util;

import net.momirealms.craftengine.core.plugin.config.KnownResourceException;
import net.momirealms.craftengine.core.plugin.config.ResourceException;
import net.momirealms.craftengine.core.plugin.config.UnknownResourceException;

import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class ResourceConfigUtils {
    private ResourceConfigUtils() {}

    public static void runCatching(Path filePath, String node, Runnable runnable, Consumer<ResourceException> collector) {
        try {
            runnable.run();
        } catch (KnownResourceException e) {
            e.setFilePath(filePath);
            collector.accept(e);
        } catch (Throwable t) {
            collector.accept(new UnknownResourceException(filePath, node, t));
        }
    }

    public static void runCatching(Path filePath, String node, Executor executor, Runnable runnable, Consumer<ResourceException> collector) {
        executor.execute(() -> {
            try {
                runnable.run();
            } catch (KnownResourceException e) {
                e.setFilePath(filePath);
                collector.accept(e);
            } catch (Throwable t) {
                collector.accept(new UnknownResourceException(filePath, node, t));
            }
        });
    }
}
