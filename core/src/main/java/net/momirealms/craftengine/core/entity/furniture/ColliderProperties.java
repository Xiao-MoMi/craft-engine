package net.momirealms.craftengine.core.entity.furniture;

public enum ColliderProperties {
    NONE(false, false, false),
    COLLISION(true, false, false),
    BLOCK_BUILDING(false, true, false),
    COLLISION_AND_BLOCK_BUILDING(true, true, false),
    PROJECTILE_HIT(false, false, true),
    COLLISION_AND_PROJECTILE_HIT(true, false, true),
    BLOCK_BUILDING_AND_PROJECTILE_HIT(false, true, true),
    ALL(true, true, true);

    // Bits: collision = 1, building = 2, projectile = 4.
    private static final ColliderProperties[] BY_FLAGS = values();

    public final boolean canCollide;
    public final boolean blocksBuilding;
    public final boolean canBeHitByProjectile;

    ColliderProperties(boolean canCollide, boolean blocksBuilding, boolean canBeHitByProjectile) {
        this.canCollide = canCollide;
        this.blocksBuilding = blocksBuilding;
        this.canBeHitByProjectile = canBeHitByProjectile;
    }

    public static ColliderProperties of(boolean canCollide, boolean blocksBuilding, boolean canBeHitByProjectile) {
        return BY_FLAGS[(canCollide ? 1 : 0) | (blocksBuilding ? 2 : 0) | (canBeHitByProjectile ? 4 : 0)];
    }
}
