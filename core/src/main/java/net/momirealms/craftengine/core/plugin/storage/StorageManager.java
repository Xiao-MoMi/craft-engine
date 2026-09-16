package net.momirealms.craftengine.core.plugin.storage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

public final class StorageManager implements AutoCloseable {
    private final Executor executor;
    private final Storage storage;

    public StorageManager(Executor executor, Storage storage) {
        this.executor = executor;
        this.storage = storage;
    }

    public <T> CompletableFuture<T> submit(Operation<T> operation) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return operation.execute(this.storage);
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, this.executor);
    }

    @Override
    public void close() {
        this.storage.close();
    }

    @FunctionalInterface
    public interface Operation<T> {
        T execute(Storage storage) throws Exception;
    }
}
