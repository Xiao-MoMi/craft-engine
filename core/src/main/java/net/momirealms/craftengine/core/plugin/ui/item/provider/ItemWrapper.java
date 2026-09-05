package net.momirealms.craftengine.core.plugin.ui.item.provider;

import net.momirealms.craftengine.core.item.Item;
import org.jetbrains.annotations.NotNull;

final class ItemWrapper implements ImmediateItemProvider {
    private final Item template;

    ItemWrapper(@NotNull Item template) {
        this.template = template.copy();
    }

    @Override
    @NotNull
    public Item provideImmediately(@NotNull RenderContext context) {
        return this.template;
    }
}
