package net.momirealms.craftengine.bukkit.entity.projectile;

import net.momirealms.craftengine.bukkit.entity.BukkitEntity;
import net.momirealms.craftengine.core.entity.projectile.Projectile;

public class BukkitProjectile extends BukkitEntity implements Projectile {

    public BukkitProjectile(org.bukkit.entity.Projectile entity) {
        super(entity);
    }
}
