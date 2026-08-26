package net.momirealms.craftengine.core.plugin.ui.item.provider;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@FunctionalInterface
public interface LazyItemProvider {

    /**
     * 启动一次 Provider 解析.
     * <p>Future 及其成功结果都不得为 {@code null}.
     *
     * @return 本次解析结果
     */
    @NotNull
    CompletableFuture<ItemProvider> resolve();

    /**
     * 创建在 CraftEngine 异步调度器上执行同步解析函数的来源.
     *
     * @param supplier 同步解析函数
     * @return 异步解析来源
     */
    @NotNull
    static LazyItemProvider compute(@NotNull Supplier<ItemProvider> supplier) {
        return () -> CompletableFuture.supplyAsync(supplier, CraftEngine.instance().scheduler().async());
    }
}
