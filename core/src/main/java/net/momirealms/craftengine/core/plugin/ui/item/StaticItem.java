package net.momirealms.craftengine.core.plugin.ui.item;

import net.momirealms.craftengine.core.plugin.ui.item.click.BundleSelectClick;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemClick;
import net.momirealms.craftengine.core.plugin.ui.item.provider.ItemProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

public final class StaticItem implements Item {
    private final ItemProvider itemProvider;
    private final BiConsumer<Item, ItemClick> clickHandler;
    private final BiConsumer<Item, BundleSelectClick> bundleSelectHandler;

    public StaticItem(@NotNull net.momirealms.craftengine.core.item.Item template) {
        this(ItemProvider.constant(template), null);
    }

    public StaticItem(@NotNull ItemProvider itemProvider) {
        this(itemProvider, null);
    }

    public StaticItem(@NotNull ItemProvider itemProvider, @Nullable BiConsumer<Item, ItemClick> clickHandler) {
        this(itemProvider, clickHandler, null);
    }

    public StaticItem(
            @NotNull ItemProvider itemProvider,
            @Nullable BiConsumer<Item, ItemClick> clickHandler,
            @Nullable BiConsumer<Item, BundleSelectClick> bundleSelectHandler
    ) {
        this.itemProvider = itemProvider;
        this.clickHandler = clickHandler;
        this.bundleSelectHandler = bundleSelectHandler;
    }

    @Override
    @NotNull
    public ItemProvider getItemProvider() {
        return this.itemProvider;
    }

    @Override
    public void handleClick(@NotNull ItemClick click) {
        if (this.clickHandler != null) {
            this.clickHandler.accept(this, click);
        }
    }

    @Override
    public void handleBundleSelect(@NotNull BundleSelectClick select) {
        if (this.bundleSelectHandler != null) {
            this.bundleSelectHandler.accept(this, select);
        }
    }
}
