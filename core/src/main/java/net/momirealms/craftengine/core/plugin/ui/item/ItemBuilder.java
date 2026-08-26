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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

public final class ItemBuilder {
    // 显示与刷新
    private DisplaySourceFactory source = new DisplaySourceFactory.ProviderFactory(ItemProvider.EMPTY, ItemProvider.EMPTY);
    private boolean sourceConfigured;
    private final List<Function<? super Player, ? extends Signal<?>>> dependencies = new ArrayList<>();
    private boolean updateOnClick;
    // 交互守卫
    private final List<ConfiguredItem.GuardEntry<ItemClick>> clickGuards = new ArrayList<>();
    private final List<ConfiguredItem.GuardEntry<ItemDrag>> dragGuards = new ArrayList<>();
    private final List<ConfiguredItem.GuardEntry<BundleSelectClick>> bundleSelectGuards = new ArrayList<>();
    // 交互处理器
    private BiConsumer<Item, ItemClick> clickHandler = (ignoredItem, ignoredClick) -> {};
    private BiConsumer<Item, ItemDrag> dragHandler = (ignoredItem, ignoredDrag) -> {};
    private BiConsumer<Item, BundleSelectClick> bundleHandler = (ignoredItem, ignoredSelect) -> {};
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
    public ItemBuilder setItemProvider(@NotNull Function<? super RenderContext, ? extends net.momirealms.craftengine.core.item.Item> renderer) {
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
        this.setSource(new DisplaySourceFactory.ProviderFactory(Objects.requireNonNull(itemProvider, "itemProvider"), ItemProvider.EMPTY));
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
        this.setSource(new DisplaySourceFactory.ProviderFactory(
                Objects.requireNonNull(itemProvider, "itemProvider"),
                Objects.requireNonNull(placeholder, "placeholder")
        ));
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
        this.setSource(new DisplaySourceFactory.LazyFactory(
                Objects.requireNonNull(placeholder, "placeholder"),
                Objects.requireNonNull(lazyProvider, "lazyProvider")
        ));
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
            Signal<?> signal = Objects.requireNonNull(signals[index], "signal");
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
    public ItemBuilder addClickGuard(@NotNull ItemGuard<? super ItemClick> guard) {
        return this.addClickGuard(guard, (ignoredItem, ignoredClick) -> {});
    }

    /**
     * 添加点击守卫和拒绝回调.
     *
     * @param guard 点击守卫
     * @param onRejected 此守卫拒绝时执行的回调
     * @return 此 Builder
     */
    public ItemBuilder addClickGuard(@NotNull ItemGuard<? super ItemClick> guard, @NotNull Consumer<? super ItemClick> onRejected) {
        Objects.requireNonNull(onRejected, "onRejected");
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
            @NotNull ItemGuard<? super ItemClick> guard,
            @NotNull BiConsumer<? super Item, ? super ItemClick> onRejected
    ) {
        this.clickGuards.add(new ConfiguredItem.GuardEntry<>(
                Objects.requireNonNull(guard, "guard"),
                Objects.requireNonNull(onRejected, "onRejected")
        ));
        return this;
    }

    /**
     * 添加点击处理器, 处理器按添加顺序执行.
     *
     * @param clickHandler 点击处理器
     * @return 此 Builder
     */
    public ItemBuilder addClickHandler(@NotNull Consumer<? super ItemClick> clickHandler) {
        Objects.requireNonNull(clickHandler, "clickHandler");
        return this.addClickHandler((ignoredItem, click) -> clickHandler.accept(click));
    }

    /**
     * 添加可以访问 Item 自身的点击处理器.
     *
     * @param clickHandler 点击处理器
     * @return 此 Builder
     */
    public ItemBuilder addClickHandler(@NotNull BiConsumer<? super Item, ? super ItemClick> clickHandler) {
        this.clickHandler = this.clickHandler.andThen(clickHandler);
        return this;
    }

    /**
     * 添加拖拽守卫.
     *
     * @param guard 拖拽守卫
     * @return 此 Builder
     */
    public ItemBuilder addDragGuard(@NotNull ItemGuard<? super ItemDrag> guard) {
        return this.addDragGuard(guard, (ignoredItem, ignoredDrag) -> {});
    }

    /**
     * 添加拖拽守卫和拒绝回调.
     *
     * @param guard 拖拽守卫
     * @param onRejected 此守卫拒绝时执行的回调
     * @return 此 Builder
     */
    public ItemBuilder addDragGuard(@NotNull ItemGuard<? super ItemDrag> guard, @NotNull Consumer<? super ItemDrag> onRejected) {
        Objects.requireNonNull(onRejected, "onRejected");
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
            @NotNull ItemGuard<? super ItemDrag> guard,
            @NotNull BiConsumer<? super Item, ? super ItemDrag> onRejected
    ) {
        this.dragGuards.add(new ConfiguredItem.GuardEntry<>(
                Objects.requireNonNull(guard, "guard"),
                Objects.requireNonNull(onRejected, "onRejected")
        ));
        return this;
    }

    /**
     * 添加拖拽处理器.
     *
     * @param dragHandler 拖拽处理器
     * @return 此 Builder
     */
    public ItemBuilder addDragHandler(@NotNull Consumer<? super ItemDrag> dragHandler) {
        Objects.requireNonNull(dragHandler, "dragHandler");
        return this.addDragHandler((ignoredItem, drag) -> dragHandler.accept(drag));
    }

    /**
     * 添加可以访问 Item 自身的拖拽处理器.
     *
     * @param dragHandler 拖拽处理器
     * @return 此 Builder
     */
    public ItemBuilder addDragHandler(@NotNull BiConsumer<? super Item, ? super ItemDrag> dragHandler) {
        this.dragHandler = this.dragHandler.andThen(dragHandler);
        return this;
    }

    /**
     * 添加 Bundle 选择守卫.
     *
     * @param guard Bundle 选择守卫
     * @return 此 Builder
     */
    public ItemBuilder addBundleSelectGuard(@NotNull ItemGuard<? super BundleSelectClick> guard) {
        return this.addBundleSelectGuard(guard, (ignoredItem, ignoredSelect) -> {});
    }

    /**
     * 添加 Bundle 选择守卫和拒绝回调.
     *
     * @param guard Bundle 选择守卫
     * @param onRejected 此守卫拒绝时执行的回调
     * @return 此 Builder
     */
    public ItemBuilder addBundleSelectGuard(
            @NotNull ItemGuard<? super BundleSelectClick> guard,
            @NotNull Consumer<? super BundleSelectClick> onRejected
    ) {
        Objects.requireNonNull(onRejected, "onRejected");
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
            @NotNull ItemGuard<? super BundleSelectClick> guard,
            @NotNull BiConsumer<? super Item, ? super BundleSelectClick> onRejected
    ) {
        this.bundleSelectGuards.add(new ConfiguredItem.GuardEntry<>(
                Objects.requireNonNull(guard, "guard"),
                Objects.requireNonNull(onRejected, "onRejected")
        ));
        return this;
    }

    /**
     * 添加 Bundle 选择处理器, 处理器按添加顺序执行.
     *
     * @param selectHandler Bundle 选择处理器
     * @return 此 Builder
     */
    public ItemBuilder addBundleSelectHandler(@NotNull Consumer<? super BundleSelectClick> selectHandler) {
        Objects.requireNonNull(selectHandler, "selectHandler");
        return this.addBundleSelectHandler((ignoredItem, select) -> selectHandler.accept(select));
    }

    /**
     * 添加可以访问 Item 自身的 Bundle 选择处理器.
     *
     * @param selectHandler Bundle 选择处理器
     * @return 此 Builder
     */
    public ItemBuilder addBundleSelectHandler(@NotNull BiConsumer<? super Item, ? super BundleSelectClick> selectHandler) {
        this.bundleHandler = this.bundleHandler.andThen(selectHandler);
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
                this.clickGuards,
                this.dragGuards,
                this.bundleSelectGuards,
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

        DisplaySource create(Runnable invalidator);

        record ProviderFactory(ItemProvider provider, ImmediateItemProvider placeholder) implements DisplaySourceFactory {

            @Override
            public DisplaySource create(Runnable invalidator) {
                return new DisplaySource.FixedDisplaySource(this.provider, this.placeholder);
            }
        }

        record LazyFactory(ImmediateItemProvider placeholder, LazyItemProvider lazyProvider) implements DisplaySourceFactory {

            @Override
            public DisplaySource create(Runnable invalidator) {
                return new DisplaySource.LazyDisplaySource(this.placeholder, this.lazyProvider, invalidator);
            }
        }
    }

    sealed interface DisplaySource permits DisplaySource.FixedDisplaySource, DisplaySource.LazyDisplaySource {

        ItemProvider provider();

        ImmediateItemProvider placeholder();

        default void onAttached() {
        }

        record FixedDisplaySource(@NotNull ItemProvider provider, @NotNull ImmediateItemProvider placeholder) implements DisplaySource {

            public FixedDisplaySource {
                Objects.requireNonNull(provider, "provider");
                Objects.requireNonNull(placeholder, "placeholder");
            }
        }

        final class LazyDisplaySource implements DisplaySource {
            private final ImmediateItemProvider placeholder;
            private final AtomicReference<LazyItemProvider> pendingProvider;
            private final Runnable invalidator;
            private volatile ItemProvider currentProvider;

            LazyDisplaySource(ImmediateItemProvider placeholder, LazyItemProvider lazyProvider, Runnable invalidator) {
                this.placeholder = Objects.requireNonNull(placeholder, "placeholder");
                this.currentProvider = placeholder;
                this.pendingProvider = new AtomicReference<>(Objects.requireNonNull(lazyProvider, "lazyProvider"));
                this.invalidator = Objects.requireNonNull(invalidator, "invalidator");
            }

            @Override
            public ItemProvider provider() {
                return this.currentProvider;
            }

            @Override
            public ImmediateItemProvider placeholder() {
                return this.placeholder;
            }

            @Override
            public void onAttached() {
                // getAndSet 让并发挂载只有一次能够启动解析.
                LazyItemProvider lazyProvider = this.pendingProvider.getAndSet(null);
                if (lazyProvider == null) return;

                CompletableFuture<? extends ItemProvider> stage;
                try {
                    stage = Objects.requireNonNull(lazyProvider.resolve(), "lazyProvider result");
                } catch (Throwable throwable) {
                    CraftEngine.instance().logger().error("Failed to resolve lazy item provider", throwable);
                    return;
                }

                stage.whenComplete((provider, throwable) -> {
                    if (throwable != null) {
                        CraftEngine.instance().logger().error("Failed to resolve lazy item provider", ThrowableUtils.unwrapCompletion(throwable));
                        return;
                    }
                    if (provider == null) {
                        CraftEngine.instance().logger().error("Failed to resolve lazy item provider", new NullPointerException("resolved provider"));
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
