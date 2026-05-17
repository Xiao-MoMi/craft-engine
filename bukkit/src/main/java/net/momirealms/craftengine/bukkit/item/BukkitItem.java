package net.momirealms.craftengine.bukkit.item;

import net.momirealms.craftengine.core.item.AbstractItem;
import net.momirealms.craftengine.core.item.ItemFactory;
import org.bukkit.inventory.ItemStack;

public final class BukkitItem extends AbstractItem<BukkitItemWrapper> {

    public BukkitItem(ItemFactory<BukkitItemWrapper> factory, BukkitItemWrapper item) {
        super(factory, item);
    }

    @Override
    protected AbstractItem<BukkitItemWrapper> withSameFactory(BukkitItemWrapper item) {
        return new BukkitItem(super.factory, item);
    }

    public ItemStack getBukkitItem() {
        return super.item.platformItem();
    }
}
