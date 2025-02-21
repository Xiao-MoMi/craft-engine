package net.momirealms.craftengine.bukkit.entity.furniture;

import net.momirealms.craftengine.bukkit.entity.DisplayEntityData;
import net.momirealms.craftengine.bukkit.entity.InteractionEntityData;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.util.LegacyAttributeUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.entity.furniture.*;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ArrayUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.QuaternionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.lang.ref.WeakReference;
import java.util.*;

public class LoadedFurniture {
    private final Key id;
    private final CustomFurniture furniture;
    private final AnchorType anchorType;
    private final Map<Integer, FurnitureElement> elements;
    private final Map<Integer, HitBox> hitBoxes;
    // location
    private Location location;
    // cached spawn packet
    private Object cachedSpawnPacket;
    // base entity
    private final WeakReference<Entity> baseEntity;
    private final int baseEntityId;
    // includes elements + interactions
    private final int[] subEntityIds;
    // interactions
    private final int[] interactionEntityIds;
    // seats
    private final Set<Vector3f> occupiedSeats = Collections.synchronizedSet(new HashSet<>());
    private final Vector<Entity> seats = new Vector<>();

    public LoadedFurniture(Entity baseEntity,
                           CustomFurniture furniture,
                           AnchorType anchorType) {
        this.id = furniture.id();
        this.baseEntityId = baseEntity.getEntityId();
        this.anchorType = anchorType;
        this.location = baseEntity.getLocation();
        this.baseEntity = new WeakReference<>(baseEntity);
        this.furniture = furniture;
        this.hitBoxes = new HashMap<>();
        this.elements = new HashMap<>();
        List<Integer> entityIds = new ArrayList<>();
        List<Integer> interactionEntityIds = new ArrayList<>();
        CustomFurniture.Placement placement = furniture.getPlacement(anchorType);
        for (FurnitureElement element : placement.elements()) {
            int entityId = Reflections.instance$Entity$ENTITY_COUNTER.incrementAndGet();
            entityIds.add(entityId);
            this.elements.put(entityId, element);
        }
        for (HitBox hitBox : placement.hitbox()) {
            int entityId = Reflections.instance$Entity$ENTITY_COUNTER.incrementAndGet();
            entityIds.add(entityId);
            interactionEntityIds.add(entityId);
            this.hitBoxes.put(entityId, hitBox);
        }
        this.subEntityIds = new int[entityIds.size()];
        for (int i = 0; i < entityIds.size(); ++i) {
            this.subEntityIds[i] = entityIds.get(i);
        }
        this.interactionEntityIds = new int[interactionEntityIds.size()];
        for (int i = 0; i < interactionEntityIds.size(); ++i) {
            this.interactionEntityIds[i] = interactionEntityIds.get(i);
        }
    }

    private void resetSpawnPackets() {
        try {
            List<Object> packets = new ArrayList<>();
            for (Map.Entry<Integer, FurnitureElement> entry : elements.entrySet()) {
                int entityId = entry.getKey();
                FurnitureElement element = entry.getValue();
                Item<ItemStack> item = BukkitItemManager.instance().createWrappedItem(element.item(), null);
                if (item == null) {
                    CraftEngine.instance().logger().warn("Failed to create furniture element for " + id + " because item " + element.item() + " not found");
                    continue;
                }
                item.load();

                Vector3f offset = QuaternionUtils.toQuaternionf(0, Math.toRadians(180 - this.location.getYaw()), 0).conjugate().transform(new Vector3f(element.offset()));
                Object addEntityPacket = Reflections.constructor$ClientboundAddEntityPacket.newInstance(
                        entityId, UUID.randomUUID(), this.location.getX() + offset.x, this.location.getY() + offset.y, this.location.getZ() - offset.z, 0, this.location.getYaw(),
                        Reflections.instance$EntityType$ITEM_DISPLAY, 0, Reflections.instance$Vec3$Zero, 0
                );

                ArrayList<Object> values = new ArrayList<>();
                DisplayEntityData.DisplayedItem.addEntityDataIfNotDefaultValue(item.getLiteralObject(), values);
                DisplayEntityData.Scale.addEntityDataIfNotDefaultValue(element.scale(), values);
                DisplayEntityData.RotationLeft.addEntityDataIfNotDefaultValue(element.rotation(), values);
                DisplayEntityData.BillboardConstraints.addEntityDataIfNotDefaultValue(element.billboard().id(), values);
                DisplayEntityData.Translation.addEntityDataIfNotDefaultValue(element.translation(), values);
                DisplayEntityData.DisplayType.addEntityDataIfNotDefaultValue(element.transform().id(), values);
                Object setDataPacket = Reflections.constructor$ClientboundSetEntityDataPacket.newInstance(entityId, values);

                packets.add(addEntityPacket);
                packets.add(setDataPacket);
            }
            for (Map.Entry<Integer, HitBox> entry : hitBoxes.entrySet()) {
                int entityId = entry.getKey();
                HitBox hitBox = entry.getValue();
                Vector3f offset = QuaternionUtils.toQuaternionf(0, Math.toRadians(180 - this.location.getYaw()), 0).conjugate().transform(new Vector3f(hitBox.offset()));
                Object addEntityPacket = Reflections.constructor$ClientboundAddEntityPacket.newInstance(
                        entityId, UUID.randomUUID(), this.location.getX() + offset.x, this.location.getY() + offset.y, this.location.getZ() - offset.z, 0, this.location.getYaw(),
                        Reflections.instance$EntityType$INTERACTION, 0, Reflections.instance$Vec3$Zero, 0
                );

                ArrayList<Object> values = new ArrayList<>();
                InteractionEntityData.Height.addEntityDataIfNotDefaultValue(hitBox.size().y, values);
                InteractionEntityData.Width.addEntityDataIfNotDefaultValue(hitBox.size().x, values);
                InteractionEntityData.Responsive.addEntityDataIfNotDefaultValue(hitBox.responsive(), values);
                Object setDataPacket = Reflections.constructor$ClientboundSetEntityDataPacket.newInstance(entityId, values);

                packets.add(addEntityPacket);
                packets.add(setDataPacket);
            }
            this.cachedSpawnPacket = Reflections.constructor$ClientboundBundlePacket.newInstance(packets);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to init spawn packets for furniture " + id, e);
        }
    }

