package net.momirealms.craftengine.core.plugin.ui.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.plugin.ui.item.click.BundleSelectClick;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemClick;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemDragClick;
import net.momirealms.craftengine.core.plugin.ui.item.guard.ItemGuard;
import net.momirealms.craftengine.core.plugin.ui.item.provider.ImmediateItemProvider;
import net.momirealms.craftengine.core.plugin.ui.item.provider.ItemProvider;
import net.momirealms.craftengine.core.plugin.ui.item.provider.LazyItemProvider;
import net.momirealms.craftengine.core.plugin.ui.item.provider.RenderContext;
import net.momirealms.craftengine.core.plugin.ui.signal.Signal;
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
    private DisplaySourceFactory source = new DisplaySourceFactory.ProviderFactory(ItemProvider.EMPTY, ItemProvider.EMPTY); // 显示来源声明, 只能配置一次
    private boolean sourceConfigured; // 显示来源是否已完成配置

    private final List<ConfiguredItem.GuardEntry<ItemClick>> clickGuards = new ArrayList<>(); // 点击前置处理器
    private final List<ConfiguredItem.GuardEntry<ItemDragClick>> dragGuards = new ArrayList<>(); // 拖拽前置处理器
    private final List<ConfiguredItem.GuardEntry<BundleSelectClick>> bundleSelectGuards = new ArrayList<>(); // Bundle 前置处理器
    private BiConsumer<Item, ItemClick> clickHandler = (ignoredItem, ignoredClick) -> { };      // 点击处理器
    private BiConsumer<Item, ItemDragClick> dragHandler = (ignoredItem, ignoredDrag) -> { };         // 拖拽处理器
    private BiConsumer<Item, BundleSelectClick> bundleHandler = (ignoredItem, ignoredSelect) -> { }; // Bundle 选择处理器
    private Consumer<ObservableItem> modifier = ignoredItem -> { }; // 构建完成后执行的修改器链
    private final List<Function<? super Player, ? extends Signal<?>>> dependencies = new ArrayList<>(); // 渲染依赖声明的 signal
    private boolean updateOnClick; // 点击成功后是否主动失效

    /**
     * 配置在渲染线程立即返回 ItemStack 的同步 renderer.
     *
     * @param renderer 同步渲染函数
     * @return 此构建器
     */
    public ItemBuilder setItemProvider(@NotNull Function<? super RenderContext, ? extends net.momirealms.craftengine.core.item.Item> renderer) {
        return this.setItemProviderAsync(ItemProvider.sync(renderer));
    }

    /**
     * 配置固定显示的物品.
     *
     * @param item 固定物品模板
     * @return 此构建器
     */
    public ItemBuilder setItemProviderConstant(@NotNull net.momirealms.craftengine.core.item.Item item) {
        return this.setItemProviderAsync(ItemProvider.constant(item));
    }

    /**
     * 配置 ItemProvider, 未完成的 Future 暂时显示最近一次成功结果或空物品.
     *
     * @param itemProvider 显示提供器
     * @return 此构建器
     */
    public ItemBuilder setItemProviderAsync(@NotNull ItemProvider itemProvider) {
        this.setSource(new DisplaySourceFactory.ProviderFactory(Objects.requireNonNull(itemProvider, "itemProvider"), ItemProvider.EMPTY));
        return this;
    }

    /**
     * 配置 ItemProvider 及其首次成功结果前显示的占位物品.
     *
     * @param itemProvider 显示提供器
     * @param placeholder 首次成功结果前显示的占位物品
     * @return 此构建器
     */
    public ItemBuilder setItemProviderAsync(@NotNull ItemProvider itemProvider, @NotNull net.momirealms.craftengine.core.item.Item placeholder) {
        return this.setItemProviderAsync(itemProvider, ItemProvider.constant(placeholder));
    }

    /**
     * 配置 ItemProvider 及其首次成功结果前使用的占位提供器.
     *
     * @param itemProvider 显示提供器
     * @param placeholder 首次成功结果前使用的占位提供器
     * @return 此构建器
     */
    public ItemBuilder setItemProviderAsync(@NotNull ItemProvider itemProvider, @NotNull ImmediateItemProvider placeholder) {
        this.setSource(new DisplaySourceFactory.ProviderFactory(
                Objects.requireNonNull(itemProvider, "itemProvider"),
                Objects.requireNonNull(placeholder, "placeholder")
        ));
        return this;
    }

    /**
     * 配置第一次挂载时解析一次的懒加载显示来源, 解析完成前显示空物品.
     *
     * @param lazyProvider 懒加载显示提供器
     * @return 此构建器
     */
    public ItemBuilder setLazyItemProvider(@NotNull LazyItemProvider lazyProvider) {
        return this.setLazyItemProvider(ItemProvider.EMPTY, lazyProvider);
    }

    /**
     * 配置第一次挂载时解析一次的懒加载显示来源.
     * <p>解析出来的 Provider 由这件 Item 的全部显示挂载共用, 之后不再解析.
     *
     * @param placeholder 解析完成前的显示内容
     * @param lazyProvider 懒加载显示提供器
     * @return 此构建器
     */
    public ItemBuilder setLazyItemProvider(@NotNull net.momirealms.craftengine.core.item.Item placeholder, @NotNull LazyItemProvider lazyProvider) {
        return this.setLazyItemProvider(
                ItemProvider.constant(Objects.requireNonNull(placeholder, "placeholder")),
                lazyProvider
        );
    }

    /**
     * 配置第一次挂载时解析一次的懒加载显示来源.
     * <p>解析出来的 Provider 由这件 Item 的全部显示挂载共用, 之后不再解析.
     *
     * @param placeholder 解析完成前的显示内容
     * @param lazyProvider 懒加载显示提供器
     * @return 此构建器
     */
    public ItemBuilder setLazyItemProvider(@NotNull ImmediateItemProvider placeholder, @NotNull LazyItemProvider lazyProvider) {
        this.setSource(new DisplaySourceFactory.LazyFactory(
                Objects.requireNonNull(placeholder, "placeholder"),
                Objects.requireNonNull(lazyProvider, "lazyProvider")
        ));
        return this;
    }

