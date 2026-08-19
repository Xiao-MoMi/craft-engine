package net.momirealms.craftengine.core.plugin.ui.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.ui.Observer;
import net.momirealms.craftengine.core.plugin.ui.internal.ObservableDispatcher;
import net.momirealms.craftengine.core.plugin.ui.item.click.BundleSelectClick;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemClick;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemDragClick;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemInteraction;
import net.momirealms.craftengine.core.plugin.ui.item.guard.ItemGuard;
import net.momirealms.craftengine.core.plugin.ui.item.provider.ImmediateItemProvider;
import net.momirealms.craftengine.core.plugin.ui.item.provider.ItemProvider;
import net.momirealms.craftengine.core.plugin.ui.signal.Signal;
import net.momirealms.craftengine.core.plugin.ui.window.Window;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

final class ConfiguredItem implements ObservableItem {
    private final ItemBuilder.DisplaySource displaySource; // 显示来源, 决定渲染提供器与挂载行为
    private final List<GuardEntry<ItemClick>> clickGuards; // 点击前置处理器
    private final List<GuardEntry<ItemDragClick>> dragGuards; // 拖拽前置处理器
    private final List<GuardEntry<BundleSelectClick>> bundleSelectGuards; // Bundle 选择前置处理器
    private final BiConsumer<? super Item, ? super ItemClick> clickHandler;     // 点击处理器
    private final BiConsumer<? super Item, ? super ItemDragClick> dragHandler;       // 拖拽处理器
    private final BiConsumer<? super Item, ? super BundleSelectClick> bundleHandler; // Bundle 选择处理器
    private final boolean updateOnClick; // 点击成功后是否主动失效
    private final List<Function<? super Player, ? extends Signal<?>>> dependencies; // 构建器声明的依赖, 每次挂载按查看者解析
    private final ObservableDispatcher<Item> observers = new ObservableDispatcher<>(); // 挂载观察者注册表, 负责广播失效

    ConfiguredItem(
            @NotNull ItemBuilder.DisplaySourceFactory source,
            @NotNull List<? extends Function<? super Player, ? extends Signal<?>>> dependencies,
            @NotNull List<? extends GuardEntry<ItemClick>> clickGuards,
            @NotNull List<? extends GuardEntry<ItemDragClick>> dragGuards,
            @NotNull List<? extends GuardEntry<BundleSelectClick>> bundleSelectGuards,
            @NotNull BiConsumer<? super Item, ? super ItemClick> clickHandler,
            @NotNull BiConsumer<? super Item, ? super ItemDragClick> dragHandler,
            @NotNull BiConsumer<? super Item, ? super BundleSelectClick> bundleHandler,
            boolean updateOnClick
    ) {
        this.displaySource = Objects.requireNonNull(source.create(this::notifyWindows), "source result");
        this.dependencies = List.copyOf(dependencies);
        this.clickGuards = List.copyOf(clickGuards);
        this.dragGuards = List.copyOf(dragGuards);
        this.bundleSelectGuards = List.copyOf(bundleSelectGuards);
        this.clickHandler = Objects.requireNonNull(clickHandler, "clickHandler");
        this.dragHandler = Objects.requireNonNull(dragHandler, "dragHandler");
        this.bundleHandler = Objects.requireNonNull(bundleHandler, "bundleHandler");
        this.updateOnClick = updateOnClick;
    }

    @NotNull
    @Override
    public ItemProvider getItemProvider() {
        return this.displaySource.provider();
    }

    @NotNull
    @Override
    public ImmediateItemProvider getPlaceholder() {
        return this.displaySource.placeholder();
    }

    @Override
    public void handleClick(ItemClick click) {
        // 守卫全部通过才执行处理器, 点击成功后按需主动失效
        if (!this.passes(this.clickGuards, click)) return;
        this.clickHandler.accept(this, click);
        if (this.updateOnClick) {
            this.notifyWindows();
        }
    }

    @Override
    public void handleDrag(ItemDragClick drag) {
        if (!this.passes(this.dragGuards, drag)) return;
        this.dragHandler.accept(this, drag);
    }

    @Override
    public void handleBundleSelect(@NotNull BundleSelectClick select) {
        if (!this.passes(this.bundleSelectGuards, select)) return;
        this.bundleHandler.accept(this, select);
    }

    // 按添加顺序执行守卫, 首个拒绝者执行善后回调并宣告不通过.
    private <C extends ItemInteraction> boolean passes(@NotNull List<GuardEntry<C>> guards, @NotNull C interaction) {
        for (int index = 0; index < guards.size(); index++) {
            GuardEntry<C> entry = guards.get(index);
            if (!entry.guard().test(this, interaction)) {
                entry.onRejected().accept(this, interaction);
                return false;
            }
        }
        return true;
    }

    // 登记观察者, 按查看者解析并订阅声明的依赖, 再触发显示来源的首次挂载回调.
    @Override
    public ItemAttachment attach(@NotNull Window window, @NotNull Observer<? super Item> observer) {
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(observer, "observer");
        ItemAttachment.Tracking attachment = ItemAttachment.tracking(this, observer);
        try {
            attachment.track(this.observers.subscribe(observer));
            attachment.subscribeDependencies(this.dependencies, window.viewer());
            this.displaySource.onAttached();
            return attachment;
        } catch (RuntimeException | Error throwable) {
            // 回滚失败不能盖掉挂载失败的原因
            try {
                attachment.close();
            } catch (RuntimeException | Error closeFailure) {
                throwable.addSuppressed(closeFailure);
            }
            throw throwable;
        }
    }

    @Override
    public void notifyWindows() {
        this.observers.publish(this);
    }

    record GuardEntry<C extends ItemInteraction>(
            @NotNull ItemGuard<? super C> guard,
            @NotNull BiConsumer<? super Item, ? super C> onRejected
    ) {
    }
}
