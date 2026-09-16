package net.momirealms.craftengine.bukkit.entity.furniture.hitbox;

import net.momirealms.craftengine.bukkit.entity.seat.BukkitSeat;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.seat.Seat;
import net.momirealms.craftengine.core.entity.seat.SeatConfig;
import net.momirealms.craftengine.core.entity.seat.SeatOwner;
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
        if (seatConfigs.length == 0) return EMPTY_SEATS;
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

    public abstract FurnitureHitBoxConfig<?> config();

    @Override
    public boolean canUseItemOn() {
        return config().canUseItemOn();
    }

    @Override
    public Seat<SeatOwner>[] seats() {
        return this.seats;
    }
}
