package net.momirealms.craftengine.bukkit.entity;

import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.core.entity.item.ItemEntity;
import org.bukkit.entity.Item;

public class BukkitItemEntity extends BukkitEntity implements ItemEntity {

    public BukkitItemEntity(Item entity) {
        super(entity);
    }

    @Override
    public net.momirealms.craftengine.core.item.Item getItem() {
        return BukkitItemManager.instance().wrap(((Item) platformEntity()).getItemStack());
    }
}
