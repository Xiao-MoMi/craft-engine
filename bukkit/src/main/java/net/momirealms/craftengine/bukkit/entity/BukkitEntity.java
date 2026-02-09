package net.momirealms.craftengine.bukkit.entity;

import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.DirectionUtils;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.AbstractEntity;
import net.momirealms.craftengine.core.entity.data.EntityData;
import net.momirealms.craftengine.core.util.Direction;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.proxy.minecraft.network.syncher.SynchedEntityDataProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import org.bukkit.entity.Entity;

import java.lang.ref.WeakReference;
import java.util.UUID;

public class BukkitEntity extends AbstractEntity {
    protected final WeakReference<org.bukkit.entity.Entity> entity;

    public BukkitEntity(org.bukkit.entity.Entity entity) {
        this.entity = new WeakReference<>(entity);
    }

    @Override
    public double x() {
        return platformEntity().getX();
    }

    @Override
    public double y() {
        return platformEntity().getY();
    }

    @Override
    public double z() {
        return platformEntity().getZ();
    }

    @Override
    public void tick() {
    }

    @Override
    public WorldPosition position() {
        return LocationUtils.toWorldPosition(platformEntity().getLocation());
    }

    @Override
    public int entityId() {
        return platformEntity().getEntityId();
    }

    @Override
    public float xRot() {
        return platformEntity().getYaw();
    }

    @Override
    public float yRot() {
        return platformEntity().getPitch();
    }

    @Override
    public World world() {
        return BukkitAdaptors.adapt(platformEntity().getWorld());
    }

    @Override
    public Direction getDirection() {
        return DirectionUtils.toDirection(platformEntity().getFacing());
    }

    @Override
    public org.bukkit.entity.Entity platformEntity() {
        return this.entity.get();
    }

    @Override
    public Object serverEntity() {
        return FastNMS.INSTANCE.method$CraftEntity$getHandle(platformEntity());
    }

    @Override
    public Key type() {
        return EntityUtils.getEntityType(platformEntity());
    }

    @Override
    public boolean isValid() {
        Entity bkEntity = platformEntity();
        if (bkEntity == null) return false;
        return bkEntity.isValid();
    }

    @Override
    public String name() {
        return platformEntity().getName();
    }

    @Override
    public UUID uuid() {
        return platformEntity().getUniqueId();
    }

    @Override
    public Object entityData() {
        return EntityProxy.INSTANCE.getEntityData(serverEntity());
    }

    @Override
    public <T> T getEntityData(EntityData<T> data) {
        return SynchedEntityDataProxy.INSTANCE.get(entityData(), data.entityDataAccessor());
    }

    @Override
    public <T> void setEntityData(EntityData<T> data, T value, boolean force) {
        SynchedEntityDataProxy.INSTANCE.set(entityData(), data.entityDataAccessor(), value, force);
    }

    @Override
    public void remove() {
        this.platformEntity().remove();
    }
}
