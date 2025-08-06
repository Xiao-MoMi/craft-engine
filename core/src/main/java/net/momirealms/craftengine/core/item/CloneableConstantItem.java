package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.util.Key;

public class CloneableConstantItem<I> implements BuildableItem<I> {
    private final Item<I> item;

    private CloneableConstantItem(Item<I> item) {
        this.item = item;
    }

    public static <I> CloneableConstantItem<I> of(Item<I> item) {
        return new CloneableConstantItem<>(item);
    }

    @Override
    public Key id() {
        return this.item.id();
    }

    @Override
    public Item<I> buildItem(ItemBuildContext context, int count) {
        return this.item.copyWithCount(count);
    }

    @Override
    public I buildItemStack(ItemBuildContext context, int count) {
        return this.item.copyWithCount(count).getItem();
    }
}
