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
     * 发起本次显示物品的计算.
     * <p><strong>不得修改 Window 或额外请求刷新.</strong> Future 和成功结果均不得为 {@code null}.
     *
     * @param context 当前渲染上下文
     * @return 本次渲染结果的 Future
     */
    @NotNull
    CompletableFuture<? extends Item> provide(@NotNull RenderContext context);

    /**
     * 创建在渲染调用线程立即执行的 Provider.
     *
     * @param renderer 同步渲染函数
     * @return 同步 Provider
     */
    @NotNull
    static ImmediateItemProvider sync(@NotNull Function<? super RenderContext, ? extends Item> renderer) {
        Objects.requireNonNull(renderer, "renderer");
        return renderer::apply;
    }

    /**
     * 创建固定显示同一份只读物品的 Provider, 模板在创建时复制一次.
     *
     * @param template 显示模板
     * @return 固定内容 Provider
     */
    @NotNull
    static ImmediateItemProvider constant(@NotNull Item template) {
        return new ItemWrapper(Objects.requireNonNull(template, "template"));
    }
}
