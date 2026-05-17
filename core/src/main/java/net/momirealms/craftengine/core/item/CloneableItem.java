package net.momirealms.craftengine.core.item;

import net.momirealms.craftengine.core.util.Key;

public final class CloneableItem implements BuildableItem {
    private final Item item;

    private CloneableItem(Item item) {
        this.item = item;
    }

    public static CloneableItem of(Item item) {
        return new CloneableItem(item);
    }

    @Override
    public Key id() {
        return this.item.id();
    }

    @Override
    public Item buildItem(ItemBuildContext context, int count) {
        return this.item.copyWithCount(count);
    }
}
