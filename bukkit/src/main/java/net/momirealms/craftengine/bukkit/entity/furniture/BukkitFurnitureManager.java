package net.momirealms.craftengine.bukkit.entity.furniture;

import net.momirealms.craftengine.bukkit.compatibility.bettermodel.BetterModelModel;
import net.momirealms.craftengine.bukkit.compatibility.modelengine.ModelEngineModel;
import net.momirealms.craftengine.bukkit.entity.furniture.hitbox.InteractionHitBox;
import net.momirealms.craftengine.bukkit.nms.CollisionEntity;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.bukkit.util.Reflections;
import net.momirealms.craftengine.core.entity.furniture.*;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.pack.LoadingSequence;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.config.Config;
import net.momirealms.craftengine.core.plugin.config.ConfigSectionParser;
import net.momirealms.craftengine.core.plugin.locale.TranslationManager;
import net.momirealms.craftengine.core.sound.SoundData;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import net.momirealms.craftengine.core.world.Vec3d;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitFurnitureManager extends AbstractFurnitureManager {
    public static final NamespacedKey FURNITURE_KEY = Objects.requireNonNull(NamespacedKey.fromString("craftengine:furniture_id"));
    public static final NamespacedKey FURNITURE_ANCHOR_KEY = Objects.requireNonNull(NamespacedKey.fromString("craftengine:anchor_type"));
    public static final NamespacedKey FURNITURE_SEAT_BASE_ENTITY_KEY = Objects.requireNonNull(NamespacedKey.fromString("craftengine:seat_to_base_entity"));
    public static final NamespacedKey FURNITURE_SEAT_VECTOR_3F_KEY = Objects.requireNonNull(NamespacedKey.fromString("craftengine:seat_vector"));
    public static final NamespacedKey FURNITURE_COLLISION = Objects.requireNonNull(NamespacedKey.fromString("craftengine:collision"));
    private static BukkitFurnitureManager instance;
    private final BukkitCraftEngine plugin;
    private final FurnitureParser furnitureParser;
    private final Map<Integer, LoadedFurniture> furnitureByRealEntityId = new ConcurrentHashMap<>(256, 0.5f);
    private final Map<Integer, LoadedFurniture> furnitureByEntityId = new ConcurrentHashMap<>(512, 0.5f);
    // Event listeners
    private final Listener dismountListener;
    private final FurnitureEventListener furnitureEventListener;

    public static BukkitFurnitureManager instance() {
        return instance;
    }

    public BukkitFurnitureManager(BukkitCraftEngine plugin) {
        instance = this;
        this.plugin = plugin;
        this.furnitureParser = new FurnitureParser();
        this.furnitureEventListener = new FurnitureEventListener(this);
        this.dismountListener = VersionHelper.isVersionNewerThan1_20_3() ? new DismountListener1_20_3(this) : new DismountListener1_20(this::handleDismount);
    }

    @Override
    public Furniture place(CustomFurniture furniture, Vec3d vec3d, net.momirealms.craftengine.core.world.World world, AnchorType anchorType, boolean playSound) {
        return this.place(furniture, new Location((World) world.platformWorld(), vec3d.x(), vec3d.y(), vec3d.z()), anchorType, playSound);
    }

    public LoadedFurniture place(CustomFurniture furniture, Location location, AnchorType anchorType, boolean playSound) {
        if (furniture.isAllowedPlacement(anchorType)) {
            anchorType = furniture.getAnyPlacement();
        }
        AnchorType finalAnchorType = anchorType;
        Entity furnitureEntity = EntityUtils.spawnEntity(location.getWorld(), location, EntityType.ITEM_DISPLAY, entity -> {
            ItemDisplay display = (ItemDisplay) entity;
            display.getPersistentDataContainer().set(BukkitFurnitureManager.FURNITURE_KEY, PersistentDataType.STRING, furniture.id().toString());
            display.getPersistentDataContainer().set(BukkitFurnitureManager.FURNITURE_ANCHOR_KEY, PersistentDataType.STRING, finalAnchorType.name());
            handleBaseEntityLoadEarly(display);
        });
        if (playSound) {
            SoundData data = furniture.settings().sounds().placeSound();
            location.getWorld().playSound(location, data.id().toString(), SoundCategory.BLOCKS, data.volume(), data.pitch());
        }
        return loadedFurnitureByRealEntityId(furnitureEntity.getEntityId());
    }

    @Override
    public ConfigSectionParser parser() {
        return this.furnitureParser;
    }

    public class FurnitureParser implements ConfigSectionParser {
        public static final String[] CONFIG_SECTION_NAME = new String[] { "furniture" };

        @Override
        public String[] sectionId() {
            return CONFIG_SECTION_NAME;
        }

        @Override
        public int loadingSequence() {
            return LoadingSequence.FURNITURE;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
            if (byId.containsKey(id)) {
                TranslationManager.instance().log("warning.config.furniture.duplicated", path.toString(), id.toString());
                return;
            }

            Map<String, Object> lootMap = MiscUtils.castToMap(section.get("loot"), true);
            Map<String, Object> settingsMap = MiscUtils.castToMap(section.get("settings"), true);
            Map<String, Object> placementMap = MiscUtils.castToMap(section.get("placement"), true);
            EnumMap<AnchorType, CustomFurniture.Placement> placements = new EnumMap<>(AnchorType.class);
            if (placementMap == null) {
                TranslationManager.instance().log("warning.config.furniture.lack_placement", path.toString(), id.toString());
                return;
            }

            for (Map.Entry<String, Object> entry : placementMap.entrySet()) {
                // anchor type
                AnchorType anchorType = AnchorType.valueOf(entry.getKey().toUpperCase(Locale.ENGLISH));
                Map<String, Object> placementArguments = MiscUtils.castToMap(entry.getValue(), true);

                // furniture display elements
                List<FurnitureElement> elements = new ArrayList<>();
                List<Map<String, Object>> elementConfigs = (List<Map<String, Object>>) placementArguments.getOrDefault("elements", List.of());
                for (Map<String, Object> element : elementConfigs) {
                    String key = (String) element.get("item");
                    if (key == null) {
                        TranslationManager.instance().log("warning.config.furniture.element.lack_item", path.toString(), id.toString());
                        return;
                    }
                    ItemDisplayContext transform = ItemDisplayContext.valueOf(element.getOrDefault("transform", "NONE").toString().toUpperCase(Locale.ENGLISH));
                    Billboard billboard = Billboard.valueOf(element.getOrDefault("billboard", "FIXED").toString().toUpperCase(Locale.ENGLISH));
                    FurnitureElement furnitureElement = new BukkitFurnitureElement(Key.of(key), billboard, transform,
                            MiscUtils.getVector3f(element.getOrDefault("scale", "1")),
                            MiscUtils.getVector3f(element.getOrDefault("translation", "0")),
                            MiscUtils.getVector3f(element.getOrDefault("position", "0")),
                            MiscUtils.getQuaternionf(element.getOrDefault("rotation", "0"))
                    );
                    elements.add(furnitureElement);
                }

                // external model providers
                Optional<ExternalModel> externalModel;
                if (placementArguments.containsKey("model-engine")) {
                    externalModel = Optional.of(new ModelEngineModel(placementArguments.get("model-engine").toString()));
                } else if (placementArguments.containsKey("better-model")) {
                    externalModel = Optional.of(new BetterModelModel(placementArguments.get("better-model").toString()));
                } else {
                    externalModel = Optional.empty();
                }

                // add hitboxes
                List<Map<String, Object>> hitboxConfigs = (List<Map<String, Object>>) placementArguments.getOrDefault("hitboxes", List.of());
                List<HitBox> hitboxes = new ArrayList<>();
                for (Map<String, Object> config : hitboxConfigs) {
                    HitBox hitBox = HitBoxTypes.fromMap(config);
                    hitboxes.add(hitBox);
                }
                if (hitboxes.isEmpty() && externalModel.isEmpty()) {
                    hitboxes.add(InteractionHitBox.DEFAULT);
                }

                // rules
                Map<String, Object> ruleSection = MiscUtils.castToMap(placementArguments.get("rules"), true);
                if (ruleSection != null) {
                    RotationRule rotationRule = Optional.ofNullable((String) ruleSection.get("rotation"))
                            .map(it -> RotationRule.valueOf(it.toUpperCase(Locale.ENGLISH)))
                            .orElse(RotationRule.ANY);
                    AlignmentRule alignmentRule = Optional.ofNullable((String) ruleSection.get("alignment"))
                            .map(it -> AlignmentRule.valueOf(it.toUpperCase(Locale.ENGLISH)))
                            .orElse(AlignmentRule.CENTER);
                    placements.put(anchorType, new CustomFurniture.Placement(
                            elements.toArray(new FurnitureElement[0]),
                            hitboxes.toArray(new HitBox[0]),
                            rotationRule,
                            alignmentRule,
                            externalModel
                    ));
                } else {
                    placements.put(anchorType, new CustomFurniture.Placement(
                            elements.toArray(new FurnitureElement[0]),
                            hitboxes.toArray(new HitBox[0]),
                            RotationRule.ANY,
                            AlignmentRule.CENTER,
                            externalModel
                    ));
                }
            }

            CustomFurniture furniture = new CustomFurniture(
                    id,
                    FurnitureSettings.fromMap(settingsMap),
                    placements,
                    lootMap == null ? null : LootTable.fromMap(lootMap)
            );

            byId.put(id, furniture);
        }
    }

    @Override
    public void delayedInit() {
        Bukkit.getPluginManager().registerEvents(this.dismountListener, this.plugin.bootstrap());
        Bukkit.getPluginManager().registerEvents(this.furnitureEventListener, this.plugin.bootstrap());
        for (World world : Bukkit.getWorlds()) {
            List<Entity> entities = world.getEntities();
            for (Entity entity : entities) {
                if (entity instanceof ItemDisplay display) {
                    handleBaseEntityLoadEarly(display);
                } else if (entity instanceof Boat boat) {
                    handleCollisionEntityLoadOnEntitiesLoad(boat);
                }
            }
        }
    }

    @Override
    public void disable() {
        HandlerList.unregisterAll(this.dismountListener);
        HandlerList.unregisterAll(this.furnitureEventListener);
        unload();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Entity vehicle = player.getVehicle();
            if (vehicle != null) {
                tryLeavingSeat(player, vehicle);
            }
        }
    }

    @Override
    public boolean isFurnitureRealEntity(int entityId) {
        return this.furnitureByRealEntityId.containsKey(entityId);
    }

    @Nullable
    @Override
    public LoadedFurniture loadedFurnitureByRealEntityId(int entityId) {
        return this.furnitureByRealEntityId.get(entityId);
    }

    @Override
    @Nullable
    public LoadedFurniture loadedFurnitureByEntityId(int entityId) {
        return this.furnitureByEntityId.get(entityId);
    }

    protected void handleBaseEntityUnload(Entity entity) {
        int id = entity.getEntityId();
        LoadedFurniture furniture = this.furnitureByRealEntityId.remove(id);
        if (furniture != null) {
            Location location = entity.getLocation();
            boolean isPreventing = FastNMS.INSTANCE.isPreventingStatusUpdates(location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
            if (!isPreventing) {
                furniture.destroySeats();
            }
            for (int sub : furniture.entityIds()) {
                this.furnitureByEntityId.remove(sub);
            }
        }
    }

    protected void handleCollisionEntityUnload(Entity entity) {
        int id = entity.getEntityId();
        this.furnitureByRealEntityId.remove(id);
    }

    @SuppressWarnings("deprecation") // just a misleading name `getTrackedPlayers`
    protected void handleBaseEntityLoadLate(ItemDisplay display, int depth) {
        // must be a furniture item
        String id = display.getPersistentDataContainer().get(FURNITURE_KEY, PersistentDataType.STRING);
        if (id == null) return;

        Key key = Key.of(id);
        Optional<CustomFurniture> optionalFurniture = furnitureById(key);
        if (optionalFurniture.isEmpty()) return;

        CustomFurniture customFurniture = optionalFurniture.get();
        LoadedFurniture previous = this.furnitureByRealEntityId.get(display.getEntityId());
        if (previous != null) return;

        Location location = display.getLocation();
        boolean above1_20_1 = VersionHelper.isVersionNewerThan1_20_2();
        boolean preventChange = FastNMS.INSTANCE.isPreventingStatusUpdates(location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
        if (above1_20_1) {
            if (!preventChange) {
                LoadedFurniture furniture = addNewFurniture(display, customFurniture, getAnchorType(display, customFurniture));
                furniture.initializeColliders();
                for (Player player : display.getTrackedPlayers()) {
                    this.plugin.adapt(player).furnitureView().computeIfAbsent(furniture.baseEntityId(), k -> new ArrayList<>()).addAll(furniture.fakeEntityIds());
                    this.plugin.networkManager().sendPacket(player, furniture.spawnPacket(player));
                }
            }
        } else {
            LoadedFurniture furniture = addNewFurniture(display, customFurniture, getAnchorType(display, customFurniture));
            for (Player player : display.getTrackedPlayers()) {
                this.plugin.adapt(player).furnitureView().computeIfAbsent(furniture.baseEntityId(), k -> new ArrayList<>()).addAll(furniture.fakeEntityIds());
                this.plugin.networkManager().sendPacket(player, furniture.spawnPacket(player));
            }
            if (preventChange) {
                this.plugin.scheduler().sync().runLater(furniture::initializeColliders, 1, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
            } else {
                furniture.initializeColliders();
            }
        }
        if (depth > 2) return;
        this.plugin.scheduler().sync().runLater(() -> handleBaseEntityLoadLate(display, depth + 1), 1, location.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    protected void handleCollisionEntityLoadLate(Boat boat, int depth) {
        // remove the boat if it's not a collision entity, it might be wrongly copied by WorldEdit
        if (FastNMS.INSTANCE.method$CraftEntity$getHandle(boat) instanceof CollisionEntity) {
            return;
        }
        // not a collision entity
        Byte flag = boat.getPersistentDataContainer().get(FURNITURE_COLLISION, PersistentDataType.BYTE);
        if (flag == null || flag != 1) {
            return;
        }

        Location location = boat.getLocation();
        World world = location.getWorld();
        int chunkX = location.getBlockX() >> 4;
        int chunkZ = location.getBlockZ() >> 4;
        if (!FastNMS.INSTANCE.isPreventingStatusUpdates(world, chunkX, chunkZ)) {
            boat.remove();
            return;
        }

        if (depth > 2) return;
        plugin.scheduler().sync().runLater(() -> {
            handleCollisionEntityLoadLate(boat, depth + 1);
        }, 1, world, chunkX, chunkZ);
    }

    public void handleBaseEntityLoadEarly(ItemDisplay display) {
        String id = display.getPersistentDataContainer().get(FURNITURE_KEY, PersistentDataType.STRING);
        if (id == null) return;
        // Remove the entity if it's not a valid furniture
        if (Config.handleInvalidFurniture()) {
            String mapped = Config.furnitureMappings().get(id);
            if (mapped != null) {
                if (mapped.isEmpty()) {
                    display.remove();
                    return;
                } else {
                    id = mapped;
                    display.getPersistentDataContainer().set(FURNITURE_KEY, PersistentDataType.STRING, id);
                }
            }
        }

        Key key = Key.of(id);
        Optional<CustomFurniture> optionalFurniture = furnitureById(key);
        if (optionalFurniture.isPresent()) {
            CustomFurniture customFurniture = optionalFurniture.get();
            LoadedFurniture previous = this.furnitureByRealEntityId.get(display.getEntityId());
            if (previous != null) return;
            LoadedFurniture furniture = addNewFurniture(display, customFurniture, getAnchorType(display, customFurniture));
            furniture.initializeColliders(); // safely do it here
        }
    }

    public void handleCollisionEntityLoadOnEntitiesLoad(Boat boat) {
        // faster
        if (FastNMS.INSTANCE.method$CraftEntity$getHandle(boat) instanceof CollisionEntity) {
            boat.remove();
            return;
        }

        // not a collision entity
        Byte flag = boat.getPersistentDataContainer().get(FURNITURE_COLLISION, PersistentDataType.BYTE);
        if (flag == null || flag != 1) {
            return;
        }

        boat.remove();
    }

    private AnchorType getAnchorType(Entity baseEntity, CustomFurniture furniture) {
        String anchorType = baseEntity.getPersistentDataContainer().get(FURNITURE_ANCHOR_KEY, PersistentDataType.STRING);
        if (anchorType != null) {
            try {
                AnchorType unverified = AnchorType.valueOf(anchorType);
                if (furniture.isAllowedPlacement(unverified)) {
                    return unverified;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        AnchorType anchorTypeEnum = furniture.getAnyPlacement();
        baseEntity.getPersistentDataContainer().set(FURNITURE_ANCHOR_KEY, PersistentDataType.STRING, anchorTypeEnum.name());
        return anchorTypeEnum;
    }

    private synchronized LoadedFurniture addNewFurniture(ItemDisplay display, CustomFurniture furniture, AnchorType anchorType) {
        LoadedFurniture loadedFurniture = new LoadedFurniture(display, furniture, anchorType);
        this.furnitureByRealEntityId.put(loadedFurniture.baseEntityId(), loadedFurniture);
        for (int entityId : loadedFurniture.entityIds()) {
            this.furnitureByEntityId.put(entityId, loadedFurniture);
        }
        for (Collider collisionEntity : loadedFurniture.collisionEntities()) {
            int collisionEntityId = FastNMS.INSTANCE.method$Entity$getId(collisionEntity.handle());
            this.furnitureByRealEntityId.put(collisionEntityId, loadedFurniture);
        }
        return loadedFurniture;
    }

    protected void handleDismount(Player player, Entity entity) {
        if (!isSeatCarrierType(entity)) return;
        Location location = entity.getLocation();
        plugin.scheduler().sync().runDelayed(() -> tryLeavingSeat(player, entity), player.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    protected void tryLeavingSeat(@NotNull Player player, @NotNull Entity vehicle) {
        Integer baseFurniture = vehicle.getPersistentDataContainer().get(FURNITURE_SEAT_BASE_ENTITY_KEY, PersistentDataType.INTEGER);
        if (baseFurniture == null) return;
        vehicle.remove();
        LoadedFurniture furniture = loadedFurnitureByRealEntityId(baseFurniture);
        if (furniture == null) {
            return;
        }
        String vector3f = vehicle.getPersistentDataContainer().get(BukkitFurnitureManager.FURNITURE_SEAT_VECTOR_3F_KEY, PersistentDataType.STRING);
        if (vector3f == null) {
            plugin.logger().warn("Failed to get vector3f for player " + player.getName() + "'s seat");
            return;
        }
        Vector3f seatPos = MiscUtils.getVector3f(vector3f);
        furniture.removeOccupiedSeat(seatPos);

        if (player.getVehicle() != null) return;
        Location vehicleLocation = vehicle.getLocation();
        Location originalLocation = vehicleLocation.clone();
        originalLocation.setY(furniture.location().getY());
        Location targetLocation = originalLocation.clone().add(vehicleLocation.getDirection().multiply(1.1));
        if (!isSafeLocation(targetLocation)) {
            targetLocation = findSafeLocationNearby(originalLocation);
            if (targetLocation == null) return;
        }
        targetLocation.setYaw(player.getLocation().getYaw());
        targetLocation.setPitch(player.getLocation().getPitch());
        player.teleport(targetLocation);
    }

    protected boolean isSeatCarrierType(Entity entity) {
        return (entity instanceof ArmorStand || entity instanceof ItemDisplay);
    }

    private boolean isSafeLocation(Location location) {
        World world = location.getWorld();
        if (world == null) return false;
        int x = location.getBlockX();
        int y = location.getBlockY();
        int z = location.getBlockZ();
        if (!world.getBlockAt(x, y - 1, z).getType().isSolid()) return false;
        if (!world.getBlockAt(x, y, z).isPassable()) return false;
        if (isEntityBlocking(location)) return false;
        return world.getBlockAt(x, y + 1, z).isPassable();
    }

    @Nullable
    private Location findSafeLocationNearby(Location center) {
        World world = center.getWorld();
        if (world == null) return null;
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) continue;
                int x = centerX + dx;
                int z = centerZ + dz;
                Location nearbyLocation = new Location(world, x + 0.5, centerY, z + 0.5);
                if (isSafeLocation(nearbyLocation)) return nearbyLocation;
            }
        }
        return null;
    }

    private boolean isEntityBlocking(Location location) {
        World world = location.getWorld();
        if (world == null) return true;
        try {
            Collection<Entity> nearbyEntities = world.getNearbyEntities(location, 0.38, 2, 0.38);
            for (Entity bukkitEntity : nearbyEntities) {
                if (bukkitEntity instanceof Player) continue;
                Object nmsEntity = FastNMS.INSTANCE.method$CraftEntity$getHandle(bukkitEntity);
                return (boolean) Reflections.method$Entity$canBeCollidedWith.invoke(nmsEntity);
            }
        } catch (Exception ignored) {}
        return false;
    }
}
