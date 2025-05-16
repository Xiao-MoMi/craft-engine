package net.momirealms.craftengine.bukkit.entity.furniture;

import net.momirealms.craftengine.bukkit.entity.BukkitEntity;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.util.LegacyAttributeUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.entity.furniture.*;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ArrayUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.QuaternionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.*;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.*;

public class LoadedFurniture implements Furniture {
    private final Key id;
    private final CustomFurniture furniture;
    private final AnchorType anchorType;
    private FurnitureExtraData extraData;
    // location
    private final Location location;
    // base entity
    private final WeakReference<Entity> baseEntity;
    private final int baseEntityId;
    // colliders
    private final Collider[] colliderEntities;
    // cache
    private final List<Integer> fakeEntityIds;
    private final List<Integer> entityIds;
    private final Map<Integer, HitBox> hitBoxes  = new HashMap<>();
    private final Map<Integer, AABB> aabb = new HashMap<>();
    private final boolean minimized;
    private final boolean hasExternalModel;
    // seats
    private final Set<Vector3f> occupiedSeats = Collections.synchronizedSet(new HashSet<>());
    private final Vector<WeakReference<Entity>> seats = new Vector<>();
    // cached spawn packet
    private Object cachedSpawnPacket;
    private Object cachedMinimizedSpawnPacket;

