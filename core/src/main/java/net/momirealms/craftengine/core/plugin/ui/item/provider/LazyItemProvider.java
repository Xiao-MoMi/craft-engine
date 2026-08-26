package net.momirealms.craftengine.core.plugin.ui.item.provider;

import net.momirealms.craftengine.core.plugin.CraftEngine;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@FunctionalInterface
public interface LazyItemProvider {

    @NotNull
    CompletableFuture<? extends ItemProvider> resolve();

    @NotNull
    static LazyItemProvider compute(@NotNull Supplier<? extends ItemProvider> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return () -> CompletableFuture.supplyAsync(
                () -> Objects.requireNonNull(supplier.get(), "resolved provider"),
                CraftEngine.instance().scheduler().async()
        );
    }
}
