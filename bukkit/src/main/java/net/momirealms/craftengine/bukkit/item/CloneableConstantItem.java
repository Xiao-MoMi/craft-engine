package net.momirealms.craftengine.bukkit.item;

import net.momirealms.craftengine.core.item.BuildableItem;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import net.momirealms.craftengine.core.util.Key;
import org.bukkit.inventory.ItemStack;

public class CloneableConstantItem implements BuildableItem<ItemStack> {
    private final ItemStack item;
    private final Key id;

    public CloneableConstantItem(Key id, ItemStack item) {
        this.item = item;
        this.id = id;
    }

    @Override
    public Key id() {
        return this.id;
    }

    @Override
    public ItemStack buildItemStack(ItemBuildContext context, int count) {
        ItemStack itemStack = this.item.clone();
        itemStack.setAmount(count);
        return itemStack;
    }
}
