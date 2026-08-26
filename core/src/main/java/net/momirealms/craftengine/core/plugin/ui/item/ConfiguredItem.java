package net.momirealms.craftengine.core.plugin.ui.item;

import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.ui.ObservableDispatcher;
import net.momirealms.craftengine.core.plugin.ui.Observer;
import net.momirealms.craftengine.core.plugin.ui.item.click.BundleSelectClick;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemClick;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemDrag;
import net.momirealms.craftengine.core.plugin.ui.item.guard.ItemGuard;
import net.momirealms.craftengine.core.plugin.ui.item.provider.ImmediateItemProvider;
import net.momirealms.craftengine.core.plugin.ui.item.provider.ItemProvider;
import net.momirealms.craftengine.core.plugin.ui.state.Signal;
import net.momirealms.craftengine.core.plugin.ui.window.Window;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

final class ConfiguredItem implements ObservableItem {
    private final ItemBuilder.DisplaySource displaySource;
    private final List<Function<Player, Signal<?>>> dependencies;
    private final ObservableDispatcher<Item> observers = new ObservableDispatcher<>();

    @Nullable private final ItemGuard<ItemClick> clickGuard;
    @Nullable private final ItemGuard<ItemDrag> dragGuard;
    @Nullable private final ItemGuard<BundleSelectClick> bundleSelectGuard;

    @Nullable private final BiConsumer<Item, ItemClick> clickHandler;
    @Nullable private final BiConsumer<Item, ItemDrag> dragHandler;
    @Nullable private final BiConsumer<Item, BundleSelectClick> bundleHandler;
    private final boolean updateOnClick;

    ConfiguredItem(
            @NotNull ItemBuilder.DisplaySourceFactory source,
            @NotNull List<Function<Player, Signal<?>>> dependencies,
            @Nullable ItemGuard<ItemClick> clickGuard,
            @Nullable ItemGuard<ItemDrag> dragGuard,
            @Nullable ItemGuard<BundleSelectClick> bundleSelectGuard,
            @Nullable BiConsumer<Item, ItemClick> clickHandler,
            @Nullable BiConsumer<Item, ItemDrag> dragHandler,
            @Nullable BiConsumer<Item, BundleSelectClick> bundleHandler,
            boolean updateOnClick
    ) {
        this.displaySource = source.create(this::notifyWindows);
        this.dependencies = List.copyOf(dependencies);
        this.clickGuard = clickGuard;
        this.dragGuard = dragGuard;
        this.bundleSelectGuard = bundleSelectGuard;
        this.clickHandler = clickHandler;
        this.dragHandler = dragHandler;
        this.bundleHandler = bundleHandler;
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
        if (this.clickGuard != null && !this.clickGuard.test(this, click)) return;
        if (this.clickHandler != null) {
            this.clickHandler.accept(this, click);
        }
        if (this.updateOnClick) {
            this.notifyWindows();
        }
    }

    @Override
    public void handleDrag(@NotNull ItemDrag drag) {
        if (this.dragGuard != null && !this.dragGuard.test(this, drag)) return;
        if (this.dragHandler != null) {
            this.dragHandler.accept(this, drag);
        }
    }

    @Override
    public void handleBundleSelect(@NotNull BundleSelectClick select) {
        if (this.bundleSelectGuard != null && !this.bundleSelectGuard.test(this, select)) return;
        if (this.bundleHandler != null) {
            this.bundleHandler.accept(this, select);
        }
    }

    @Override
    public ItemAttachment attach(@NotNull Window window, @NotNull Observer<? super Item> observer) {
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
}
