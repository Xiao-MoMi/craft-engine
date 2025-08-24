package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.util.UniqueKey;
import org.jetbrains.annotations.NotNull;

public class UniqueIdItem<T> {
    private final Item<T> rawItem;
    private final UniqueKey uniqueId;

    private UniqueIdItem(@NotNull Item<T> rawItem) {
        this.rawItem = rawItem;
        this.uniqueId = rawItem.recipeIngredientId();
    }

    public static <T> UniqueIdItem<T> of(Item<T> rawItem) {
        return new UniqueIdItem<>(rawItem);
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
        return this.uniqueId == null;
    }

    @Override
    public String toString() {
        return "UniqueIdItem[" + "uniqueId=" + uniqueId + ", item=" + rawItem.getItem() + ']';
    }
}
