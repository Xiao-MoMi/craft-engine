package net.momirealms.craftengine.core.plugin.ui.item;

import net.momirealms.craftengine.core.plugin.ui.item.provider.ItemProvider;
import org.jetbrains.annotations.NotNull;

final class EmptyItem implements Item {

    @NotNull
    @Override
    public ItemProvider getItemProvider() {
        return ItemProvider.EMPTY;
    }
}
