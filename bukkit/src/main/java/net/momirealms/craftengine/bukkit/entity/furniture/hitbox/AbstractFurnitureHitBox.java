package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import net.momirealms.craftengine.bukkit.entity.furniture.BukkitCollider;
import net.momirealms.craftengine.bukkit.entity.seat.BukkitSeat;
import net.momirealms.craftengine.core.entity.furniture.Collider;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.seat.Seat;
import net.momirealms.craftengine.core.entity.seat.SeatConfig;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.craftengine.proxy.minecraft.world.phys.AABBProxy;
import net.momirealms.sparrow.nbt.CompoundTag;

public abstract class AbstractFurnitureHitBox implements FurnitureHitBox {
    protected final Furniture furniture;
    protected Seat<FurnitureHitBox>[] seats;

    public AbstractFurnitureHitBox(Furniture furniture, FurnitureHitBoxConfig<?> config) {
        this.furniture = furniture;
        this.seats = createSeats(config);
    }

    @SuppressWarnings("unchecked")
    private Seat<FurnitureHitBox>[] createSeats(FurnitureHitBoxConfig<?> config) {
        SeatConfig[] seatConfigs = config.seats();
        Seat<FurnitureHitBox>[] seats = new Seat[seatConfigs.length];
        for (int i = 0; i < seatConfigs.length; i++) {
            seats[i] = new BukkitSeat<>(this, seatConfigs[i]);
        }
        return seats;
    }

    @Override
    public void saveEntityData(CompoundTag data) {
        data.putString("type", "furniture");
        // 用于通过座椅找到原始家具
        data.putInt("entity_id", this.furniture.entityId());
    }

    @Override
    public Seat<FurnitureHitBox>[] seats() {
        return this.seats;
    }

    protected Collider createCollider(World world, Vec3d position, AABB ceAABB, boolean canCollide, boolean blocksBuilding, boolean canBeHitByProjectile) {
        Object nmsAABB = AABBProxy.INSTANCE.newInstance(ceAABB.minX, ceAABB.minY, ceAABB.minZ, ceAABB.maxX, ceAABB.maxY, ceAABB.maxZ);
        return new BukkitCollider(world.serverWorld(), nmsAABB, position.x, position.y, position.z, canBeHitByProjectile, canCollide, blocksBuilding);
    }
}
