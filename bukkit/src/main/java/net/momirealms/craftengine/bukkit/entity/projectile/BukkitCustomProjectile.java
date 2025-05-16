package net.momirealms.craftengine.bukkit.entity.projectile;

import net.momirealms.craftengine.core.entity.projectile.AbstractCustomProjectile;
import net.momirealms.craftengine.core.entity.projectile.ProjectileMeta;
import net.momirealms.craftengine.core.item.Item;
import org.bukkit.entity.Projectile;
import org.bukkit.inventory.ItemStack;

public class BukkitCustomProjectile extends AbstractCustomProjectile {

    public BukkitCustomProjectile(ProjectileMeta meta, Projectile projectile, Item<ItemStack> projectileItem) {
        super(meta, new BukkitProjectile(projectile), projectileItem);
    }
}
