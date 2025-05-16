package net.momirealms.craftengine.core.entity.projectile;

import net.momirealms.craftengine.core.item.Item;

public interface CustomProjectile {

    ProjectileMeta metadata();

    Projectile projectile();

    Item<?> item();
}