    @NotNull
    public Location location() {
        return this.location;
    }

    public void teleport(@NotNull Location location) {
        if (location.equals(this.location)) return;
        this.location = location;
    }

    public Object spawnPacket() {
        if (this.cachedSpawnPacket == null) {
            this.resetSpawnPackets();
        }
        return this.cachedSpawnPacket;
    }

    @NotNull
    public Entity baseEntity() {
        Entity entity = baseEntity.get();
        if (entity == null) {
            throw new RuntimeException("Base entity not found");
        }
        return entity;
    }

    public boolean isValid() {
        return baseEntity().isValid();
    }

    public void destroy() {
        if (!isValid()) {
            return;
        }
        this.baseEntity().remove();
        for (Entity entity : this.seats) {
            for (Entity passenger : entity.getPassengers()) {
                entity.removePassenger(passenger);
            }
            entity.remove();
        }
        this.seats.clear();
    }

    public void destroySeats() {
        for (Entity entity : this.seats) {
            entity.remove();
        }
        this.seats.clear();
    }

    public void addSeatEntity(Entity entity) {
        this.seats.add(entity);
    }

    public Optional<Seat> getAvailableSeat(int clickedEntityId) {
        HitBox hitbox = this.hitBoxes.get(clickedEntityId);
        if (hitbox == null)
            return Optional.empty();
        Seat[] seats = hitbox.seats();
        if (ArrayUtils.isEmpty(seats)) {
            return Optional.empty();
        }
        for (Seat seat : seats) {
            if (!this.occupiedSeats.contains(seat.offset())) {
                return Optional.of(seat);
            }
        }
        return Optional.empty();
    }

    public Location getSeatLocation(Seat seat) {
        Vector3f offset = QuaternionUtils.toQuaternionf(0, Math.toRadians(180 - this.location.getYaw()), 0).conjugate().transform(new Vector3f(seat.offset()));
        double yaw = seat.yaw() + this.location.getYaw();
        if (yaw < -180) yaw += 360;
        Location newLocation = this.location.clone();
        newLocation.setYaw((float) yaw);
        newLocation.add(offset.x, offset.y + 0.6, -offset.z);
        return newLocation;
    }

    public boolean releaseSeat(Vector3f seat) {
        return this.occupiedSeats.remove(seat);
    }

    public boolean occupySeat(Seat seat) {
        if (this.occupiedSeats.contains(seat.offset())) {
            return false;
        }
        this.occupiedSeats.add(seat.offset());
        return true;
    }

    public int baseEntityId() {
        return baseEntityId;
    }

    public int[] interactionEntityIds() {
        return interactionEntityIds;
    }

    public int[] subEntityIds() {
        return this.subEntityIds;
    }

    public AnchorType anchorType() {
        return anchorType;
    }

    public Key furnitureId() {
        return id;
    }

    public CustomFurniture furniture() {
        return furniture;
    }

    public void mountSeat(org.bukkit.entity.Player player, Seat seat) {
        Location location = this.getSeatLocation(seat);
        Entity seatEntity = seat.limitPlayerRotation() ?
                EntityUtils.spawnEntity(player.getWorld(), VersionHelper.isVersionNewerThan1_20_2() ? location.subtract(0,0.9875,0) : location.subtract(0,0.990625,0), EntityType.ARMOR_STAND, entity -> {
                    ArmorStand armorStand = (ArmorStand) entity;
                    if (VersionHelper.isVersionNewerThan1_21_3()) {
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
                EntityUtils.spawnEntity(player.getWorld(), VersionHelper.isVersionNewerThan1_20_2() ? location : location.subtract(0,0.25,0), EntityType.ITEM_DISPLAY, entity -> {
                    ItemDisplay itemDisplay = (ItemDisplay) entity;
                    itemDisplay.setPersistent(false);
                    itemDisplay.getPersistentDataContainer().set(BukkitFurnitureManager.FURNITURE_SEAT_BASE_ENTITY_KEY, PersistentDataType.INTEGER, this.baseEntityId());
                    itemDisplay.getPersistentDataContainer().set(BukkitFurnitureManager.FURNITURE_SEAT_VECTOR_3F_KEY, PersistentDataType.STRING, seat.offset().x + ", " + seat.offset().y + ", " + seat.offset().z);
                });
        this.addSeatEntity(seatEntity);
        seatEntity.addPassenger(player);
    }
}
