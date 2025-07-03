package net.momirealms.craftengine.bukkit.entity.furniture;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.momirealms.craftengine.bukkit.entity.BukkitEntity;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.reflection.minecraft.CoreReflections;
import net.momirealms.craftengine.bukkit.util.BlockStateUtils;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.util.LegacyAttributeUtils;
import net.momirealms.craftengine.bukkit.util.LocationUtils;
import net.momirealms.craftengine.core.entity.furniture.AnchorType;
import net.momirealms.craftengine.core.entity.furniture.Collider;
import net.momirealms.craftengine.core.entity.furniture.CustomFurniture;
import net.momirealms.craftengine.core.entity.furniture.ExternalModel;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.FurnitureElement;
import net.momirealms.craftengine.core.entity.furniture.FurnitureExtraData;
import net.momirealms.craftengine.core.entity.furniture.FurnitureManager;
import net.momirealms.craftengine.core.entity.furniture.HitBox;
import net.momirealms.craftengine.core.entity.furniture.Seat;
import net.momirealms.craftengine.core.plugin.CraftEngine;
import net.momirealms.craftengine.core.util.ArrayUtils;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.QuaternionUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.BlockPosition;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.craftengine.core.world.collision.AABB;

/**
 * Data class to store complete BlockStateHitBox information for proper cleanup
 */
class BlockStateHitBoxData {
    private final WorldPosition position;
    private final net.momirealms.craftengine.core.block.BlockStateWrapper originalBlockState;
    private final boolean dropContainer;
    private final boolean actuallyPlaced;
    
    public BlockStateHitBoxData(WorldPosition position, 
                               net.momirealms.craftengine.core.block.BlockStateWrapper originalBlockState,
                               boolean dropContainer, 
                               boolean actuallyPlaced) {
        this.position = position;
        this.originalBlockState = originalBlockState;
        this.dropContainer = dropContainer;
        this.actuallyPlaced = actuallyPlaced;
    }
    
    public WorldPosition getPosition() {
        return position;
    }
    
    public net.momirealms.craftengine.core.block.BlockStateWrapper getOriginalBlockState() {
        return originalBlockState;
    }
    
    public boolean isDropContainer() {
        return dropContainer;
    }
    
    public boolean isActuallyPlaced() {
        return actuallyPlaced;
    }
    
    /**
     * Converts this data to a string format for storage in PDC.
     * Format: "worldName,x,y,z,dropContainer,actuallyPlaced,originalBlockStateRegistryId"
     */
    public String toStorageString() {
        return position.world().name() + "," + 
               (int)position.x() + "," + 
               (int)position.y() + "," + 
               (int)position.z() + "," + 
               dropContainer + "," + 
               actuallyPlaced + "," + 
               (originalBlockState != null ? originalBlockState.registryId() : -1);
    }
    
    /**
     * Creates BlockStateHitBoxData from a storage string.
     * Returns null if the string is invalid or the world doesn't match.
     */
    public static BlockStateHitBoxData fromStorageString(String str, net.momirealms.craftengine.core.world.World currentWorld) {
        try {
            String[] parts = str.split(",", 7);
            if (parts.length != 7) return null;
            
            String worldName = parts[0];
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            boolean dropContainer = Boolean.parseBoolean(parts[4]);
            boolean actuallyPlaced = Boolean.parseBoolean(parts[5]);
            int originalStateId = Integer.parseInt(parts[6]);
            
            // Check if world matches
            if (!currentWorld.name().equals(worldName)) {
                return null; // Skip data from different worlds
            }
            
            WorldPosition position = new WorldPosition(currentWorld, x, y, z);
            net.momirealms.craftengine.core.block.BlockStateWrapper originalBlockState = null;
            
            if (originalStateId >= 0) {
                // Reconstruct the original block state from registry ID
                originalBlockState = BlockStateUtils.toPackedBlockState(
                    BlockStateUtils.fromBlockData(BlockStateUtils.idToBlockState(originalStateId))
                );
            }
            
            return new BlockStateHitBoxData(position, originalBlockState, dropContainer, actuallyPlaced);
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to parse BlockStateHitBoxData from string: " + str, e);
            return null;
        }
    }
}

