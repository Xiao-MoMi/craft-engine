package net.momirealms.craftengine.bukkit.entity.furniture;

import net.momirealms.craftengine.bukkit.nms.CollisionEntity;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.entity.furniture.Collider;
import net.momirealms.craftengine.core.entity.furniture.ColliderConfig;
import net.momirealms.craftengine.core.entity.furniture.ColliderProperties;
import net.momirealms.craftengine.core.entity.furniture.ColliderType;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.craftengine.proxy.minecraft.world.phys.AABBProxy;

public final class BukkitCollider implements Collider {
    private final CollisionEntity collisionEntity;

    public BukkitCollider(Object level, double x, double y, double z, ColliderConfig config) {
        AABB box = config.bounds;
        Object aabb = AABBProxy.INSTANCE.newInstance(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
        ColliderProperties flags = config.properties;
        this.collisionEntity = BukkitFurnitureManager.COLLISION_ENTITY_TYPE == ColliderType.INTERACTION ?
                FastNMS.INSTANCE.createCollisionInteraction(level, aabb, x, y, z, flags.canBeHitByProjectile, flags.canCollide, flags.blocksBuilding) :
                FastNMS.INSTANCE.createCollisionBoat(level, aabb, x, y, z, flags.canBeHitByProjectile, flags.canCollide, flags.blocksBuilding);
    }

    @Override
    public void destroy() {
        this.collisionEntity.destroy();
    }

    @Override
    public int entityId() {
        return this.collisionEntity.getEntityId();
    }

    @Override
    public Object handle() {
        return this.collisionEntity;
    }
}
