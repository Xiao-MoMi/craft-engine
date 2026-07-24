package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import net.momirealms.craftengine.bukkit.entity.furniture.BukkitCollider;
import net.momirealms.craftengine.bukkit.entity.seat.BukkitSeat;
import net.momirealms.craftengine.core.entity.furniture.Collider;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.seat.Seat;
import net.momirealms.craftengine.core.entity.seat.SeatConfig;
import net.momirealms.craftengine.core.entity.seat.SeatOwner;
import net.momirealms.craftengine.core.world.Position;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.sparrow.nbt.CompoundTag;

public abstract class AbstractFurnitureHitBox implements FurnitureHitBox, SeatOwner {
    protected final Furniture furniture;
    protected Seat<SeatOwner>[] seats;

    public AbstractFurnitureHitBox(Furniture furniture, FurnitureHitBoxConfig<?> config) {
        this.furniture = furniture;
        this.seats = createSeats(config);
    }

    @SuppressWarnings("unchecked")
    private Seat<SeatOwner>[] createSeats(FurnitureHitBoxConfig<?> config) {
        SeatConfig[] seatConfigs = config.seats();
        Seat<SeatOwner>[] seats = new Seat[seatConfigs.length];
        for (int i = 0; i < seatConfigs.length; i++) {
            seats[i] = new BukkitSeat<>(this, seatConfigs[i]);
        }
        return seats;
    }

    @Override
    public void saveSeatEntityData(CompoundTag data) {
        data.putString("type", "furniture");
        // 用于通过座椅找到原始家具
        data.putInt("entity_id", this.furniture.entityId());
    }

    @Override
    public Seat<SeatOwner>[] seats() {
        return this.seats;
    }

    protected Collider createCollider(World world, Position position, AABB ceAABB, boolean canCollide, boolean blocksBuilding, boolean canBeHitByProjectile) {
       return BukkitCollider.create(world, position, ceAABB, canCollide, blocksBuilding, canBeHitByProjectile);
    }
}
