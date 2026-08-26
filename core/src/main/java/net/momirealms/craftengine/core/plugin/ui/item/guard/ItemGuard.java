package net.momirealms.craftengine.core.plugin.ui.item.guard;

import net.momirealms.craftengine.core.plugin.ui.item.Item;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemInteraction;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiPredicate;

@FunctionalInterface
public interface ItemGuard<C extends ItemInteraction> extends BiPredicate<Item, C> {

    /**
     * 判断当前 Item 是否接受本次交互.
     *
     * @param item 被交互的 Item
     * @param interaction 交互上下文
     * @return 接受时为 {@code true}
     */
    @Override
    boolean test(@NotNull Item item, @NotNull C interaction);
}
