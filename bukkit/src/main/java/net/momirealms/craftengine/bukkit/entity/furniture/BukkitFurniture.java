package net.momirealms.craftengine.bukkit.entity.furniture;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.entity.BukkitEntity;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.CollisionUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.furniture.*;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.CustomDataType;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.QuaternionUtils;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.entity.CraftEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypeProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.AABBProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("DuplicatedCode")
public final class BukkitFurniture extends Furniture {
    private final AtomicBoolean isMoving = new AtomicBoolean(false);
    private final WeakReference<ItemDisplay> metaEntity;
    private Location location;

    public BukkitFurniture(ItemDisplay metaEntity, FurnitureDefinition config, FurniturePersistentData data) {
        super(new BukkitEntity(metaEntity), data, config);
        this.metaEntity = new WeakReference<>(metaEntity);
        this.location = metaEntity.getLocation();
    }

    @Override
    protected FurnitureSnapshotState createSnapshot(List<FurnitureElement> elements,
                                                             List<FurnitureHitBox> hitboxes,
                                                             Int2ObjectMap<FurnitureHitBox> hitboxMap,
                                                             List<Collider> colliders,
                                                             Map<CustomDataType<?>, Object> customData) {
        return new BukkitVariantSnapshot(elements, hitboxes, hitboxMap, colliders, customData);
    }

    @Override
    public boolean setVariant(String variantName, boolean force) {
        FurnitureVariant variant = this.config.getVariant(variantName);
        if (variant == null) return false;
        if (this.currentVariant == variant) return false;
        // 检查新位置是否可用
        if (!force) {
            List<AABB> aabbs = new ArrayList<>();
            WorldPosition position = position();
            for (FurnitureHitBoxConfig<?> hitBoxConfig : variant.hitBoxConfigs()) {
                hitBoxConfig.prepareBoundingBox(position, aabbs::add, false);
            }
            if (!aabbs.isEmpty()) {
                if (!CollisionUtils.test(position.world.minecraftWorld(), aabbs.stream().map(it -> AABBProxy.INSTANCE.newInstance(it.minX, it.minY, it.minZ, it.maxX, it.maxY, it.maxZ)).toList(),
                        o -> {
                            for (Collider collider : super.snapshot.colliders()) {
                                if (o == collider.handle()) {
                                    return false;
                                }
                            }
                            return true;
                        })) {
                    return false;
                }
            }
        }

        // 先移除
        {
            BukkitFurnitureManager.instance().invalidateFurniture(this, false);
            super.destroySeats();
            super.clearColliders();
        }

        super.setVariantInternal(variant);

        // 后展示
        {
            BukkitFurnitureManager.instance().initFurniture(this);
            this.addCollidersToWorld();
            this.refresh();
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public CompletableFuture<Boolean> moveTo(WorldPosition position, boolean force) {
        // 加锁
        if (!this.isMoving.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(new IllegalStateException("Furniture is moving"));
        }
        try {
            ItemDisplay itemDisplay = this.metaEntity.get();
            if (itemDisplay == null) {
                this.isMoving.set(false); // 解锁
                return CompletableFuture.completedFuture(false);
            }
            if (!force) {
                // 检查新位置是否可用
                List<AABB> aabbs = new ArrayList<>();
                for (FurnitureHitBoxConfig<?> hitBoxConfig : currentVariant().hitBoxConfigs()) {
                    hitBoxConfig.prepareBoundingBox(position, aabbs::add, false);
                }
                if (!aabbs.isEmpty()) {
                    if (!CollisionUtils.test(position.world.minecraftWorld(), aabbs.stream().map(it -> AABBProxy.INSTANCE.newInstance(it.minX, it.minY, it.minZ, it.maxX, it.maxY, it.maxZ)).toList(),
                            o -> {
                                for (Collider collider : super.snapshot.colliders()) {
                                    if (o == collider.handle()) {
                                        return false;
                                    }
                                }
                                return true;
                            })) {
                        this.isMoving.set(false); // 解锁
                        return CompletableFuture.completedFuture(false);
                    }
                }
            }

            // 先移除
            {
                BukkitFurnitureManager.instance().invalidateFurniture(this, false);
                super.destroySeats();
                super.clearColliders();

                Object removePacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(), l -> l.add(itemDisplay.getEntityId())));
                for (Player player : itemDisplay.getTrackedPlayers()) {
                    BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
                    if (serverPlayer == null) continue;
                    serverPlayer.sendPacket(removePacket, false);
                }
            }

            this.location = LocationUtils.toLocation(position);

            return itemDisplay.teleportAsync(this.location).handle((result, throwable) -> {
                try {
                    if (result != null && result && throwable == null) {
                        super.setVariantInternal(currentVariant());
                        BukkitFurnitureManager.instance().initFurniture(this);
                        this.addCollidersToWorld();
                        Object addPacket = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(itemDisplay.getEntityId(), itemDisplay.getUniqueId(),
                                itemDisplay.getX(), itemDisplay.getY(), itemDisplay.getZ(), itemDisplay.getPitch(), itemDisplay.getYaw(), EntityTypeProxy.ITEM_DISPLAY, 0, Vec3Proxy.ZERO, 0);

                        for (Player player : itemDisplay.getTrackedPlayers()) {
                            BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
                            if (serverPlayer == null) continue;
                            serverPlayer.sendPacket(addPacket, false);
                        }
                        return true;
                    } else {
                        return false;
                    }
                } finally {
                    this.isMoving.set(false); // 解锁
                }
            });
        } catch (Throwable e) {
            this.isMoving.set(false); // 因发生异常而解锁
            return CompletableFuture.failedFuture(e);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void refresh() {
        ItemDisplay itemDisplay = this.metaEntity.get();
        if (itemDisplay == null) return;
        Object removePacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(), l -> l.add(itemDisplay.getEntityId())));
        Object addPacket = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(itemDisplay.getEntityId(), itemDisplay.getUniqueId(),
                itemDisplay.getX(), itemDisplay.getY(), itemDisplay.getZ(), itemDisplay.getPitch(), itemDisplay.getYaw(), EntityTypeProxy.ITEM_DISPLAY, 0, Vec3Proxy.ZERO, 0);
        for (Player player : itemDisplay.getTrackedPlayers()) {
            BukkitServerPlayer serverPlayer = BukkitAdaptor.adapt(player);
            if (serverPlayer == null) continue;
            serverPlayer.sendPacket(removePacket, false);
            serverPlayer.sendPacket(addPacket, false);
        }
    }

