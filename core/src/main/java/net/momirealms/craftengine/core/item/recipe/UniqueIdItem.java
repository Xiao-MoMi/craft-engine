package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.UniqueKey;
import org.jetbrains.annotations.NotNull;

public class UniqueIdItem<T> {
    private final Item<T> rawItem;
    private final UniqueKey uniqueId;

    public UniqueIdItem(@NotNull UniqueKey uniqueId, @NotNull Item<T> rawItem) {
        this.uniqueId = uniqueId;
        this.rawItem = rawItem;
    }

    @NotNull
    public UniqueKey id() {
        return uniqueId;
    }

    @NotNull
    public Item<T> item() {
        return this.rawItem;
    }

    public boolean is(UniqueKey id) {
        return this.uniqueId == id;
    }

    public boolean isEmpty() {
        return this.uniqueId == UniqueKey.AIR;
    }

    @Override
    public String toString() {
        return "UniqueIdItem[" + "uniqueId=" + uniqueId + ", item=" + rawItem.getItem() + ']';
    }
}
