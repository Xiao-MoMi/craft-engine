package net.momirealms.craftengine.bukkit.entity.furniture;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptor;
import net.momirealms.craftengine.bukkit.entity.BukkitEntity;
import net.momirealms.craftengine.bukkit.plugin.network.packet.ClientboundFurnitureUpdatePacket;
import net.momirealms.craftengine.bukkit.util.CollisionUtils;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.furniture.*;
import net.momirealms.craftengine.core.entity.furniture.element.FurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBox;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.QuaternionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import net.momirealms.craftengine.proxy.bukkit.craftbukkit.entity.CraftEntityProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundAddEntityPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacketProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityProxy;
import net.momirealms.craftengine.proxy.minecraft.world.entity.EntityTypesProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.AABBProxy;
import net.momirealms.craftengine.proxy.minecraft.world.phys.Vec3Proxy;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.persistence.PersistentDataType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("DuplicatedCode")
public final class BukkitFurniture extends Furniture {
    private final AtomicBoolean isMoving = new AtomicBoolean(false);
    private final WeakReference<ItemDisplay> metaEntity;
    private Location location;
    // 仅在构建、行为回调和服务端登记完成后发布，网络线程不会读取构建中的快照。
    private volatile FurnitureSnapshotState clientSnapshot;

    public FurnitureSnapshotState clientSnapshot() {
        return this.clientSnapshot;
    }

    public void publishClientSnapshot(List<Player> players) {
        this.clientSnapshot = this.snapshot;
        if (players.isEmpty()) return;
        ClientboundFurnitureUpdatePacket packet = new ClientboundFurnitureUpdatePacket(this.entityId());
        for (Player player : players) player.sendCustomPacket(packet);
    }

    @Override
    protected Collider createCollider(ColliderConfig config) {
        Object entity = this.metaDataEntity.minecraftEntity();
        return new BukkitCollider(EntityProxy.INSTANCE.getLevel(entity), EntityProxy.INSTANCE.getX(entity), EntityProxy.INSTANCE.getY(entity), EntityProxy.INSTANCE.getZ(entity), config);
    }

    public BukkitFurniture(ItemDisplay metaEntity, FurnitureDefinition config, FurniturePersistentData data) {
        super(new BukkitEntity(metaEntity), data, config);
        this.metaEntity = new WeakReference<>(metaEntity);
        this.location = metaEntity.getLocation();
    }

    @Override
    protected FurnitureSnapshotState createSnapshot(List<FurnitureElement> elements,
                                                    List<FurnitureHitBox> hitboxes,
                                                    Int2ObjectMap<FurnitureHitBox> hitboxMap,
                                                    List<Collider> colliders) {
        return new BukkitVariantSnapshot(elements, hitboxes, hitboxMap, colliders);
    }

