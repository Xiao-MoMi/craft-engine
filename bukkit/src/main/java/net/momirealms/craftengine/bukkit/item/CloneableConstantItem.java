package net.momirealms.craftengine.bukkit.item;

import net.momirealms.craftengine.core.item.BuildableItem;
import net.momirealms.craftengine.core.item.ItemBuildContext;
import org.bukkit.inventory.ItemStack;

public class CloneableConstantItem implements BuildableItem<ItemStack> {
    private final ItemStack item;

    public CloneableConstantItem(ItemStack item) {
        this.item = item;
    }

    @Override
    public ItemStack buildItemStack(ItemBuildContext context, int count) {
        ItemStack itemStack = this.item.clone();
        itemStack.setAmount(count);
        return itemStack;
    }
}
