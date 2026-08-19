package net.momirealms.craftengine.core.plugin.ui.item.provider;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@FunctionalInterface
public interface LazyItemProvider {

    /**
     * 启动这一次解析并返回结果阶段.
     * <p>解析出 {@code null} 视为失败, 此时保留占位内容.
     */
    CompletableFuture<? extends ItemProvider> resolve();

    // 把同步的解析函数放到 Paper 全局异步调度器上执行.
    @NotNull
    static LazyItemProvider compute(@NotNull Supplier<? extends ItemProvider> supplier) {
        return () -> {
            CompletableFuture<ItemProvider> future = new CompletableFuture<>();
            CraftEngine.instance().scheduler().async().execute(() -> {
                try {
                    future.complete(supplier.get());
                } catch (Throwable throwable) {
                    future.completeExceptionally(throwable);
                }
            });
            return future;
        };
    }
}
