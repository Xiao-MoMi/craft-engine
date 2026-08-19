package net.momirealms.craftengine.core.plugin.ui.item.guard;

import net.momirealms.craftengine.core.plugin.ui.item.Item;
import net.momirealms.craftengine.core.plugin.ui.item.click.ItemInteraction;
import org.jetbrains.annotations.NotNull;

import java.util.function.BiPredicate;

@FunctionalInterface
public interface ItemGuard<C extends ItemInteraction> extends BiPredicate<Item, C> {

    @Override
    boolean test(@NotNull Item item, @NotNull C interaction);
}
