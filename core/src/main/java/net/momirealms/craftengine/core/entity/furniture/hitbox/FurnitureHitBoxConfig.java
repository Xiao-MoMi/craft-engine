package net.momirealms.craftengine.core.entity.furniture.hitbox;

import net.momirealms.craftengine.core.entity.furniture.ColliderProperties;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.seat.SeatConfig;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import org.joml.Vector3f;

import java.util.function.Consumer;

public interface FurnitureHitBoxConfig<H extends FurnitureHitBox> {

    H create(Furniture furniture);

    SeatConfig[] seats();

    Vector3f position();

    boolean blocksBuilding();

    boolean canBeHitByProjectile();

    boolean canUseItemOn();

    /** Opts a single, static collider into configuration-time optimization. */
    default ColliderProperties colliderProperties() {
        return null;
    }

    void prepareBoundingBox(WorldPosition targetPos, Consumer<AABB> aabbConsumer, boolean ignoreBlocksBuilding);
}