    @Override
    public void refresh(net.momirealms.craftengine.core.entity.player.Player player) {
        ItemDisplay itemDisplay = this.metaEntity.get();
        if (itemDisplay == null) return;
        Object removePacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(), l -> l.add(itemDisplay.getEntityId())));
        Object addPacket = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(itemDisplay.getEntityId(), itemDisplay.getUniqueId(),
                itemDisplay.getX(), itemDisplay.getY(), itemDisplay.getZ(), itemDisplay.getPitch(), itemDisplay.getYaw(), EntityTypeProxy.ITEM_DISPLAY, 0, Vec3Proxy.ZERO, 0);
        player.sendPacket(removePacket, false);
        player.sendPacket(addPacket, false);
    }

    @Override
    public void destroy(net.momirealms.craftengine.core.entity.player.Player player) {
        try {
            this.controller.preRemove(player);
        } finally {
            Optional.ofNullable(this.metaEntity.get()).ifPresent(Entity::remove);
            for (Collider entity : super.snapshot.colliders()) {
                entity.destroy();
            }
            this.controller.postRemove(player);
        }
    }

    // 获取掉落物的位置，受到家具变种的影响
    public Location getDropLocation() {
        Vector3f dropOffset = this.currentVariant().dropOffset();
        Quaternionf conjugated = QuaternionUtils.toQuaternionf(0, Math.toRadians(180 - this.location.getYaw()), 0).conjugate();
        Vector3f offset = conjugated.transform(new Vector3f(dropOffset));
        return new Location(this.location.getWorld(), this.location.getX() + offset.x, this.location.getY() + offset.y, this.location.getZ() - offset.z);
    }

    public Location location() {
        return location;
    }

    public Entity getBukkitEntity() {
        return bukkitEntity();
    }

    public Entity bukkitEntity() {
        return this.metaEntity.get();
    }

    /**
     * Use {@link #bukkitEntity()} instead
     */
    @Deprecated
    public Entity baseEntity() {
        return bukkitEntity();
    }

    @SuppressWarnings("deprecation")
    @Override
    public Set<net.momirealms.craftengine.core.entity.player.Player> getTrackedBy() {
        ItemDisplay itemDisplay = this.metaEntity.get();
        if (itemDisplay == null) return Set.of();
        Set<Player> trackedPlayers = itemDisplay.getTrackedPlayers();
        Set<net.momirealms.craftengine.core.entity.player.Player> players = new HashSet<>();
        for (Player player : trackedPlayers) {
            players.add(BukkitAdaptor.adapt(player));
        }
        return players;
    }

    @Override
    public void saveIfDirty() {
        if (super.isUnsaved()) {
            CompoundTag dataToSave = new CompoundTag();
            this.controller.saveCustomData(dataToSave);
            if (dataToSave.isEmpty()) {
                this.persistentData.removeTag(FurniturePersistentData.CUSTOM_DATA);
            } else {
                this.persistentData.addTag(FurniturePersistentData.CUSTOM_DATA, dataToSave);
            }
            super.unsaved = false;
        }
        if (super.persistentData.isUnsaved()) {
            try {
                bukkitEntity().getPersistentDataContainer().set(BukkitFurnitureManager.FURNITURE_EXTRA_DATA_KEY, PersistentDataType.BYTE_ARRAY, super.persistentData.toBytes());
            } catch (IOException e) {
                CraftEngine.instance().logger().warn("Failed to save furniture data for " + CraftEntityProxy.INSTANCE.getEntity(bukkitEntity()), e);
            }
            super.persistentData.clearUnsavedFlag();
        }
    }
}