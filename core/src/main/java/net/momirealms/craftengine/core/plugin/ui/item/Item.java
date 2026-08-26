package net.momirealms.craftengine.core.plugin.ui.item;

import net.momirealms.craftengine.core.plugin.ui.item.provider.ImmediateItemProvider;
import net.momirealms.craftengine.core.plugin.ui.item.provider.ItemProvider;
import org.jetbrains.annotations.NotNull;

public interface Item {
    Item EMPTY = new EmptyItem();

    @NotNull
    static Item empty() {
        return EMPTY;
    }

    @NotNull
    static Item simple(@NotNull net.momirealms.craftengine.core.item.Item template) {
        return new StaticItem(ItemProvider.constant(template));
    }

    @NotNull
    static Item simple(@NotNull ItemProvider itemProvider) {
        return new StaticItem(itemProvider);
    }

    @NotNull
    ItemProvider getItemProvider();

    @NotNull
    default ImmediateItemProvider getPlaceholder() {
        return ItemProvider.EMPTY;
    }
}
