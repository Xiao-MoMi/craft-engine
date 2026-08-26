package net.momirealms.craftengine.core.plugin.ui.item;

import net.momirealms.craftengine.core.plugin.ui.item.provider.ItemProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class StaticItem implements Item {
    private final ItemProvider itemProvider;

    public StaticItem(@NotNull net.momirealms.craftengine.core.item.Item template) {
        this(ItemProvider.constant(template));
    }

    public StaticItem(@NotNull ItemProvider itemProvider) {
        this.itemProvider = Objects.requireNonNull(itemProvider, "itemProvider");
    }

    @Override
    @NotNull
    public ItemProvider getItemProvider() {
        return this.itemProvider;
    }
}
