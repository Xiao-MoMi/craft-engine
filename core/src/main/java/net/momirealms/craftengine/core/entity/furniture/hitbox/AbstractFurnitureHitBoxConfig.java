package net.momirealms.craftengine.core.entity.furniture.hitbox;

import net.momirealms.craftengine.core.entity.seat.SeatConfig;
import org.joml.Vector3f;

public abstract class AbstractFurnitureHitBoxConfig<H extends FurnitureHitBox> implements FurnitureHitBoxConfig<H> {
    public final SeatConfig[] seats;
    public final Vector3f position;
    public final boolean canUseItemOn;
    public final boolean blocksBuilding;
    public final boolean canBeHitByProjectile;

    public AbstractFurnitureHitBoxConfig(SeatConfig[] seats,
                                         Vector3f position,
                                         boolean canUseItemOn,
                                         boolean blocksBuilding,
                                         boolean canBeHitByProjectile) {
        this.seats = seats;
        this.position = position;
        this.canUseItemOn = canUseItemOn;
        this.blocksBuilding = blocksBuilding;
        this.canBeHitByProjectile = canBeHitByProjectile;
    }

    @Override
    public SeatConfig[] seats() {
        return this.seats;
    }

    @Override
    public Vector3f position() {
        return this.position;
    }

    @Override
    public boolean blocksBuilding() {
        return this.blocksBuilding;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return this.canBeHitByProjectile;
    }

    @Override
    public boolean canUseItemOn() {
        return this.canUseItemOn;
    }
}