public class BukkitFurniture implements Furniture {
    private final Key id;
    private final CustomFurniture furniture;
    private final CustomFurniture.Placement placement;
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
    private final Map<Integer, HitBox> hitBoxes  = new Int2ObjectArrayMap<>();
    private final Map<Integer, AABB> aabb = new Int2ObjectArrayMap<>();
    private final boolean minimized;
    private final boolean hasExternalModel;
    // seats
    private final Set<Vector3f> occupiedSeats = Collections.synchronizedSet(new HashSet<>());
    private final Vector<WeakReference<Entity>> seats = new Vector<>();
    // BlockStateHitBox positions storage - persistent across server restarts
    private final Set<BlockPosition> blockStateHitBoxPositions = Collections.synchronizedSet(new HashSet<>());
    // BlockStateHitBox complete data for proper cleanup
    private final Map<BlockPosition, BlockStateHitBoxData> blockStateHitBoxDataMap = Collections.synchronizedMap(new HashMap<>());
    // cached spawn packet
    private Object cachedSpawnPacket;
    private Object cachedMinimizedSpawnPacket;

    public BukkitFurniture(Entity baseEntity,
                           CustomFurniture furniture,
                           FurnitureExtraData extraData) {
        this.id = furniture.id();
        this.extraData = extraData;
        this.baseEntityId = baseEntity.getEntityId();

        this.location = baseEntity.getLocation();
        this.baseEntity = new WeakReference<>(baseEntity);
        this.furniture = furniture;
        this.minimized = furniture.settings().minimized();
        List<Integer> fakeEntityIds = new IntArrayList();
        List<Integer> mainEntityIds = new IntArrayList();
        mainEntityIds.add(this.baseEntityId);

        this.placement = furniture.getValidPlacement(extraData.anchorType().orElseGet(furniture::getAnyAnchorType));
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

        Quaternionf conjugated = QuaternionUtils.toQuaternionf(0, Math.toRadians(180 - this.location.getYaw()), 0).conjugate();
        List<Object> packets = new ArrayList<>();
        List<Object> minimizedPackets = new ArrayList<>();
        List<Collider> colliders = new ArrayList<>();
        WorldPosition position = position();
        for (FurnitureElement element : placement.elements()) {
            int entityId = CoreReflections.instance$Entity$ENTITY_COUNTER.incrementAndGet();
            fakeEntityIds.add(entityId);
            element.initPackets(this, entityId, conjugated, packet -> {
                packets.add(packet);
                if (this.minimized) minimizedPackets.add(packet);
            });
        }
        for (HitBox hitBox : placement.hitBoxes()) {
            // Set parent furniture for BlockStateHitBox to enable proper block tracking
            if (hitBox instanceof net.momirealms.craftengine.bukkit.entity.furniture.hitbox.BlockStateHitBox) {
                ((net.momirealms.craftengine.bukkit.entity.furniture.hitbox.BlockStateHitBox) hitBox).setParentFurniture(this);
            }
            
            int[] ids = hitBox.acquireEntityIds(CoreReflections.instance$Entity$ENTITY_COUNTER::incrementAndGet);
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
        
        // Load BlockStateHitBox data from PersistentDataContainer
        this.loadBlockStateHitBoxDataFromPDC();
        
        // Initialize BlockStateHitBox positions after all hitboxes are set up
        this.initializeBlockStateHitBoxPositions();
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
    public WorldPosition position() {
        return LocationUtils.toWorldPosition(this.location);
    }

    @Override
    @NotNull
    public float yaw() {
        return this.baseEntity().getLocation().getYaw();
    }
    @Override
    @NotNull
    public float pitch() {
    
        return this.baseEntity().getLocation().getPitch();
    }

    @NotNull
    public Location location() {
        return this.location.clone();
    }

    @NotNull
    public Entity baseEntity() {
        Entity entity = this.baseEntity.get();
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
        Optional<Vector3f> dropOffset = this.placement.dropOffset();
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
        
        
        // Load blocks from PDC before clearing them for proper cleanup
        List<BlockStateHitBoxData> blocksToRemove = new ArrayList<>();
        try {
            // Read blocks from PDC for later removal
            Entity baseEntity = this.baseEntity();
            String dataString = baseEntity.getPersistentDataContainer().get(
                BukkitFurnitureManager.FURNITURE_BLOCKSTATE_HITBOX_DATA, 
                PersistentDataType.STRING
            );
            
            if (dataString != null && !dataString.isEmpty()) {
                String[] dataStrings = dataString.split(";");
                
                for (String dataStr : dataStrings) {
                    if (dataStr.trim().isEmpty()) continue;
                    
                    BlockStateHitBoxData data = BlockStateHitBoxData.fromStorageString(
                        dataStr, 
                        LocationUtils.toWorldPosition(this.location).world()
                    );
                    
                    if (data != null) {
                        blocksToRemove.add(data);
                    }
                }
                
                CraftEngine.instance().logger().info("Loaded " + blocksToRemove.size() + 
                    " BlockStateHitBox data entries from PDC for removal during furniture destroy: " + this.id());
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to load BlockStateHitBox data from PDC during destroy for furniture: " + this.id(), e);
        }
        
        // Process the loaded blocks for cleanup using the correct parameters
        for (BlockStateHitBoxData data : blocksToRemove) {
            try {
                WorldPosition worldPos = data.getPosition();
                
                // Unregister from furniture manager
                CraftEngine.instance().furnitureManager().unregisterBlockStateHitBox(worldPos);
                
                // Use BlockStateUtils.removeBlockStateHitBoxBlock with correct parameters
                BlockStateUtils.removeBlockStateHitBoxBlock(
                    worldPos, 
                    data.getOriginalBlockState(), 
                    data.isDropContainer(), 
                    data.isActuallyPlaced()
                );
                
                CraftEngine.instance().logger().info("Processed BlockStateHitBox cleanup for position: " + worldPos + 
                    " (original state: " + (data.getOriginalBlockState() != null ? "preserved" : "air") + 
                    ", actually placed: " + data.isActuallyPlaced() + ")");
                    
            } catch (Exception e) {
                CraftEngine.instance().logger().warn("Failed to process block cleanup for data: " + data.toStorageString(), e);
            }
        }
        
        // Clear our own storage and remove from PDC
        this.blockStateHitBoxPositions.clear();
        this.blockStateHitBoxDataMap.clear();
        try {
            this.baseEntity().getPersistentDataContainer().remove(BukkitFurnitureManager.FURNITURE_BLOCKSTATE_HITBOX_POSITIONS);
            this.baseEntity().getPersistentDataContainer().remove(BukkitFurnitureManager.FURNITURE_BLOCKSTATE_HITBOX_DATA);
            CraftEngine.instance().logger().info("Removed BlockStateHitBox data from PDC for furniture: " + this.id());
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to remove BlockStateHitBox data from PDC during destroy", e);
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
        return this.placement.anchorType();
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
        if (!seatEntity.addPassenger(player)) {
            seatEntity.remove();
            this.removeOccupiedSeat(seat.offset());
        }
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

    // Implementation of BlockStateHitBox storage methods
    @Override
    public void addBlockStateHitBoxPosition(WorldPosition position) {
        BlockPosition blockPos = BlockPosition.fromWorldPosition(position);
        this.blockStateHitBoxPositions.add(blockPos);
        // Save to PersistentDataContainer immediately
        this.saveBlockStateHitBoxPositionsToPDC();
    }
    
    /**
     * Adds complete BlockStateHitBox data for proper cleanup.
     * This method should be called by BlockStateHitBox during initialization.
     */
    public void addBlockStateHitBoxData(WorldPosition position, 
                                       net.momirealms.craftengine.core.block.BlockStateWrapper originalBlockState,
                                       boolean dropContainer, 
                                       boolean actuallyPlaced) {
        BlockPosition blockPos = BlockPosition.fromWorldPosition(position);
        BlockStateHitBoxData data = new BlockStateHitBoxData(position, originalBlockState, dropContainer, actuallyPlaced);
        
        this.blockStateHitBoxPositions.add(blockPos);
        this.blockStateHitBoxDataMap.put(blockPos, data);
        
        // Save to PersistentDataContainer immediately
        this.saveBlockStateHitBoxDataToPDC();
    }

    @Override
    public boolean removeBlockStateHitBoxPosition(WorldPosition position) {
        BlockPosition blockPos = BlockPosition.fromWorldPosition(position);
        boolean removed = this.blockStateHitBoxPositions.remove(blockPos);
        if (removed) {
            this.blockStateHitBoxDataMap.remove(blockPos);
            // Save to PersistentDataContainer immediately
            this.saveBlockStateHitBoxDataToPDC();
        }
        return removed;
    }

    @Override
    public java.util.Collection<BlockPosition> getBlockStateHitBoxPositions() {
        return Collections.unmodifiableSet(this.blockStateHitBoxPositions);
    }

    @Override
    public boolean hasBlockStateHitBoxAt(WorldPosition position) {
        BlockPosition blockPos = BlockPosition.fromWorldPosition(position);
        // Check if the position is in our stored positions
        for (BlockPosition storedPos : this.blockStateHitBoxPositions) {
            if (storedPos.x() == blockPos.x() && 
                storedPos.y() == blockPos.y() && 
                storedPos.z() == blockPos.z() &&
                storedPos.world().name().equals(blockPos.world().name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Loads BlockStateHitBox data from the PersistentDataContainer
     */
    private void loadBlockStateHitBoxDataFromPDC() {
        try {
            Entity baseEntity = this.baseEntity();
            String dataString = baseEntity.getPersistentDataContainer().get(
                BukkitFurnitureManager.FURNITURE_BLOCKSTATE_HITBOX_DATA, 
                PersistentDataType.STRING
            );
            
            this.blockStateHitBoxPositions.clear();
            this.blockStateHitBoxDataMap.clear();
            
            if (dataString != null && !dataString.isEmpty()) {
                String[] dataStrings = dataString.split(";");
                int loadedCount = 0;
                int skippedCount = 0;
                
                for (String dataStr : dataStrings) {
                    if (dataStr.trim().isEmpty()) continue;
                    
                    BlockStateHitBoxData data = BlockStateHitBoxData.fromStorageString(
                        dataStr, 
                        LocationUtils.toWorldPosition(this.location).world()
                    );
                    
                    if (data != null) {
                        BlockPosition blockPos = BlockPosition.fromWorldPosition(data.getPosition());
                        this.blockStateHitBoxPositions.add(blockPos);
                        this.blockStateHitBoxDataMap.put(blockPos, data);
                        loadedCount++;
                    } else {
                        // Skip invalid or different-world data
                        skippedCount++;
                        CraftEngine.instance().logger().warn("Skipped invalid BlockStateHitBox data: " + dataStr + " for furniture: " + this.id());
                    }
                }
                
                if (loadedCount > 0) {
                    CraftEngine.instance().logger().info("Loaded " + loadedCount + 
                        " BlockStateHitBox data entries from PDC for furniture: " + this.id() +
                        (skippedCount > 0 ? " (skipped " + skippedCount + " invalid/different-world entries)" : ""));
                }
                
                // If we skipped any data, update the PDC to remove them
                if (skippedCount > 0) {
                    this.saveBlockStateHitBoxDataToPDC();
                }
            }
            
            // Legacy support: try to load from old format if new format is empty
            if (this.blockStateHitBoxDataMap.isEmpty()) {
                this.loadLegacyBlockStateHitBoxPositionsFromPDC();
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to load BlockStateHitBox data from PDC for furniture: " + this.id(), e);
        }
    }
    
    /**
     * Legacy method to load old format positions (for backwards compatibility)
     */
    private void loadLegacyBlockStateHitBoxPositionsFromPDC() {
        try {
            Entity baseEntity = this.baseEntity();
            String positionsData = baseEntity.getPersistentDataContainer().get(
                BukkitFurnitureManager.FURNITURE_BLOCKSTATE_HITBOX_POSITIONS, 
                PersistentDataType.STRING
            );
            
            if (positionsData != null && !positionsData.isEmpty()) {
                String[] positionStrings = positionsData.split(";");
                int loadedCount = 0;
                
                for (String posStr : positionStrings) {
                    if (posStr.trim().isEmpty()) continue;
                    try {
                        String[] parts = posStr.split(",", 4);
                        if (parts.length == 4) {
                            String worldName = parts[0];
                            int x = Integer.parseInt(parts[1]);
                            int y = Integer.parseInt(parts[2]);
                            int z = Integer.parseInt(parts[3]);
                            
                            // Check if the world name matches our current world
                            if (this.location.getWorld().getName().equals(worldName)) {
                                net.momirealms.craftengine.core.world.World world = LocationUtils.toWorldPosition(this.location).world();
                                WorldPosition worldPos = new WorldPosition(world, x, y, z);
                                BlockPosition blockPos = new BlockPosition(world, x, y, z);
                                
                                // Create default data for legacy format (assume air original state)
                                BlockStateHitBoxData data = new BlockStateHitBoxData(
                                    worldPos, 
                                    null, // No original state preserved in legacy format
                                    true, // Default to dropping containers
                                    true  // Assume it was actually placed
                                );
                                
                                this.blockStateHitBoxPositions.add(blockPos);
                                this.blockStateHitBoxDataMap.put(blockPos, data);
                                loadedCount++;
                            }
                        }
                    } catch (NumberFormatException e) {
                        CraftEngine.instance().logger().warn("Invalid legacy BlockPosition string in PDC: " + posStr, e);
                    }
                }
                
                if (loadedCount > 0) {
                    CraftEngine.instance().logger().info("Loaded " + loadedCount + 
                        " legacy BlockStateHitBox positions, converting to new format for furniture: " + this.id());
                    
                    // Convert legacy data to new format and save
                    this.saveBlockStateHitBoxDataToPDC();
                    
                    // Remove legacy data
                    baseEntity.getPersistentDataContainer().remove(BukkitFurnitureManager.FURNITURE_BLOCKSTATE_HITBOX_POSITIONS);
                }
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to load legacy BlockStateHitBox positions from PDC for furniture: " + this.id(), e);
        }
    }

    /**
     * Saves BlockStateHitBox data to the PersistentDataContainer
     */
    private void saveBlockStateHitBoxDataToPDC() {
        try {
            Entity baseEntity = this.baseEntity();
            
            if (this.blockStateHitBoxDataMap.isEmpty()) {
                // Remove the key if no data
                baseEntity.getPersistentDataContainer().remove(BukkitFurnitureManager.FURNITURE_BLOCKSTATE_HITBOX_DATA);
            } else {
                // Convert data to string format: "dataString1;dataString2;..."
                String dataString = this.blockStateHitBoxDataMap.values().stream()
                    .map(BlockStateHitBoxData::toStorageString)
                    .reduce((a, b) -> a + ";" + b)
                    .orElse("");
                
                baseEntity.getPersistentDataContainer().set(
                    BukkitFurnitureManager.FURNITURE_BLOCKSTATE_HITBOX_DATA,
                    PersistentDataType.STRING,
                    dataString
                );
            }
        } catch (Exception e) {
            CraftEngine.instance().logger().warn("Failed to save BlockStateHitBox data to PDC for furniture: " + this.id(), e);
        }
    }

    /**
     * Legacy method for backwards compatibility - now delegates to the new data method
     */
    private void saveBlockStateHitBoxPositionsToPDC() {
        this.saveBlockStateHitBoxDataToPDC();
    }

    /**
     * Initializes BlockStateHitBox positions by scanning the current hitboxes
     * This method should be called after the furniture is fully loaded
     */
    public void initializeBlockStateHitBoxPositions() {
        for (HitBox hitBox : hitBoxes.values()) {
            if (hitBox instanceof net.momirealms.craftengine.bukkit.entity.furniture.hitbox.BlockStateHitBox blockStateHitBox) {
                // The BlockStateHitBox will register itself when initialized
                // We just need to ensure it has the parent reference
                blockStateHitBox.setParentFurniture(this);
            }
        }
    }

    /**
     * Clean up orphaned BlockStateHitBox data that no longer correspond to actual blocks
     * This method can be called periodically or when issues are detected
     */
    @Override
    public void cleanupOrphanedBlockStateHitBoxPositions() {
        if (this.blockStateHitBoxDataMap.isEmpty()) return;
        
        int initialSize = this.blockStateHitBoxDataMap.size();
        this.blockStateHitBoxDataMap.entrySet().removeIf(entry -> {
            BlockPosition blockPos = entry.getKey();
            BlockStateHitBoxData data = entry.getValue();
            
            try {
                // Check if the block at this position still exists and belongs to this furniture
                WorldPosition worldPos = data.getPosition();
                Furniture furnitureAtPos = CraftEngine.instance().furnitureManager().getFurnitureByBlockPosition(worldPos);
                
                // Remove if no furniture is found at this position, or if it's a different furniture
                boolean shouldRemove = furnitureAtPos == null || !furnitureAtPos.uuid().equals(this.uuid());
                
                if (shouldRemove) {
                    CraftEngine.instance().logger().info("Removed orphaned BlockStateHitBox data: " + data.toStorageString() + 
                        " for furniture: " + this.id());
                    // Also remove from the positions set
                    this.blockStateHitBoxPositions.remove(blockPos);
                }
                
                return shouldRemove;
            } catch (Exception e) {
                // If there's an error checking the position, remove it to be safe
                CraftEngine.instance().logger().warn("Error checking BlockStateHitBox data " + data.toStorageString() + 
                    ", removing for safety", e);
                this.blockStateHitBoxPositions.remove(blockPos);
                return true;
            }
        });
        
        int removedCount = initialSize - this.blockStateHitBoxDataMap.size();
        if (removedCount > 0) {
            CraftEngine.instance().logger().info("Cleaned up " + removedCount + 
                " orphaned BlockStateHitBox data entries for furniture: " + this.id());
            // Save the cleaned up data to PDC
            this.saveBlockStateHitBoxDataToPDC();
        }
    }
}