    public LoadedFurniture(Entity baseEntity,
                           CustomFurniture furniture,
                           FurnitureExtraData extraData) {
        this.id = furniture.id();
        this.extraData = extraData;
        this.baseEntityId = baseEntity.getEntityId();
        this.anchorType = extraData.anchorType().orElse(furniture.getAnyPlacement());
        this.location = baseEntity.getLocation();
        this.baseEntity = new WeakReference<>(baseEntity);
        this.furniture = furniture;
        this.minimized = furniture.settings().minimized();
        List<Integer> fakeEntityIds = new ArrayList<>();
        List<Integer> mainEntityIds = new ArrayList<>();
        mainEntityIds.add(this.baseEntityId);

        CustomFurniture.Placement placement = furniture.getPlacement(anchorType);
        // bind external furniture
        Optional<ExternalModel> optionalExternal = placement.externalModel();
        if (optionalExternal.isPresent()) {
            try {
                optionalExternal.get().bindModel(new BukkitEntity(baseEntity));
            } catch (Exception e) {
                CraftEngine.instance().logger().warn("Failed to load external model for furniture " + id, e);
            }
            this.hasExternalModel = true;
        } else {
            this.hasExternalModel = false;
        }

        float yaw = this.location.getYaw();
        Quaternionf conjugated = QuaternionUtils.toQuaternionf(0, Math.toRadians(180 - yaw), 0).conjugate();

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();

        List<Object> packets = new ArrayList<>();
        List<Object> minimizedPackets = new ArrayList<>();
        List<Collider> colliders = new ArrayList<>();

        World world = world();
        WorldPosition position = new WorldPosition(world, x, y, z, yaw, 0);
        Integer dyedColor = this.extraData.dyedColor().orElse(null);
        for (FurnitureElement element : placement.elements()) {
            int entityId = Reflections.instance$Entity$ENTITY_COUNTER.incrementAndGet();
            fakeEntityIds.add(entityId);
            element.initPackets(entityId, position, conjugated, dyedColor, packet -> {
                packets.add(packet);
                if (this.minimized) minimizedPackets.add(packet);
            });
        }
        for (HitBox hitBox : placement.hitBoxes()) {
            int[] ids = hitBox.acquireEntityIds(Reflections.instance$Entity$ENTITY_COUNTER::incrementAndGet);
            for (int entityId : ids) {
                fakeEntityIds.add(entityId);
                mainEntityIds.add(entityId);
                this.hitBoxes.put(entityId, hitBox);
            }
            hitBox.initPacketsAndColliders(ids, position, conjugated, (packet, canBeMinimized) -> {
                packets.add(packet);
                if (this.minimized && !canBeMinimized) {
                    minimizedPackets.add(packet);
                }
            }, colliders::add, this.aabb::put);
        }
        try {
            this.cachedSpawnPacket = FastNMS.INSTANCE.constructor$ClientboundBundlePacket(packets);
            if (this.minimized) {
                this.cachedMinimizedSpawnPacket = FastNMS.INSTANCE.constructor$ClientboundBundlePacket(minimizedPackets);
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to init spawn packets for furniture " + id, e);
        }
        this.fakeEntityIds = fakeEntityIds;
        this.entityIds = mainEntityIds;
        this.colliderEntities = colliders.toArray(new Collider[0]);
    }

    @Override
    public void initializeColliders() {
        Object world = FastNMS.INSTANCE.field$CraftWorld$ServerLevel(this.location.getWorld());
        for (Collider entity : this.colliderEntities) {
            FastNMS.INSTANCE.method$LevelWriter$addFreshEntity(world, entity.handle());
            Entity bukkitEntity = FastNMS.INSTANCE.method$Entity$getBukkitEntity(entity.handle());
            bukkitEntity.getPersistentDataContainer().set(BukkitFurnitureManager.FURNITURE_COLLISION, PersistentDataType.BYTE, (byte) 1);
        }
    }

    @NotNull
    public Object spawnPacket(Player player) {
        // TODO hasPermission might be slow, can we use a faster way in the future?
        if (!this.minimized || player.hasPermission(FurnitureManager.FURNITURE_ADMIN_NODE)) {
            return this.cachedSpawnPacket;
        } else {
            return this.cachedMinimizedSpawnPacket;
        }
    }

    @Override
    public Vec3d position() {
        return new Vec3d(location.getX(), location.getY(), location.getZ());
    }

    @Override
    public World world() {
        return new BukkitWorld(this.location.getWorld());
    }

    @NotNull
    public Location location() {
        return this.location.clone();
    }

    @NotNull
    public Entity baseEntity() {
        Entity entity = baseEntity.get();
        if (entity == null) {
            throw new RuntimeException("Base entity not found. It might be unloaded.");
        }
        return entity;
    }

    @Override
    public boolean isValid() {
        return baseEntity().isValid();
    }

    @NotNull
    public Location dropLocation() {
        Optional<Vector3f> dropOffset = config().getPlacement(this.anchorType).dropOffset();
        if (dropOffset.isEmpty()) {
            return location();
        }
        Quaternionf conjugated = QuaternionUtils.toQuaternionf(0, Math.toRadians(180 - this.location.getYaw()), 0).conjugate();
        Vector3f offset = conjugated.transform(new Vector3f(dropOffset.get()));
        return new Location(this.location.getWorld(), this.location.getX() + offset.x, this.location.getY() + offset.y, this.location.getZ() - offset.z);
    }

    @Override
    public void destroy() {
        if (!isValid()) {
            return;
        }
        this.baseEntity().remove();
        for (Collider entity : this.colliderEntities) {
            if (entity != null)
                entity.destroy();
        }
        for (WeakReference<Entity> r : this.seats) {
            Entity entity = r.get();
            if (entity == null) continue;
            for (Entity passenger : entity.getPassengers()) {
                entity.removePassenger(passenger);
            }
            entity.remove();
        }
        this.seats.clear();
    }

    @Override
    public void destroySeats() {
        for (WeakReference<Entity> entity : this.seats) {
            Entity e = entity.get();
            if (e != null) {
                e.remove();
            }
        }
        this.seats.clear();
    }

    @Override
    public Optional<Seat> findFirstAvailableSeat(int targetEntityId) {
        HitBox hitbox = hitBoxes.get(targetEntityId);
        if (hitbox == null) return Optional.empty();

        Seat[] seats = hitbox.seats();
        if (ArrayUtils.isEmpty(seats)) return Optional.empty();

        return Arrays.stream(seats)
                .filter(s -> !occupiedSeats.contains(s.offset()))
                .findFirst();
    }

    @Override
    public boolean removeOccupiedSeat(Vector3f seat) {
        return this.occupiedSeats.remove(seat);
    }

    @Override
    public boolean tryOccupySeat(Seat seat) {
        if (this.occupiedSeats.contains(seat.offset())) {
            return false;
        }
        this.occupiedSeats.add(seat.offset());
        return true;
    }

    @Override
    public UUID uuid() {
        return this.baseEntity().getUniqueId();
    }

    @Override
    public int baseEntityId() {
        return this.baseEntityId;
    }

    @NotNull
    public List<Integer> entityIds() {
        return Collections.unmodifiableList(this.entityIds);
    }

    @NotNull
    public List<Integer> fakeEntityIds() {
        return Collections.unmodifiableList(this.fakeEntityIds);
    }

    public Collider[] collisionEntities() {
        return this.colliderEntities;
    }

    @Nullable
    public HitBox hitBoxByEntityId(int id) {
        return this.hitBoxes.get(id);
    }

    @Nullable
    public AABB aabbByEntityId(int id) {
        return this.aabb.get(id);
    }

    @Override
    public @NotNull AnchorType anchorType() {
        return this.anchorType;
    }

    @Override
    public @NotNull Key id() {
        return this.id;
    }

    @Override
    public @NotNull CustomFurniture config() {
        return this.furniture;
    }

    @Override
    public boolean hasExternalModel() {
        return hasExternalModel;
    }

    @Override
    public void spawnSeatEntityForPlayer(net.momirealms.craftengine.core.entity.player.Player player, Seat seat) {
        spawnSeatEntityForPlayer((Player) player.platformPlayer(), seat);
    }

    @Override
    public FurnitureExtraData extraData() {
        return this.extraData;
    }

    @Override
    public void setExtraData(FurnitureExtraData extraData) {
        this.extraData = extraData;
        this.save();
    }

    @Override
    public void save() {
        try {
            this.baseEntity().getPersistentDataContainer().set(BukkitFurnitureManager.FURNITURE_EXTRA_DATA_KEY, PersistentDataType.BYTE_ARRAY, this.extraData.toBytes());
        } catch (IOException e) {
            CraftEngine.instance().logger().warn("Failed to save furniture data.", e);
        }
    }

    private void spawnSeatEntityForPlayer(org.bukkit.entity.Player player, Seat seat) {
        Location location = this.calculateSeatLocation(seat);
        Entity seatEntity = seat.limitPlayerRotation() ?
                EntityUtils.spawnEntity(player.getWorld(), VersionHelper.isOrAbove1_20_2() ? location.subtract(0,0.9875,0) : location.subtract(0,0.990625,0), EntityType.ARMOR_STAND, entity -> {
                    ArmorStand armorStand = (ArmorStand) entity;
                    if (VersionHelper.isOrAbove1_21_3()) {
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
                    armorStand.getPersistentDataContainer().set(BukkitFurnitureManager.FURNITURE_SEAT_BASE_ENTITY_KEY, PersistentDataType.INTEGER, this.baseEntityId());
                    armorStand.getPersistentDataContainer().set(BukkitFurnitureManager.FURNITURE_SEAT_VECTOR_3F_KEY, PersistentDataType.STRING, seat.offset().x + ", " + seat.offset().y + ", " + seat.offset().z);
                }) :
                EntityUtils.spawnEntity(player.getWorld(), VersionHelper.isOrAbove1_20_2() ? location : location.subtract(0,0.25,0), EntityType.ITEM_DISPLAY, entity -> {
                    ItemDisplay itemDisplay = (ItemDisplay) entity;
                    itemDisplay.setPersistent(false);
                    itemDisplay.getPersistentDataContainer().set(BukkitFurnitureManager.FURNITURE_SEAT_BASE_ENTITY_KEY, PersistentDataType.INTEGER, this.baseEntityId());
                    itemDisplay.getPersistentDataContainer().set(BukkitFurnitureManager.FURNITURE_SEAT_VECTOR_3F_KEY, PersistentDataType.STRING, seat.offset().x + ", " + seat.offset().y + ", " + seat.offset().z);
                });
        this.seats.add(new WeakReference<>(seatEntity));
        seatEntity.addPassenger(player);
    }

    private Location calculateSeatLocation(Seat seat) {
        Vector3f offset = QuaternionUtils.toQuaternionf(0, Math.toRadians(180 - this.location.getYaw()), 0).conjugate().transform(new Vector3f(seat.offset()));
        double yaw = seat.yaw() + this.location.getYaw();
        if (yaw < -180) yaw += 360;
        Location newLocation = this.location.clone();
        newLocation.setYaw((float) yaw);
        newLocation.add(offset.x, offset.y + 0.6, -offset.z);
        return newLocation;
    }
}
