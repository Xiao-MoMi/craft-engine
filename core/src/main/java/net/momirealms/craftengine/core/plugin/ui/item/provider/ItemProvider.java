package net.momirealms.craftengine.core.plugin.ui.item.provider;

import net.momirealms.craftengine.core.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@FunctionalInterface
public interface ItemProvider {
    ImmediateItemProvider EMPTY = ItemProvider.sync(ignoredContext -> Item.empty());

    /**
     * 发起本次要显示物品的计算.
     * <p><strong>不得改动 Window、Pane、Inventory, 也不得额外请求刷新或同步.</strong>
     */
    @NotNull
    CompletableFuture<? extends Item> provide(@NotNull RenderContext context);

    @NotNull
    static ImmediateItemProvider sync(@NotNull Function<? super RenderContext, ? extends Item> renderer) {
        Objects.requireNonNull(renderer, "renderer");
        return renderer::apply;
    }

    @NotNull
    static ImmediateItemProvider constant(@NotNull Item template) {
        return new ItemWrapper(Objects.requireNonNull(template, "template"));
    }
}
