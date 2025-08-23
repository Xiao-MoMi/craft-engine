package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.item.modifier.ItemDataModifier;

public interface ItemDataModifierFactory<I> {

    ItemDataModifier<I> create(Object arg);
}
