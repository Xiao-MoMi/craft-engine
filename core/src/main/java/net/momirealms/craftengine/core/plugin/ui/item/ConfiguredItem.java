package net.momirealms.craftengine.core.plugin.ui.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.ui.ObservableDispatcher;
import net.momirealms.craftengine.core.plugin.ui.Observer;
import net.momirealms.craftengine.core.plugin.ui.item.click.BundleSelectClick;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemClick;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemDrag;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemInteraction;
import net.momirealms.craftengine.core.plugin.ui.item.guard.ItemGuard;
import net.momirealms.craftengine.core.plugin.ui.item.provider.ImmediateItemProvider;
import net.momirealms.craftengine.core.plugin.ui.item.provider.ItemProvider;
import net.momirealms.craftengine.core.plugin.ui.state.Signal;
import net.momirealms.craftengine.core.plugin.ui.window.Window;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

final class ConfiguredItem implements ObservableItem {
    private final ItemBuilder.DisplaySource displaySource;
    private final List<Function<? super Player, ? extends Signal<?>>> dependencies;
    private final ObservableDispatcher<Item> observers = new ObservableDispatcher<>();

    private final List<GuardEntry<ItemClick>> clickGuards;
    private final List<GuardEntry<ItemDrag>> dragGuards;
    private final List<GuardEntry<BundleSelectClick>> bundleSelectGuards;

    private final BiConsumer<? super Item, ? super ItemClick> clickHandler;
    private final BiConsumer<? super Item, ? super ItemDrag> dragHandler;
    private final BiConsumer<? super Item, ? super BundleSelectClick> bundleHandler;
    private final boolean updateOnClick;

    ConfiguredItem(
            @NotNull ItemBuilder.DisplaySourceFactory source,
            @NotNull List<? extends Function<? super Player, ? extends Signal<?>>> dependencies,
            @NotNull List<? extends GuardEntry<ItemClick>> clickGuards,
            @NotNull List<? extends GuardEntry<ItemDrag>> dragGuards,
            @NotNull List<? extends GuardEntry<BundleSelectClick>> bundleSelectGuards,
            @NotNull BiConsumer<? super Item, ? super ItemClick> clickHandler,
            @NotNull BiConsumer<? super Item, ? super ItemDrag> dragHandler,
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

    @Override
    @NotNull
    public ItemProvider getItemProvider() {
        return this.displaySource.provider();
    }

    @Override
    @NotNull
    public ImmediateItemProvider getPlaceholder() {
        return this.displaySource.placeholder();
    }

    @Override
    public void handleClick(@NotNull ItemClick click) {
        if (!this.passes(this.clickGuards, click)) return;
        this.clickHandler.accept(this, click);
        if (this.updateOnClick) {
            this.notifyWindows();
        }
    }

    @Override
    public void handleDrag(@NotNull ItemDrag drag) {
        if (!this.passes(this.dragGuards, drag)) return;
        this.dragHandler.accept(this, drag);
    }

    @Override
    public void handleBundleSelect(@NotNull BundleSelectClick select) {
        if (!this.passes(this.bundleSelectGuards, select)) return;
        this.bundleHandler.accept(this, select);
    }

    private <C extends ItemInteraction> boolean passes(@NotNull List<GuardEntry<C>> guards, @NotNull C interaction) {
        // 第一个拒绝交互的守卫执行自己的拒绝回调并结束处理.
        for (int index = 0; index < guards.size(); index++) {
            GuardEntry<C> entry = guards.get(index);
            if (!entry.guard().test(this, interaction)) {
                entry.onRejected().accept(this, interaction);
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemAttachment attach(@NotNull Window window, @NotNull Observer<? super Item> observer) {
        Objects.requireNonNull(window, "window");
        Objects.requireNonNull(observer, "observer");
        ItemAttachment.Tracking attachment = ItemAttachment.tracking(this, observer);
        // 先建立失效订阅, 同步完成的 lazy 来源随后发布时不会漏掉通知.
        try {
            attachment.track(this.observers.subscribe(observer));
            attachment.subscribeDependencies(this.dependencies, window.viewer());
            this.displaySource.onAttached();
            return attachment;
        } catch (RuntimeException | Error throwable) {
            try {
                attachment.close();
            } catch (RuntimeException | Error closeFailure) {
                // 原始挂载异常仍是主异常, 清理失败作为补充信息.
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
