package net.momirealms.craftengine.core.plugin.ui.item.provider;

import net.momirealms.craftengine.core.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface ImmediateItemProvider extends ItemProvider {

    @NotNull
    Item provideImmediately(@NotNull RenderContext context);

    @Override
    @NotNull
    default CompletableFuture<Item> provide(@NotNull RenderContext context) {
        return CompletableFuture.completedFuture(this.provideImmediately(context));
    }
}
