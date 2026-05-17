package net.momirealms.craftengine.bukkit.entity.seat;

import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.util.LegacyAttributeUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.seat.Seat;
import net.momirealms.craftengine.core.entity.seat.SeatConfig;
import net.momirealms.craftengine.core.entity.seat.SeatOwner;
import net.momirealms.craftengine.core.util.QuaternionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.sparrow.nbt.CompoundTag;
import net.momirealms.sparrow.nbt.NBT;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Objects;

public final class BukkitSeat<O extends SeatOwner> implements Seat<O> {
    private final O owner;
    private final SeatConfig seatConfig;
    private WeakReference<Entity> entity;

    public BukkitSeat(O owner, SeatConfig config) {
        this.owner = owner;
        this.seatConfig = config;
    }

    @Override
    public O owner() {
        return this.owner;
    }

    @Override
    public SeatConfig config() {
        return this.seatConfig;
    }

    @Nullable
    public Entity getSeatEntity() {
        return this.entity == null ? null : this.entity.get();
    }

    @Override
    public boolean isOccupied() {
        Entity seatEntity = getSeatEntity();
        return seatEntity != null && seatEntity.isValid() && !seatEntity.getPassengers().isEmpty();
    }

    @Override
    public void destroy() {
        if (this.entity != null) {
            Entity entity = this.entity.get();
            if (entity != null) {
                if (entity.isValid()) {
                    entity.remove();
                }
                this.entity = null;
            }
        }
    }

    private float yRot() {
        return this.seatConfig.yRot();
    }

    private Vector3f position() {
        return this.seatConfig.position();
    }

    private boolean limitPlayerRotation() {
        return this.seatConfig.limitPlayerRotation();
    }

    private Location calculateSeatLocation(Location sourceLocation) {
        Vector3f offset = QuaternionUtils.toQuaternionf(0, Math.toRadians(180 - sourceLocation.getYaw()), 0).conjugate().transform(new Vector3f(this.position()));
        double yaw = this.yRot() + sourceLocation.getYaw();
        if (yaw < -180) yaw += 360;
        Location newLocation = sourceLocation.clone();
        newLocation.setYaw((float) yaw);
        newLocation.add(offset.x, offset.y + 0.6, -offset.z);
        return newLocation;
    }

    @Override
    public boolean spawnSeat(net.momirealms.craftengine.core.entity.player.Player player, WorldPosition source) {
        return spawnSeatEntityForPlayer((Player) player.platformPlayer(), LocationUtils.toLocation(source));
    }

    private boolean spawnSeatEntityForPlayer(Player player, Location sourceLocation) {
        // 移除就有的座椅
        this.destroy();
        // 计算座椅的位置
        Location location = this.calculateSeatLocation(sourceLocation);

        CompoundTag extraData = new CompoundTag();
        this.owner.saveSeatEntityData(extraData);
        byte[] data;
        try {
            data = NBT.toBytes(extraData);
        } catch (IOException e) {
            return false;
        }

        // 生成座椅实体
        Entity seatEntity = this.limitPlayerRotation() ?
                EntityUtils.spawnEntity(player.getWorld(), VersionHelper.isOrAbove1_20_2 ? location.subtract(0,0.9875,0) : location.subtract(0,0.990625,0), EntityType.ARMOR_STAND, entity -> {
                    ArmorStand armorStand = (ArmorStand) entity;
                    if (VersionHelper.isOrAbove1_21_3) {
                        Objects.requireNonNull(armorStand.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(0.01);
                    } else {
                        LegacyAttributeUtils.setMaxHealth(armorStand);
                    }
                    armorStand.setSmall(true);
                    armorStand.setInvisible(true);
                    armorStand.setSilent(true);
                    armorStand.setInvulnerable(true);
                    armorStand.setArms(false);
                    armorStand.setCanTick(false);
                    armorStand.setAI(false);
                    armorStand.setGravity(false);
                    armorStand.setPersistent(false);
                    armorStand.getPersistentDataContainer().set(BukkitSeatManager.SEAT_KEY, PersistentDataType.BOOLEAN, true);
                    armorStand.getPersistentDataContainer().set(BukkitSeatManager.SEAT_EXTRA_DATA_KEY, PersistentDataType.BYTE_ARRAY, data);
                }) :
                EntityUtils.spawnEntity(player.getWorld(), VersionHelper.isOrAbove1_20_2 ? location : location.subtract(0,0.25,0), EntityType.ITEM_DISPLAY, entity -> {
                    ItemDisplay itemDisplay = (ItemDisplay) entity;
                    itemDisplay.setPersistent(false);
                    itemDisplay.getPersistentDataContainer().set(BukkitSeatManager.SEAT_KEY, PersistentDataType.BOOLEAN, true);
                    itemDisplay.getPersistentDataContainer().set(BukkitSeatManager.SEAT_EXTRA_DATA_KEY, PersistentDataType.BYTE_ARRAY, data);
                });
        if (!seatEntity.addPassenger(player)) {
            seatEntity.remove();
            return false;
        } else {
            this.entity = new WeakReference<>(seatEntity);
            return true;
        }
    }
}