//    /**
//     * 让 Item 在被显示期间每隔固定 tick 重新渲染一次.
//     *
//     * @param periodTicks 正数 tick 周期
//     * @return 此构建器
//     */
//    public ItemBuilder updatePeriodically(int periodTicks) {
//        return this.dependsOn(Signals.everyTicks(periodTicks));
//    }

    /**
     * 声明渲染读取了哪些 Signal, 失效时重新渲染这个 Item.
     *
     * @param signals 渲染依赖的数据源
     * @return 此构建器
     */
    public ItemBuilder dependsOn(@NotNull Signal<?>... signals) {
        for (int index = 0; index < signals.length; index++) {
            Signal<?> signal = Objects.requireNonNull(signals[index], "signal");
            this.dependencies.add(ignoredViewer -> signal);
        }
        return this;
    }

//    public ItemBuilder dependsOn(@NotNull PlayerKeyedSignal<?> signal) {
//        Objects.requireNonNull(signal, "signal");
//        this.dependencies.add(viewer -> signal.at(viewer.getUniqueId()));
//        return this;
//    }
//
//    public <K> ItemBuilder dependsOn(@NotNull KeyedSignal<K, ?> signal, @NotNull Function<? super Player, ? extends K> keyOf) {
//        Objects.requireNonNull(signal, "signal");
//        Objects.requireNonNull(keyOf, "keyOf");
//        this.dependencies.add(viewer -> signal.at(keyOf.apply(viewer)));
//        return this;
//    }

    /**
     * 配置点击处理器成功完成后主动失效 Item.
     *
     * @return 此构建器
     */
    public ItemBuilder updateOnClick() {
        this.updateOnClick = true;
        return this;
    }

    /**
     * 添加点击前置处理器.
     * 添加顺序执行, 第一个返回 false 的守卫会拒绝点击.
     *
     * @param guard 点击前置处理器
     * @return 此构建器
     */
    public ItemBuilder addClickGuard(@NotNull ItemGuard<? super ItemClick> guard) {
        return this.addClickGuard(guard, (ignoredItem, ignoredClick) -> { });
    }

    /**
     * 添加点击前置处理器与拒绝回调.
     *
     * @param guard 点击前置处理器
     * @param onRejected 点击前置处理器返回 false 时执行的回调
     * @return 此构建器
     */
    public ItemBuilder addClickGuard(@NotNull ItemGuard<? super ItemClick> guard, @NotNull Consumer<? super ItemClick> onRejected) {
        Objects.requireNonNull(onRejected, "onRejected");
        return this.addClickGuard(guard, (ignoredItem, click) -> onRejected.accept(click));
    }

    /**
     * 添加点击前置处理器与拒绝回调.
     *
     * @param guard 点击前置处理器
     * @param onRejected 点击前置处理器返回 false 时执行的回调
     * @return 此构建器
     */
    public ItemBuilder addClickGuard(@NotNull ItemGuard<? super ItemClick> guard, @NotNull BiConsumer<? super Item, ? super ItemClick> onRejected) {
        this.clickGuards.add(new ConfiguredItem.GuardEntry<>(
                Objects.requireNonNull(guard, "guard"),
                Objects.requireNonNull(onRejected, "onRejected")
        ));
        return this;
    }

    /**
     * 添加点击处理器. 处理器按添加顺序执行.
     *
     * @param clickHandler 点击处理器
     * @return 此构建器
     */
    public ItemBuilder addClickHandler(@NotNull Consumer<? super ItemClick> clickHandler) {
        return this.addClickHandler((ignoredItem, click) -> clickHandler.accept(click));
    }

    /**
     * 添加可以访问 Item 自身的点击处理器, 供处理逻辑同时读取物品和点击事件.
     *
     * @param clickHandler 同时接收物品和点击事件的处理器
     * @return 此构建器
     */
    public ItemBuilder addClickHandler(@NotNull BiConsumer<? super Item, ? super ItemClick> clickHandler) {
        this.clickHandler = this.clickHandler.andThen(clickHandler);
        return this;
    }

    /**
     * 添加拖拽前置处理器.
     *
     * @param guard 拖拽前置处理器
     * @return 此构建器
     */
    public ItemBuilder addDragGuard(@NotNull ItemGuard<? super ItemDragClick> guard) {
        return this.addDragGuard(guard, (ignoredItem, ignoredDrag) -> { });
    }

    /**
     * 添加拖拽前置处理器与拒绝回调.
     *
     * @param guard 拖拽前置处理器
     * @param onRejected 前置处理器返回 false 时执行的回调
     * @return 此构建器
     */
    public ItemBuilder addDragGuard(@NotNull ItemGuard<? super ItemDragClick> guard, @NotNull Consumer<? super ItemDragClick> onRejected) {
        Objects.requireNonNull(onRejected, "onRejected");
        return this.addDragGuard(guard, (ignoredItem, drag) -> onRejected.accept(drag));
    }

    /**
     * 添加拖拽前置处理器与拒绝回调.
     *
     * @param guard 拖拽前置处理器
     * @param onRejected 前置处理器返回 false 时执行的回调
     * @return 此构建器
     */
    public ItemBuilder addDragGuard(@NotNull ItemGuard<? super ItemDragClick> guard, @NotNull BiConsumer<? super Item, ? super ItemDragClick> onRejected) {
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
     * @return 此构建器
     */
    public ItemBuilder addDragHandler(@NotNull Consumer<? super ItemDragClick> dragHandler) {
        return this.addDragHandler((ignoredItem, drag) -> dragHandler.accept(drag));
    }

    /**
     * 添加可以访问 Item 自身的拖拽处理器, 供处理逻辑同时读取物品和拖拽事件.
     *
     * @param dragHandler 同时接收物品和拖拽事件的处理器
     * @return 此构建器
     */
    public ItemBuilder addDragHandler(@NotNull BiConsumer<? super Item, ? super ItemDragClick> dragHandler) {
        this.dragHandler = this.dragHandler.andThen(dragHandler);
        return this;
    }

    /**
     * 添加 Bundle 选择前置处理器.
     *
     * @param guard Bundle 选择前置处理器
     * @return 此构建器
     */
    public ItemBuilder addBundleSelectGuard(@NotNull ItemGuard<? super BundleSelectClick> guard) {
        return this.addBundleSelectGuard(guard, (ignoredItem, ignoredSelect) -> { });
    }

    /**
     * 添加 Bundle 选择前置处理器与拒绝回调.
     *
     * @param guard Bundle 选择前置处理器
     * @param onRejected 前置处理器返回 false 时执行的回调
     * @return 此构建器
     */
    public ItemBuilder addBundleSelectGuard(@NotNull ItemGuard<? super BundleSelectClick> guard, @NotNull Consumer<? super BundleSelectClick> onRejected) {
        Objects.requireNonNull(onRejected, "onRejected");
        return this.addBundleSelectGuard(guard, (ignoredItem, select) -> onRejected.accept(select));
    }

    /**
     * 添加 Bundle 选择前置处理器与拒绝回调.
     *
     * @param guard Bundle 选择前置处理器
     * @param onRejected 前置处理器返回 false 时执行的回调
     * @return 此构建器
     */
    public ItemBuilder addBundleSelectGuard(@NotNull ItemGuard<? super BundleSelectClick> guard, @NotNull BiConsumer<? super Item, ? super BundleSelectClick> onRejected) {
        this.bundleSelectGuards.add(new ConfiguredItem.GuardEntry<>(
                Objects.requireNonNull(guard, "guard"),
                Objects.requireNonNull(onRejected, "onRejected")
        ));
        return this;
    }

    /**
     * 添加 Bundle 选择处理器. 处理器按添加顺序执行.
     *
     * @param selectHandler 选择处理器
     * @return 此构建器
     */
    public ItemBuilder addBundleSelectHandler(@NotNull Consumer<? super BundleSelectClick> selectHandler) {
        return this.addBundleSelectHandler((ignoredItem, select) -> selectHandler.accept(select));
    }

    /**
     * 添加可以访问 Item 自身的 Bundle 选择处理器.
     *
     * @param selectHandler 同时接收物品和选择事件的处理器
     * @return 此构建器
     */
    public ItemBuilder addBundleSelectHandler(@NotNull BiConsumer<? super Item, ? super BundleSelectClick> selectHandler) {
        this.bundleHandler = this.bundleHandler.andThen(selectHandler);
        return this;
    }

    /**
     * 添加在 Item 完整构建后执行的修改器. 修改器按添加顺序执行.
     * <p>修改器可以保存 Item 引用, 建立外部注册关系或调用 {@link ObservableItem#notifyWindows()}.
     * 如果某个修改器抛出异常, 后续修改器不会执行, 异常由 {@link #build()} 直接抛出.</p>
     *
     * @param modifier 构建完成后的修改器
     * @return 此构建器
     */
    public ItemBuilder addModifier(@NotNull Consumer<? super ObservableItem> modifier) {
        this.modifier = this.modifier.andThen(modifier);
        return this;
    }

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
        // 构建完成后按添加顺序执行修改器, 让调用方拿到完整的 Item
        this.modifier.accept(item);
        return item;
    }

    // 写入显示来源声明, 并保证只配置一次.
    private void setSource(DisplaySourceFactory source) {
        if (this.sourceConfigured)
            throw new IllegalStateException("display source has already been configured");
        this.source = source;
        this.sourceConfigured = true;
    }

    // 构建器阶段的显示来源声明, 每次 {@link #build()} 都创建一个独立的 {@link DisplaySource}.
    sealed interface DisplaySourceFactory permits DisplaySourceFactory.ProviderFactory, DisplaySourceFactory.LazyFactory {

        DisplaySource create(Runnable invalidator);

        // 固定或上下文来源声明
        record ProviderFactory(ItemProvider provider, ImmediateItemProvider placeholder) implements DisplaySourceFactory {

            @Override
            public DisplaySource create(Runnable invalidator) {
                // 固定来源没有解析阶段, 失效回调用不上
                return new DisplaySource.FixedDisplaySource(this.provider, this.placeholder);
            }
        }

        // 懒加载来源声明
        record LazyFactory(ImmediateItemProvider placeholder, LazyItemProvider lazyProvider) implements DisplaySourceFactory {

            @Override
            public DisplaySource create(Runnable invalidator) {
                return new DisplaySource.LazyDisplaySource(this.placeholder, this.lazyProvider, invalidator);
            }
        }
    }

    // Item 的显示来源, 决定每次渲染使用的提供器与挂载行为.
    sealed interface DisplaySource permits DisplaySource.FixedDisplaySource, DisplaySource.LazyDisplaySource {

        // 获取当前渲染使用的提供器.
        ItemProvider provider();

        // 获取首次成功结果前使用的占位提供器.
        ImmediateItemProvider placeholder();

        // Item 挂载到槽位时的回调. 默认无操作.
        default void onAttached() {
        }

        // 固定不变的显示来源
        record FixedDisplaySource(@NotNull ItemProvider provider, @NotNull ImmediateItemProvider placeholder) implements DisplaySource {
            public FixedDisplaySource {
                Objects.requireNonNull(provider, "provider");
                Objects.requireNonNull(placeholder, "placeholder");
            }
        }

        // 第一次挂载时异步解析一次, 之后复用结果的懒加载显示来源.
        final class LazyDisplaySource implements DisplaySource {
            private final ImmediateItemProvider placeholder; // 占位提供器, 解析出的显示来源首次成功前继续使用
            private final AtomicReference<LazyItemProvider> pendingProvider; // 挂起的提供器, 取出后置 null 保证只解析一次
            private final Runnable invalidator;                         // 解析完成后通知 Window 失效的回调

            private volatile ItemProvider currentProvider;              // 当前渲染使用的提供器, 初始为占位内容, 解析完成后替换

            /**
             * 创建懒加载显示来源, 解析完成前渲染占位内容.
             *
             * @param placeholder 解析完成前的占位提供器
             * @param lazyProvider 懒加载显示提供器
             * @param invalidator 解析完成后通知 Window 失效的回调
             */
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

            // 仅第一次挂载真正提交解析, 后续直接复用结果.
            @Override
            public void onAttached() {
                // 取出并清空挂起的提供器, 保证同一 Item 多次挂载也只执行一次解析
                LazyItemProvider lazyProvider = this.pendingProvider.getAndSet(null);
                if (lazyProvider == null) return;

                // 同步抛出同样视为解析失败, 与异步异常走同一通道
                CompletableFuture<? extends ItemProvider> stage;
                try {
                    stage = Objects.requireNonNull(lazyProvider.resolve(), "lazyProvider result");
                } catch (Throwable throwable) {
                    CraftEngine.instance().logger().warn("Failed to resolve lazy item provider", throwable);
                    return;
                }

                stage.whenComplete((provider, throwable) -> {
                    // 加载失败时转发异常, 保留占位显示
                    if (throwable != null) {
                        CraftEngine.instance().logger().warn("Failed to resolve lazy item provider", ThrowableUtils.unwrapCompletion(throwable));
                        return;
                    }
                    // 解析结果为 null 也视为失败, 避免渲染时空指针
                    if (provider == null) {
                        CraftEngine.instance().logger().warn("Failed to resolve lazy item provider", new NullPointerException("resolved provider"));
                        return;
                    }

                    // 替换当前提供器并通知窗口重新渲染
                    this.currentProvider = provider;
                    try {
                        this.invalidator.run();
                    } catch (RuntimeException exception) {
                        CraftEngine.instance().logger().warn("Failed to invalidate windows for lazy item", exception);
                    }
                });
            }
        }

    }
}