    @Override
    public boolean setVariant(String variantName, boolean force) {
        if (!this.isValid()) return false;
        FurnitureVariant variant = this.config.getVariant(variantName);
        if (variant == null) return false;
        if (this.currentVariant == variant) return false;
        // 检查新位置是否可用
        if (!force) {
            List<AABB> aabbs = new ArrayList<>();
            WorldPosition position = position();
            List<? extends FurnitureHitBoxConfig<?>> hitboxConfigs = variant.hitBoxConfigs();
            for (int configIndex = 0, configCount = hitboxConfigs.size(); configIndex < configCount; configIndex++) {
                FurnitureHitBoxConfig<?> hitBoxConfig = hitboxConfigs.get(configIndex);
                hitBoxConfig.prepareBoundingBox(position, aabbs::add, false);
            }
            if (!aabbs.isEmpty()) {
                if (!CollisionUtils.test(position.world.minecraftWorld(), aabbs.stream().map(it -> AABBProxy.INSTANCE.newInstance(it.minX, it.minY, it.minZ, it.maxX, it.maxY, it.maxZ)).toList(),
                        o -> {
                            List<Collider> colliders = super.snapshot.colliders();
                            for (int colliderIndex = 0, colliderCount = colliders.size(); colliderIndex < colliderCount; colliderIndex++) {
                                Collider collider = colliders.get(colliderIndex);
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

        List<Player> trackedBy = this.trackedBy();
        // 服务端实体仍在家具所属线程销毁和登记。
        BukkitFurnitureManager.instance().unregisterFurniture(this, true);
        super.setVariantInternal(variant);
        BukkitFurnitureManager.instance().registerFurniture(this);
        this.addCollidersToWorld();
        this.publishClientSnapshot(trackedBy);
        return true;
    }

    @Override
    public CompletableFuture<Boolean> moveTo(WorldPosition position, boolean force) {
        // 加锁
        if (!this.isMoving.compareAndSet(false, true)) {
            return CompletableFuture.failedFuture(new IllegalStateException("Furniture is moving"));
        }
        try {
            ItemDisplay itemDisplay = this.metaEntity.get();
            if (itemDisplay == null || !itemDisplay.isValid()) {
                this.isMoving.set(false); // 解锁
                return CompletableFuture.completedFuture(false);
            }
            if (!force) {
                // 检查新位置是否可用
                List<AABB> aabbs = new ArrayList<>();
                List<? extends FurnitureHitBoxConfig<?>> hitboxConfigs = currentVariant().hitBoxConfigs();
                for (int configIndex = 0, configCount = hitboxConfigs.size(); configIndex < configCount; configIndex++) {
                    FurnitureHitBoxConfig<?> hitBoxConfig = hitboxConfigs.get(configIndex);
                    hitBoxConfig.prepareBoundingBox(position, aabbs::add, false);
                }
                if (!aabbs.isEmpty()) {
                    if (!CollisionUtils.test(position.world.minecraftWorld(), aabbs.stream().map(it -> AABBProxy.INSTANCE.newInstance(it.minX, it.minY, it.minZ, it.maxX, it.maxY, it.maxZ)).toList(),
                            o -> {
                                List<Collider> colliders = super.snapshot.colliders();
                                for (int colliderIndex = 0, colliderCount = colliders.size(); colliderIndex < colliderCount; colliderIndex++) {
                                    Collider collider = colliders.get(colliderIndex);
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
                BukkitFurnitureManager.instance().unregisterFurniture(this, true);
            }

            Location location = LocationUtils.toLocation(position);
            if (VersionHelper.hasPaperPatch) {
                return itemDisplay.teleportAsync(location).handle((result, throwable) -> {
                    try {
                        if (result != null && result && throwable == null && this.isValid()) {
                            this.location = location;
                            super.updatePlacement();
                            List<Player> afterTrackedBy = trackedBy();
                            super.setVariantInternal(currentVariant());
                            BukkitFurnitureManager.instance().registerFurniture(this);
                            this.addCollidersToWorld();
                            this.publishClientSnapshot(afterTrackedBy);
                            return true;
                        } else {
                            return false;
                        }
                    } finally {
                        this.isMoving.set(false); // 解锁
                    }
                });
            } else {
                itemDisplay.teleport(location);
                if (!this.isValid()) {
                    this.isMoving.set(false);
                    return CompletableFuture.completedFuture(false);
                }
                this.location = location;
                super.updatePlacement();
                List<Player> afterTrackedBy = trackedBy();
                super.setVariantInternal(currentVariant());
                BukkitFurnitureManager.instance().registerFurniture(this);
                this.addCollidersToWorld();
                this.publishClientSnapshot(afterTrackedBy);
                this.isMoving.set(false);
                return CompletableFuture.completedFuture(true);
            }
        } catch (Throwable e) {
            this.isMoving.set(false); // 因发生异常而解锁
            return CompletableFuture.failedFuture(e);
        }
    }

    public boolean isMoving() {
        return this.isMoving.get();
    }

    // 外部传送已经完成；这里只同步派生状态，不再次传送元数据实体。
    void synchronizePosition() {
        ItemDisplay entity = this.metaEntity.get();
        if (entity == null || !entity.isValid()) return;
        Location actualLocation = entity.getLocation();
        if (this.location.equals(actualLocation)) return;
        if (!this.isMoving.compareAndSet(false, true)) return;
        try {
            List<Player> trackedBy = this.trackedBy();
            BukkitFurnitureManager manager = BukkitFurnitureManager.instance();
            manager.unregisterFurniture(this, true);
            this.location = actualLocation;
            super.updatePlacement();
            super.setVariantInternal(this.currentVariant());
            manager.registerFurniture(this);
            this.addCollidersToWorld();
            this.publishClientSnapshot(trackedBy);
        } finally {
            this.isMoving.set(false);
        }
    }

    @Override
    public void refresh() {
        ItemDisplay itemDisplay = this.metaEntity.get();
        if (itemDisplay == null) return;
        Object removePacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(), l -> l.add(itemDisplay.getEntityId())));
        Location displayLocation = itemDisplay.getLocation();
        Object addPacket = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(itemDisplay.getEntityId(), itemDisplay.getUniqueId(),
                displayLocation.getX(), displayLocation.getY(), displayLocation.getZ(), displayLocation.getPitch(), displayLocation.getYaw(), EntityTypesProxy.ITEM_DISPLAY, 0, Vec3Proxy.ZERO, 0);
        List<Player> trackedBy = trackedBy();
        for (int playerIndex = 0, playerCount = trackedBy.size(); playerIndex < playerCount; playerIndex++) {
            Player player = trackedBy.get(playerIndex);
            player.sendPacket(removePacket, false);
            player.sendPacket(addPacket, false);
        }
    }

    @Override
    public void refresh(Player player) {
        ItemDisplay itemDisplay = this.metaEntity.get();
        if (itemDisplay == null) return;
        Object removePacket = ClientboundRemoveEntitiesPacketProxy.INSTANCE.newInstance(MiscUtils.init(new IntArrayList(), l -> l.add(itemDisplay.getEntityId())));
        Location displayLocation = itemDisplay.getLocation();
        Object addPacket = ClientboundAddEntityPacketProxy.INSTANCE.newInstance(itemDisplay.getEntityId(), itemDisplay.getUniqueId(),
                displayLocation.getX(), displayLocation.getY(), displayLocation.getZ(), displayLocation.getPitch(), displayLocation.getYaw(), EntityTypesProxy.ITEM_DISPLAY, 0, Vec3Proxy.ZERO, 0);
        player.sendPacket(removePacket, false);
        player.sendPacket(addPacket, false);
    }

    @Override
    public void destroy(Player player) {
        // 这是 CE 主动拆除（含 preRemove/postRemove）的入口。
        // Paper 上 metaEntity.remove 会同步进入 manager.unloadFurnitureFromEntity，撤销登记并调用 onUnload；
        // /kill、WorldEdit 删除不会反向调用本方法。纯 Spigot 缺少该 Paper 单实体回调，详见生命周期文档。
        try {
            this.controller.preRemove(player);
        } finally {
            Optional.ofNullable(this.metaEntity.get()).ifPresent(Entity::remove);
            List<Collider> colliders = super.snapshot.colliders();
            for (int colliderIndex = 0, colliderCount = colliders.size(); colliderIndex < colliderCount; colliderIndex++) {
                Collider entity = colliders.get(colliderIndex);
                entity.destroy();
            }
            destroySeats();
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

    @Override
    public List<Player> trackedBy() {
        ItemDisplay itemDisplay = this.metaEntity.get();
        if (itemDisplay == null) return List.of();
        return EntityUtils.getTrackedByList(itemDisplay, BukkitAdaptor::adapt);
    }

    @Override
    public Set<Player> getTrackedBy() {
        ItemDisplay itemDisplay = this.metaEntity.get();
        if (itemDisplay == null) return Set.of();
        return EntityUtils.getTrackedBySet(itemDisplay, BukkitAdaptor::adapt);
    }

    @Override
    public void saveIfDirty() {
        // 更新元数据实体的 PDC，不直接写区块文件。WorldSave 和运行时卸载都会调用，
        // 包括已经不再 valid 的元数据实体；不能用 isValid() 作为保存前提。
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
