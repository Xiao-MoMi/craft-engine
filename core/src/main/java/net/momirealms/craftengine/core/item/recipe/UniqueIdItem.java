package net.momirealms.craftengine.core.item.recipe;

import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.UniqueKey;
import org.jetbrains.annotations.NotNull;

public final class UniqueIdItem {
    private final Item rawItem;
    private final UniqueKey uniqueId;

    private UniqueIdItem(@NotNull Item rawItem) {
        this.rawItem = rawItem;
        this.uniqueId = CraftEngine.instance().itemManager().getIngredientKey(rawItem);
    }

    public static UniqueIdItem of(Item rawItem) {
        return new UniqueIdItem(rawItem);
    }

    @NotNull
    public UniqueKey id() {
        return this.uniqueId;
    }

    @NotNull
    public Item item() {
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
        return "UniqueIdItem[" + "uniqueId=" + this.uniqueId + ", item=" + this.rawItem.minecraftItem() + ']';
    }
}
