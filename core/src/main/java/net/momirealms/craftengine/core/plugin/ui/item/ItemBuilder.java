package net.momirealms.craftengine.core.plugin.ui.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.ui.item.click.BundleSelectClick;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemClick;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemDrag;
import net.momirealms.craftengine.core.plugin.ui.item.guard.ItemGuard;
import net.momirealms.craftengine.core.plugin.ui.item.provider.ImmediateItemProvider;
import net.momirealms.craftengine.core.plugin.ui.item.provider.ItemProvider;
import net.momirealms.craftengine.core.plugin.ui.item.provider.LazyItemProvider;
import net.momirealms.craftengine.core.plugin.ui.item.provider.RenderContext;
import net.momirealms.craftengine.core.plugin.ui.state.Signal;
import net.momirealms.craftengine.core.util.ThrowableUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ItemBuilder {
    // 显示与刷新
    private DisplaySourceFactory source = new DisplaySourceFactory.ProviderFactory(ItemProvider.EMPTY, ItemProvider.EMPTY);
    private boolean sourceConfigured;
    private final List<Function<Player, Signal<?>>> dependencies = new ArrayList<>();
    private boolean updateOnClick;
    // 交互守卫
    @Nullable private ItemGuard<ItemClick> clickGuard;
    @Nullable private ItemGuard<ItemDrag> dragGuard;
    @Nullable private ItemGuard<BundleSelectClick> bundleSelectGuard;
    // 交互处理器
    @Nullable private BiConsumer<Item, ItemClick> clickHandler;
    @Nullable private BiConsumer<Item, ItemDrag> dragHandler;
    @Nullable private BiConsumer<Item, BundleSelectClick> bundleHandler;
    // 构建收尾
    private Consumer<ObservableItem> modifier = ignoredItem -> {};

    /**
     * 创建默认显示空物品的 Builder.
     */
    public ItemBuilder() {
    }

    /**
     * 配置在渲染调用线程立即返回 CraftEngine Item 的显示来源.
     *
     * @param renderer 同步渲染函数
     * @return 此 Builder
     * @throws IllegalStateException 当显示来源已经配置时
     */
    public ItemBuilder setItemProvider(@NotNull Function<RenderContext, net.momirealms.craftengine.core.item.Item> renderer) {
        return this.setAsyncItemProvider(ItemProvider.sync(renderer));
    }

    /**
     * 配置固定显示的物品, 输入模板会复制一次.
     *
     * @param template 固定显示模板
     * @return 此 Builder
     * @throws IllegalStateException 当显示来源已经配置时
     */
    public ItemBuilder setConstantItemProvider(@NotNull net.momirealms.craftengine.core.item.Item template) {
        return this.setAsyncItemProvider(ItemProvider.constant(template));
    }

    /**
     * 配置异步显示来源, 首次成功前显示空物品.
     *
     * @param itemProvider 显示来源
     * @return 此 Builder
     * @throws IllegalStateException 当显示来源已经配置时
     */
    public ItemBuilder setAsyncItemProvider(@NotNull ItemProvider itemProvider) {
        this.setSource(new DisplaySourceFactory.ProviderFactory(itemProvider, ItemProvider.EMPTY));
        return this;
    }

    /**
     * 配置异步显示来源和固定占位物品.
     *
     * @param itemProvider 显示来源
     * @param placeholder 首次成功前显示的模板
     * @return 此 Builder
     * @throws IllegalStateException 当显示来源已经配置时
     */
    public ItemBuilder setAsyncItemProvider(
            @NotNull ItemProvider itemProvider,
            @NotNull net.momirealms.craftengine.core.item.Item placeholder
    ) {
        return this.setAsyncItemProvider(itemProvider, ItemProvider.constant(placeholder));
    }

    /**
     * 配置异步显示来源和同步占位 Provider.
     *
     * @param itemProvider 显示来源
     * @param placeholder 首次成功前使用的占位来源
     * @return 此 Builder
     * @throws IllegalStateException 当显示来源已经配置时
     */
    public ItemBuilder setAsyncItemProvider(@NotNull ItemProvider itemProvider, @NotNull ImmediateItemProvider placeholder) {
        this.setSource(new DisplaySourceFactory.ProviderFactory(itemProvider, placeholder));
        return this;
    }

    /**
     * 配置首次挂载时解析一次的显示来源, 解析完成前显示空物品.
     *
     * @param lazyProvider 懒加载来源
     * @return 此 Builder
     * @throws IllegalStateException 当显示来源已经配置时
     */
    public ItemBuilder setLazyItemProvider(@NotNull LazyItemProvider lazyProvider) {
        return this.setLazyItemProvider(ItemProvider.EMPTY, lazyProvider);
    }

    /**
     * 配置首次挂载时解析一次的显示来源和固定占位物品.
     *
     * @param placeholder 解析完成前显示的模板
     * @param lazyProvider 懒加载来源
     * @return 此 Builder
     * @throws IllegalStateException 当显示来源已经配置时
     */
    public ItemBuilder setLazyItemProvider(
            @NotNull net.momirealms.craftengine.core.item.Item placeholder,
            @NotNull LazyItemProvider lazyProvider
    ) {
        return this.setLazyItemProvider(ItemProvider.constant(placeholder), lazyProvider);
    }

    /**
     * 配置首次挂载时解析一次的显示来源, 同一 Item 的后续挂载复用解析结果.
     *
     * @param placeholder 解析完成前使用的占位来源
     * @param lazyProvider 懒加载来源
     * @return 此 Builder
     * @throws IllegalStateException 当显示来源已经配置时
     */
    public ItemBuilder setLazyItemProvider(@NotNull ImmediateItemProvider placeholder, @NotNull LazyItemProvider lazyProvider) {
        this.setSource(new DisplaySourceFactory.LazyFactory(placeholder, lazyProvider));
        return this;
    }

    /**
     * 声明渲染读取的 Signal, 任一 Signal 失效时重新渲染 Item.
     *
     * @param signals 渲染依赖
     * @return 此 Builder
     */
    public ItemBuilder dependsOn(@NotNull Signal<?>... signals) {
        for (int index = 0; index < signals.length; index++) {
            Signal<?> signal = signals[index];
            this.dependencies.add(ignoredViewer -> signal);
        }
        return this;
    }

    /**
     * 让点击在守卫通过且处理器正常返回后主动使 Item 失效.
     *
     * @return 此 Builder
     */
    public ItemBuilder updateOnClick() {
        this.updateOnClick = true;
        return this;
    }

    /**
     * 添加点击守卫, 第一个拒绝结果会终止本次点击.
     *
     * @param guard 点击守卫
     * @return 此 Builder
     */
    public ItemBuilder addClickGuard(@NotNull ItemGuard<ItemClick> guard) {
        ItemGuard<ItemClick> current = this.clickGuard;
        this.clickGuard = current == null ? guard : current.and(guard);
        return this;
    }

    /**
     * 添加点击守卫和拒绝回调.
     *
     * @param guard 点击守卫
     * @param onRejected 此守卫拒绝时执行的回调
     * @return 此 Builder
     */
    public ItemBuilder addClickGuard(@NotNull ItemGuard<ItemClick> guard, @NotNull Consumer<ItemClick> onRejected) {
        return this.addClickGuard(guard, (ignoredItem, click) -> onRejected.accept(click));
    }

    /**
     * 添加可以访问 Item 自身的点击拒绝回调.
     *
     * @param guard 点击守卫
     * @param onRejected 此守卫拒绝时执行的回调
     * @return 此 Builder
     */
    public ItemBuilder addClickGuard(
            @NotNull ItemGuard<ItemClick> guard,
            @NotNull BiConsumer<Item, ItemClick> onRejected
    ) {
        ItemGuard<ItemClick> current = this.clickGuard;
        this.clickGuard = current == null
                ? guard.onRejected(onRejected)
                : current.and(guard, onRejected);
        return this;
    }

    /**
     * 添加点击处理器, 处理器按添加顺序执行.
     *
     * @param clickHandler 点击处理器
     * @return 此 Builder
     */
    public ItemBuilder addClickHandler(@NotNull Consumer<ItemClick> clickHandler) {
        return this.addClickHandler((ignoredItem, click) -> clickHandler.accept(click));
    }

    /**
     * 添加可以访问 Item 自身的点击处理器.
     *
     * @param clickHandler 点击处理器
     * @return 此 Builder
     */
    public ItemBuilder addClickHandler(@NotNull BiConsumer<Item, ItemClick> clickHandler) {
        BiConsumer<Item, ItemClick> current = this.clickHandler;
        this.clickHandler = current == null ? clickHandler : current.andThen(clickHandler);
        return this;
    }

    /**
     * 添加拖拽守卫.
     *
     * @param guard 拖拽守卫
     * @return 此 Builder
     */
    public ItemBuilder addDragGuard(@NotNull ItemGuard<ItemDrag> guard) {
        ItemGuard<ItemDrag> current = this.dragGuard;
        this.dragGuard = current == null ? guard : current.and(guard);
        return this;
    }

    /**
     * 添加拖拽守卫和拒绝回调.
     *
     * @param guard 拖拽守卫
     * @param onRejected 此守卫拒绝时执行的回调
     * @return 此 Builder
     */
    public ItemBuilder addDragGuard(@NotNull ItemGuard<ItemDrag> guard, @NotNull Consumer<ItemDrag> onRejected) {
        return this.addDragGuard(guard, (ignoredItem, drag) -> onRejected.accept(drag));
    }

    /**
     * 添加可以访问 Item 自身的拖拽拒绝回调.
     *
     * @param guard 拖拽守卫
     * @param onRejected 此守卫拒绝时执行的回调
     * @return 此 Builder
     */
    public ItemBuilder addDragGuard(
            @NotNull ItemGuard<ItemDrag> guard,
            @NotNull BiConsumer<Item, ItemDrag> onRejected
    ) {
        ItemGuard<ItemDrag> current = this.dragGuard;
        this.dragGuard = current == null
                ? guard.onRejected(onRejected)
                : current.and(guard, onRejected);
        return this;
    }

    /**
     * 添加拖拽处理器.
     *
     * @param dragHandler 拖拽处理器
     * @return 此 Builder
     */
    public ItemBuilder addDragHandler(@NotNull Consumer<ItemDrag> dragHandler) {
        return this.addDragHandler((ignoredItem, drag) -> dragHandler.accept(drag));
    }

    /**
     * 添加可以访问 Item 自身的拖拽处理器.
     *
     * @param dragHandler 拖拽处理器
     * @return 此 Builder
     */
    public ItemBuilder addDragHandler(@NotNull BiConsumer<Item, ItemDrag> dragHandler) {
        BiConsumer<Item, ItemDrag> current = this.dragHandler;
        this.dragHandler = current == null ? dragHandler : current.andThen(dragHandler);
        return this;
    }

    /**
     * 添加 Bundle 选择守卫.
     *
     * @param guard Bundle 选择守卫
     * @return 此 Builder
     */
    public ItemBuilder addBundleSelectGuard(@NotNull ItemGuard<BundleSelectClick> guard) {
        ItemGuard<BundleSelectClick> current = this.bundleSelectGuard;
        this.bundleSelectGuard = current == null ? guard : current.and(guard);
        return this;
    }

    /**
     * 添加 Bundle 选择守卫和拒绝回调.
     *
     * @param guard Bundle 选择守卫
     * @param onRejected 此守卫拒绝时执行的回调
     * @return 此 Builder
     */
    public ItemBuilder addBundleSelectGuard(
            @NotNull ItemGuard<BundleSelectClick> guard,
            @NotNull Consumer<BundleSelectClick> onRejected
    ) {
        return this.addBundleSelectGuard(guard, (ignoredItem, select) -> onRejected.accept(select));
    }

    /**
     * 添加可以访问 Item 自身的 Bundle 选择拒绝回调.
     *
     * @param guard Bundle 选择守卫
     * @param onRejected 此守卫拒绝时执行的回调
     * @return 此 Builder
     */
    public ItemBuilder addBundleSelectGuard(
            @NotNull ItemGuard<BundleSelectClick> guard,
            @NotNull BiConsumer<Item, BundleSelectClick> onRejected
    ) {
        ItemGuard<BundleSelectClick> current = this.bundleSelectGuard;
        this.bundleSelectGuard = current == null
                ? guard.onRejected(onRejected)
                : current.and(guard, onRejected);
        return this;
    }

    /**
     * 添加 Bundle 选择处理器, 处理器按添加顺序执行.
     *
     * @param selectHandler Bundle 选择处理器
     * @return 此 Builder
     */
    public ItemBuilder addBundleSelectHandler(@NotNull Consumer<BundleSelectClick> selectHandler) {
        return this.addBundleSelectHandler((ignoredItem, select) -> selectHandler.accept(select));
    }

    /**
     * 添加可以访问 Item 自身的 Bundle 选择处理器.
     *
     * @param selectHandler Bundle 选择处理器
     * @return 此 Builder
     */
    public ItemBuilder addBundleSelectHandler(@NotNull BiConsumer<Item, BundleSelectClick> selectHandler) {
        BiConsumer<Item, BundleSelectClick> current = this.bundleHandler;
        this.bundleHandler = current == null ? selectHandler : current.andThen(selectHandler);
        return this;
    }

    /**
     * 添加构建完成后执行的修改器, 多个修改器按添加顺序执行.
     *
     * @param modifier 构建后修改器
     * @return 此 Builder
     */
    public ItemBuilder addModifier(@NotNull Consumer<? super ObservableItem> modifier) {
        this.modifier = this.modifier.andThen(modifier);
        return this;
    }

    /**
     * 根据当前声明创建独立的 ObservableItem.
     *
     * @return 新 Item
     */
    @NotNull
    public ObservableItem build() {
        ObservableItem item = new ConfiguredItem(
                this.source,
                this.dependencies,
                this.clickGuard,
                this.dragGuard,
                this.bundleSelectGuard,
                this.clickHandler,
                this.dragHandler,
                this.bundleHandler,
                this.updateOnClick
        );
        this.modifier.accept(item);
        return item;
    }

    private void setSource(DisplaySourceFactory source) {
        if (this.sourceConfigured) {
            throw new IllegalStateException("display source has already been configured");
        }
        this.source = source;
        this.sourceConfigured = true;
    }

    sealed interface DisplaySourceFactory permits DisplaySourceFactory.ProviderFactory, DisplaySourceFactory.LazyFactory {

        @NotNull
        DisplaySource create(@NotNull Runnable invalidator);

        record ProviderFactory(@NotNull ItemProvider provider, @NotNull ImmediateItemProvider placeholder) implements DisplaySourceFactory {

            @Override
            @NotNull
            public DisplaySource create(@NotNull Runnable invalidator) {
                return new DisplaySource.FixedDisplaySource(this.provider, this.placeholder);
            }
        }

        record LazyFactory(@NotNull ImmediateItemProvider placeholder, @NotNull LazyItemProvider lazyProvider) implements DisplaySourceFactory {

            @Override
            @NotNull
            public DisplaySource create(@NotNull Runnable invalidator) {
                return new DisplaySource.LazyDisplaySource(this.placeholder, this.lazyProvider, invalidator);
            }
        }
    }

    sealed interface DisplaySource permits DisplaySource.FixedDisplaySource, DisplaySource.LazyDisplaySource {

        @NotNull
        ItemProvider provider();

        @NotNull
        ImmediateItemProvider placeholder();

        default void onAttached() {
        }

        record FixedDisplaySource(@NotNull ItemProvider provider, @NotNull ImmediateItemProvider placeholder) implements DisplaySource {
        }

        final class LazyDisplaySource implements DisplaySource {
            private final @NotNull ImmediateItemProvider placeholder;
            private final @NotNull AtomicReference<LazyItemProvider> pendingProvider;
            private final @NotNull Runnable invalidator;
            private volatile @NotNull ItemProvider currentProvider;

            LazyDisplaySource(@NotNull ImmediateItemProvider placeholder, @NotNull LazyItemProvider lazyProvider, @NotNull Runnable invalidator) {
                this.placeholder = placeholder;
                this.currentProvider = placeholder;
                this.pendingProvider = new AtomicReference<>(lazyProvider);
                this.invalidator = invalidator;
            }

            @Override
            @NotNull
            public ItemProvider provider() {
                return this.currentProvider;
            }

            @Override
            @NotNull
            public ImmediateItemProvider placeholder() {
                return this.placeholder;
            }

            @Override
            public void onAttached() {
                // getAndSet 让并发挂载只有一次能够启动解析.
                LazyItemProvider lazyProvider = this.pendingProvider.getAndSet(null);
                if (lazyProvider == null) return;

                CompletableFuture<ItemProvider> stage;
                try {
                    stage = lazyProvider.resolve();
                } catch (Throwable throwable) {
                    CraftEngine.instance().logger().error("Failed to resolve lazy item provider", throwable);
                    return;
                }

                stage.whenComplete((provider, throwable) -> {
                    if (throwable != null) {
                        CraftEngine.instance().logger().error("Failed to resolve lazy item provider", ThrowableUtils.unwrapCompletion(throwable));
                        return;
                    }
                    // 先发布 Provider, 失效观察者随后能够立即读到新值.
                    this.currentProvider = provider;
                    try {
                        this.invalidator.run();
                    } catch (RuntimeException exception) {
                        CraftEngine.instance().logger().error("Failed to invalidate windows for lazy item", exception);
                    }
                });
            }
        }
    }
}
