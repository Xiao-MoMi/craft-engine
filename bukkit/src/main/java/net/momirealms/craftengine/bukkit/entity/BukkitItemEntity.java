package net.momirealms.craftengine.bukkit.entity;

import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.core.entity.item.ItemEntity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

public class BukkitItemEntity extends BukkitEntity implements ItemEntity {

    public BukkitItemEntity(Object entity) {
        super(entity);
    }

    @Override
    public net.momirealms.craftengine.core.item.Item getItem() {
        return BukkitItemManager.instance().wrap(platformEntity().getItemStack());
    }

    @Override
    public void setItem(net.momirealms.craftengine.core.item.Item item) {
        platformEntity().setItemStack((ItemStack) item.platformItem());
    }

    @Override
    public Item platformEntity() {
        return (Item) super.platformEntity();
    }
}
