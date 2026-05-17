package net.momirealms.craftengine.bukkit.entity.projectile;

import net.momirealms.craftengine.core.entity.projectile.AbstractCustomProjectile;
import net.momirealms.craftengine.core.entity.projectile.ProjectileMeta;
import net.momirealms.craftengine.core.item.Item;
import org.bukkit.entity.Projectile;

public final class BukkitCustomProjectile extends AbstractCustomProjectile {

    public BukkitCustomProjectile(ProjectileMeta meta, Projectile projectile, Item projectileItem) {
        super(meta, new BukkitProjectile(projectile), projectileItem);
    }

    @Override
    public BukkitProjectile projectile() {
        return (BukkitProjectile) super.projectile();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Item item() {
        return (Item) item;
    }
}
