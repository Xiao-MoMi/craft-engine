package net.momirealms.craftengine.bukkit.entity.furniture;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.api.BukkitAdaptors;
import net.momirealms.craftengine.bukkit.entity.BukkitEntity;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.MEntityTypes;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.furniture.*;
import net.momirealms.craftengine.core.entity.furniture.hitbox.FurnitureHitBoxConfig;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.QuaternionUtils;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("DuplicatedCode")
public class BukkitFurniture extends Furniture {
    private final WeakReference<ItemDisplay> metaEntity;
    private Location location;

    public BukkitFurniture(ItemDisplay metaEntity, CustomFurniture config, FurnitureDataAccessor data) {
        super(new BukkitEntity(metaEntity), data, config);
        this.metaEntity = new WeakReference<>(metaEntity);
        this.location = metaEntity.getLocation();
    }

    @Override
    public void addCollidersToWorld() {
        Object world = FastNMS.INSTANCE.field$CraftWorld$ServerLevel(this.location.getWorld());
        for (Collider entity : super.colliders) {
            Entity bukkitEntity = FastNMS.INSTANCE.method$Entity$getBukkitEntity(entity.handle());
            bukkitEntity.getPersistentDataContainer().set(BukkitFurnitureManager.FURNITURE_COLLISION, PersistentDataType.BYTE, (byte) 1);
            bukkitEntity.setPersistent(false);
            if (!bukkitEntity.isValid()) {
                FastNMS.INSTANCE.method$LevelWriter$addFreshEntity(world, entity.handle());
            }
        }
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
                if (!FastNMS.INSTANCE.checkEntityCollision(position.world.serverWorld(), aabbs.stream().map(it -> FastNMS.INSTANCE.constructor$AABB(it.minX, it.minY, it.minZ, it.maxX, it.maxY, it.maxZ)).toList(),
                        o -> {
                            for (Collider collider : super.colliders) {
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
        // 删除椅子
        super.destroySeats();
        BukkitFurnitureManager.instance().invalidateFurniture(this);
        super.clearColliders();
        super.setVariantInternal(variant);
        BukkitFurnitureManager.instance().initFurniture(this);
        this.addCollidersToWorld();
        this.refresh();
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public CompletableFuture<Boolean> moveTo(WorldPosition position, boolean force) {
        ItemDisplay itemDisplay = this.metaEntity.get();
        if (itemDisplay == null) return CompletableFuture.completedFuture(false);
        if (!force) {
            // 检查新位置是否可用
            List<AABB> aabbs = new ArrayList<>();
            for (FurnitureHitBoxConfig<?> hitBoxConfig : currentVariant().hitBoxConfigs()) {
                hitBoxConfig.prepareBoundingBox(position, aabbs::add, false);
            }
            if (!aabbs.isEmpty()) {
                if (!FastNMS.INSTANCE.checkEntityCollision(position.world.serverWorld(), aabbs.stream().map(it -> FastNMS.INSTANCE.constructor$AABB(it.minX, it.minY, it.minZ, it.maxX, it.maxY, it.maxZ)).toList(),
                        o -> {
                            for (Collider collider : super.colliders) {
                                if (o == collider.handle()) {
                                    return false;
                                }
                            }
                            return true;
                        })) {
                    return CompletableFuture.completedFuture(false);
                }
            }
        }
        // 删除椅子
        super.destroySeats();
        // 准备传送
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        BukkitFurnitureManager.instance().invalidateFurniture(this);
        super.clearColliders();
        this.location = LocationUtils.toLocation(position);
        Object removePacket = FastNMS.INSTANCE.constructor$ClientboundRemoveEntitiesPacket(MiscUtils.init(new IntArrayList(), l -> l.add(itemDisplay.getEntityId())));
        for (Player player : itemDisplay.getTrackedPlayers()) {
            BukkitServerPlayer serverPlayer = BukkitAdaptors.adapt(player);
            if (serverPlayer == null) continue;
            serverPlayer.sendPacket(removePacket, false);
        }
        itemDisplay.teleportAsync(this.location).thenAccept(result -> {
            if (result) {
                super.setVariantInternal(currentVariant());
                BukkitFurnitureManager.instance().initFurniture(this);
                this.addCollidersToWorld();
                Object addPacket = FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(itemDisplay.getEntityId(), itemDisplay.getUniqueId(),
                        itemDisplay.getX(), itemDisplay.getY(), itemDisplay.getZ(), itemDisplay.getPitch(), itemDisplay.getYaw(), MEntityTypes.ITEM_DISPLAY, 0, CoreReflections.instance$Vec3$Zero, 0);
                for (Player player : itemDisplay.getTrackedPlayers()) {
                    BukkitServerPlayer serverPlayer = BukkitAdaptors.adapt(player);
                    if (serverPlayer == null) continue;
                    serverPlayer.sendPacket(addPacket, false);
                }
                future.complete(true);
            } else {
                future.complete(false);
            }
        });
        return future;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void refresh() {
        ItemDisplay itemDisplay = this.metaEntity.get();
        if (itemDisplay == null) return;
        Object removePacket = FastNMS.INSTANCE.constructor$ClientboundRemoveEntitiesPacket(MiscUtils.init(new IntArrayList(), l -> l.add(itemDisplay.getEntityId())));
        Object addPacket = FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(itemDisplay.getEntityId(), itemDisplay.getUniqueId(),
                itemDisplay.getX(), itemDisplay.getY(), itemDisplay.getZ(), itemDisplay.getPitch(), itemDisplay.getYaw(), MEntityTypes.ITEM_DISPLAY, 0, CoreReflections.instance$Vec3$Zero, 0);
        for (Player player : itemDisplay.getTrackedPlayers()) {
            BukkitServerPlayer serverPlayer = BukkitAdaptors.adapt(player);
            if (serverPlayer == null) continue;
            serverPlayer.sendPacket(removePacket, false);
            serverPlayer.sendPacket(addPacket, false);
        }
    }

    @Override
    public void refresh(net.momirealms.craftengine.core.entity.player.Player player) {
        ItemDisplay itemDisplay = this.metaEntity.get();
        if (itemDisplay == null) return;
        Object removePacket = FastNMS.INSTANCE.constructor$ClientboundRemoveEntitiesPacket(MiscUtils.init(new IntArrayList(), l -> l.add(itemDisplay.getEntityId())));
        Object addPacket = FastNMS.INSTANCE.constructor$ClientboundAddEntityPacket(itemDisplay.getEntityId(), itemDisplay.getUniqueId(),
                itemDisplay.getX(), itemDisplay.getY(), itemDisplay.getZ(), itemDisplay.getPitch(), itemDisplay.getYaw(), MEntityTypes.ITEM_DISPLAY, 0, CoreReflections.instance$Vec3$Zero, 0);
        player.sendPacket(removePacket, false);
        player.sendPacket(addPacket, false);
    }

    @Override
    public void destroy() {
        try {
            this.config.behavior().onRemove(this);
        } finally {
            Optional.ofNullable(this.metaEntity.get()).ifPresent(Entity::remove);
            for (Collider entity : super.colliders) {
                entity.destroy();
            }
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
            players.add(BukkitAdaptors.adapt(player));
        }
        return players;
    }
}