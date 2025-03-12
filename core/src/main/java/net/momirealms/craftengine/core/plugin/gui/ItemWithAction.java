package net.momirealms.craftengine.core.plugin.gui;

import net.momirealms.craftengine.core.item.Item;

import java.util.function.BiConsumer;

public record ItemWithAction(Item<?> item, BiConsumer<GuiElement.PageOrderedGuiElement, Click> action) {
    public static final ItemWithAction EMPTY = new ItemWithAction(null, (e, c) -> c.cancel());
}
