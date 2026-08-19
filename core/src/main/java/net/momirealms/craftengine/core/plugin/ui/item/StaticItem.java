package net.momirealms.craftengine.core.plugin.ui.item;

import net.momirealms.craftengine.core.plugin.ui.item.click.BundleSelectClick;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemClick;
import net.momirealms.craftengine.core.plugin.ui.item.provider.ItemProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.util.Objects;
import java.util.function.BiConsumer;

public final class StaticItem implements Item {
    private final ItemProvider itemProvider;
    private final BiConsumer<? super Item, ? super ItemClick> clickHandler;                 // null 表示不处理点击
    private final BiConsumer<? super Item, ? super BundleSelectClick> bundleSelectHandler;  // null 表示不处理 Bundle 选择

    public StaticItem(@NotNull net.momirealms.craftengine.core.item.Item itemStack) {
        this(ItemProvider.constant(itemStack), null);
    }

    public StaticItem(@NotNull ItemProvider itemProvider) {
        this(itemProvider, null);
    }

    public StaticItem(
            @NotNull ItemProvider itemProvider,
            @Nullable BiConsumer<? super Item, ? super ItemClick> clickHandler
    ) {
        this(itemProvider, clickHandler, null);
    }

    public StaticItem(
            @NotNull ItemProvider itemProvider,
            @Nullable BiConsumer<? super Item, ? super ItemClick> clickHandler,
            @Nullable BiConsumer<? super Item, ? super BundleSelectClick> bundleSelectHandler
    ) {
        this.itemProvider = Objects.requireNonNull(itemProvider, "itemProvider");
        this.clickHandler = clickHandler;
        this.bundleSelectHandler = bundleSelectHandler;
    }

    @NonNull
    @Override
    public ItemProvider getItemProvider() {
        return this.itemProvider;
    }

    @Override
    public void handleClick(ItemClick click) {
        if (this.clickHandler != null) {
            this.clickHandler.accept(this, Objects.requireNonNull(click, "click"));
        }
    }

    @Override
    public void handleBundleSelect(BundleSelectClick select) {
        if (this.bundleSelectHandler != null) {
            this.bundleSelectHandler.accept(this, Objects.requireNonNull(select, "select"));
        }
    }
}
