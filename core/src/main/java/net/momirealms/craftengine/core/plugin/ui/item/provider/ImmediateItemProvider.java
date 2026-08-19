package net.momirealms.craftengine.core.plugin.ui.item.provider;

import net.momirealms.craftengine.core.item.Item;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface ImmediateItemProvider extends ItemProvider {

    /**
     * 当场算出本次要显示的物品.
     * <p>与 {@link ItemProvider#provide(RenderContext)} 受同约束: 只读取,
     * 不得改动 Window、Pane、Inventory, 也不得额外请求刷新或同步.
     */
    @NotNull
    Item provideImmediately(@NotNull RenderContext context);

    @Override
    @NotNull
    default CompletableFuture<? extends Item> provide(@NotNull RenderContext context) {
        return CompletableFuture.completedFuture(Objects.requireNonNull(this.provideImmediately(context), "rendered item"));
    }
}
