package net.momirealms.craftengine.bukkit.item;

import net.momirealms.craftengine.core.item.BuildableItem;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.inventory.ItemStack;

public class CloneableConstantItem implements BuildableItem<ItemStack> {
    private final Item<ItemStack> item;
    private final Key id;

    public CloneableConstantItem(Key id, Item<ItemStack> item) {
        this.item = item;
        this.id = id;
    }

    @Override
    public Key id() {
        return this.id;
    }

    @Override
    public Item<ItemStack> buildItem(ItemBuildContext context, int count) {
        return this.item.copyWithCount(count);
    }

    @Override
    public ItemStack buildItemStack(ItemBuildContext context, int count) {
        return this.item.copyWithCount(count).getItem();
    }
}
