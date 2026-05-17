package net.momirealms.craftengine.bukkit.entity.furniture;

import net.momirealms.craftengine.bukkit.nms.CollisionEntity;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.core.entity.furniture.Collider;
import net.momirealms.craftengine.core.entity.furniture.ColliderType;
import net.momirealms.craftengine.core.world.Position;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.craftengine.proxy.minecraft.world.phys.AABBProxy;

public final class BukkitCollider implements Collider {
    private final CollisionEntity collisionEntity;

    public BukkitCollider(Object world, Object aabb, double x, double y, double z, boolean canProjectileHit, boolean canCollide, boolean blocksBuilding) {
        this.collisionEntity = BukkitFurnitureManager.COLLISION_ENTITY_TYPE == ColliderType.INTERACTION ?
                FastNMS.INSTANCE.createCollisionInteraction(world, aabb, x, y, z, canProjectileHit, canCollide, blocksBuilding) :
                FastNMS.INSTANCE.createCollisionBoat(world, aabb, x, y, z, canProjectileHit, canCollide, blocksBuilding);
    }

    public static Collider create(World world, Position position, AABB aabb, boolean canCollide, boolean blocksBuilding, boolean canBeHitByProjectile) {
        Object nmsAABB = AABBProxy.INSTANCE.newInstance(aabb.minX, aabb.minY, aabb.minZ, aabb.maxX, aabb.maxY, aabb.maxZ);
        return new BukkitCollider(world.minecraftWorld(), nmsAABB, position.x(), position.y(), position.z(), canBeHitByProjectile, canCollide, blocksBuilding);
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
