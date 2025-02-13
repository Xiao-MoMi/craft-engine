package net.momirealms.craftengine.bukkit.entity.furniture;

import net.momirealms.craftengine.bukkit.entity.DisplayEntityData;
import net.momirealms.craftengine.bukkit.entity.InteractionEntityData;
import net.momirealms.craftengine.bukkit.item.BukkitItemManager;
import net.momirealms.craftengine.bukkit.item.behavior.FurnitureItemBehavior;
import net.momirealms.craftengine.bukkit.legacy.util.LegacyAttributeUtils;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.util.ItemUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.bukkit.world.BukkitWorld;
import net.momirealms.craftengine.core.entity.furniture.AnchorType;
import net.momirealms.craftengine.core.entity.furniture.FurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.HitBox;
import net.momirealms.craftengine.core.entity.furniture.Seat;
import net.momirealms.craftengine.core.entity.player.Player;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.loot.parameter.LootParameters;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ArrayUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.QuaternionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.util.context.ContextHolder;
import net.momirealms.craftengine.core.world.Vec3d;
import net.momirealms.craftengine.core.world.World;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import java.util.*;

public class LoadedFurniture {
    private final Key id;
    private final Entity baseEntity;
    private final FurnitureItemBehavior behavior;
    private final Map<Integer, FurnitureElement> elements;
    private final Map<Integer, HitBox> hitboxes;
    private final int[] subEntityIds;
    private final int[] interactionEntityIds;
    private final Vector3d position;
    private final Set<Vector3f> occupiedSeats = Collections.synchronizedSet(new HashSet<>());
    private final int baseEntityId;
    private final Quaternionf rotation;
    private final Vector<Entity> seats = new Vector<>();
    private final AnchorType anchorType;
    private Object cachedSpawnPacket;

    public LoadedFurniture(Key id,
                           Entity baseEntity,
                           FurnitureItemBehavior behavior,
                           AnchorType anchorType,
                           Vector3d position,
                           Quaternionf rotation) {
        this.id = id;
        this.baseEntityId = baseEntity.getEntityId();
        this.anchorType = anchorType;
        this.rotation = rotation;
        this.position = position;
        this.baseEntity = baseEntity;
        this.behavior = behavior;
        this.hitboxes = new HashMap<>();
        this.elements = new HashMap<>();
        List<Integer> entityIds = new ArrayList<>();
        List<Integer> interactionEntityIds = new ArrayList<>();
        FurnitureItemBehavior.FurniturePlacement placement = behavior.getPlacement(anchorType);
        for (FurnitureElement element : placement.elements()) {
            int entityId = Reflections.instance$Entity$ENTITY_COUNTER.incrementAndGet();
            entityIds.add(entityId);
            this.elements.put(entityId, element);
        }
        for (HitBox hitBox : placement.hitbox()) {
            int entityId = Reflections.instance$Entity$ENTITY_COUNTER.incrementAndGet();
            entityIds.add(entityId);
            interactionEntityIds.add(entityId);
            this.hitboxes.put(entityId, hitBox);
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

                Vector3f offset = new Quaternionf(this.rotation.x, this.rotation.y, this.rotation.z, this.rotation.w).conjugate().transform(new Vector3f(element.offset()));
                Object addEntityPacket = Reflections.constructor$ClientboundAddEntityPacket.newInstance(
                        entityId, UUID.randomUUID(), position.x() + offset.x, position.y() + offset.y, position.z() - offset.z, 0, 0,
                        Reflections.instance$EntityType$ITEM_DISPLAY, 0, Reflections.instance$Vec3$Zero, 0
                );

                ArrayList<Object> values = new ArrayList<>();
                DisplayEntityData.DisplayedItem.addEntityDataIfNotDefaultValue(item.getLiteralObject(), values);
                DisplayEntityData.Scale.addEntityDataIfNotDefaultValue(element.scale(), values);
                DisplayEntityData.RotationRight.addEntityDataIfNotDefaultValue(this.rotation, values);
                DisplayEntityData.RotationLeft.addEntityDataIfNotDefaultValue(element.rotation(), values);
                DisplayEntityData.BillboardConstraints.addEntityDataIfNotDefaultValue(element.billboard().id(), values);
                DisplayEntityData.Translation.addEntityDataIfNotDefaultValue(element.translation(), values);
                DisplayEntityData.DisplayType.addEntityDataIfNotDefaultValue(element.transform().id(), values);
                Object setDataPacket = Reflections.constructor$ClientboundSetEntityDataPacket.newInstance(entityId, values);

                packets.add(addEntityPacket);
                packets.add(setDataPacket);
            }
            for (Map.Entry<Integer, HitBox> entry : hitboxes.entrySet()) {
                int entityId = entry.getKey();
                HitBox hitBox = entry.getValue();
                Vector3f offset = new Quaternionf(this.rotation.x, this.rotation.y, this.rotation.z, this.rotation.w).conjugate().transform(new Vector3f(hitBox.offset()));
                Object addEntityPacket = Reflections.constructor$ClientboundAddEntityPacket.newInstance(
                        entityId, UUID.randomUUID(), position.x() + offset.x, position.y() + offset.y, position.z() - offset.z, 0, 0,
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

    public Object spawnPacket() {
        if (this.cachedSpawnPacket == null) {
            this.resetSpawnPackets();
        }
        return this.cachedSpawnPacket;
    }

    public boolean isValid() {
        return this.baseEntity.isValid();
    }

    public void onPlayerDestroy(Player player) {
        if (!isValid()) return;
        Location location = baseEntity.getLocation();
        this.baseEntity.remove();
        for (Entity entity : this.seats) {
            for (Entity passenger : entity.getPassengers()) {
                entity.removePassenger(passenger);
            }
            entity.remove();
        }
        this.seats.clear();
        LootTable<ItemStack> lootTable = behavior.lootTable();
        Vec3d vec3d = new Vec3d(position.x(), position.y(), position.z());
        if (lootTable != null && !player.isCreativeMode()) {
            ContextHolder.Builder builder = ContextHolder.builder();
            World world = new BukkitWorld(this.baseEntity.getWorld());
            builder.withParameter(LootParameters.LOCATION, vec3d);
            builder.withParameter(LootParameters.WORLD, world);
            builder.withParameter(LootParameters.PLAYER, player);
            List<Item<ItemStack>> items = lootTable.getRandomItems(builder.build(), world);
            for (Item<ItemStack> item : items) {
                ItemStack itemStack = item.load();
                if (ItemUtils.isEmpty(itemStack)) continue;
                location.getWorld().dropItemNaturally(LocationUtils.toBlockCenterLocation(location), itemStack);
            }
        }
        location.getWorld().playSound(location, behavior.sounds().breakSound().toString(), SoundCategory.BLOCKS,1f, 1f);
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
        HitBox hitbox = this.hitboxes.get(clickedEntityId);
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
        Vector3f offset = new Quaternionf(this.rotation.x, this.rotation.y, this.rotation.z, this.rotation.w).conjugate().transform(new Vector3f(seat.offset()));
        double yaw = -Math.toDegrees(QuaternionUtils.quaternionToPitch(this.rotation)) + seat.yaw();
        if (yaw < -180) yaw += 360;
        return new Location(null, position.x() + offset.x, position.y() + offset.y + 0.6, position.z() - offset.z, (float) yaw, 0f);
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

    public FurnitureItemBehavior behavior() {
        return behavior;
    }

    public Entity baseEntity() {
        return this.baseEntity;
    }

    public AnchorType anchorType() {
        return anchorType;
    }

    public Key furnitureId() {
        return id;
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
