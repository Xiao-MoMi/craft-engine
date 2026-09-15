package net.momirealms.craftengine.bukkit.item;

import net.momirealms.craftengine.core.item.AbstractItem;
import net.momirealms.craftengine.core.item.ItemFactory;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.proxy.minecraft.world.item.ItemStackProxy;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof BukkitItem bukkitItem)) return false;
        return ItemStackProxy.INSTANCE.isSameItemSameComponents(bukkitItem.minecraftItem(), this.item.minecraftItem());
    }

    @Override
    public int hashCode() {
        if (VersionHelper.COMPONENT_RELEASE) {
            return ItemStackProxy.INSTANCE.hashItemAndComponents(this.item.minecraftItem());
        } else {
            Object item = this.item.minecraftItem();
            int i = 31 + ItemStackProxy.INSTANCE.getItem(item).hashCode();
            Object tag = ItemStackProxy.INSTANCE.getTag(item);
            if (tag == null) {
                return i;
            }
            return 31 + tag.hashCode();
        }
    }
}
