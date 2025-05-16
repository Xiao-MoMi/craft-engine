package net.momirealms.craftengine.core.entity.projectile;

import net.momirealms.craftengine.core.item.Item;

public abstract class AbstractCustomProjectile implements CustomProjectile {
    protected final ProjectileMeta meta;
    protected final Projectile projectile;
    protected final Item<?> item;

    protected AbstractCustomProjectile(ProjectileMeta meta, Projectile projectile, Item<?> item) {
        this.meta = meta;
        this.projectile = projectile;
        this.item = item;
    }

    @Override
    public ProjectileMeta metadata() {
        return this.meta;
    }

    @Override
    public Projectile projectile() {
        return projectile;
    }

    @Override
    public Item<?> item() {
        return item;
    }
}
