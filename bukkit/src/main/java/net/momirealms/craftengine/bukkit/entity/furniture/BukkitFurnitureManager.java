package net.momirealms.craftengine.bukkit.entity.furniture;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.util.EntityUtils;
import net.momirealms.craftengine.core.entity.furniture.*;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.pack.Pack;
import net.momirealms.craftengine.core.plugin.config.ConfigManager;
import net.momirealms.craftengine.core.plugin.scheduler.SchedulerTask;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
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

public class BukkitFurnitureManager implements FurnitureManager {
    public static final NamespacedKey FURNITURE_KEY = Objects.requireNonNull(NamespacedKey.fromString("craftengine:furniture_id"));
    public static final NamespacedKey FURNITURE_ANCHOR_KEY = Objects.requireNonNull(NamespacedKey.fromString("craftengine:anchor_type"));
    public static final NamespacedKey FURNITURE_SEAT_BASE_ENTITY_KEY = Objects.requireNonNull(NamespacedKey.fromString("craftengine:seat_to_base_entity"));
    public static final NamespacedKey FURNITURE_SEAT_VECTOR_3F_KEY = Objects.requireNonNull(NamespacedKey.fromString("craftengine:seat_vector"));
    private static BukkitFurnitureManager instance;
    private final BukkitCraftEngine plugin;

    private final Map<Key, CustomFurniture> byId = new HashMap<>();

    private final Map<Integer, LoadedFurniture> furnitureByBaseEntityId  = new ConcurrentHashMap<>(256, 0.5f);
    private final Map<Integer, LoadedFurniture> furnitureByInteractionEntityId  = new ConcurrentHashMap<>(512, 0.5f);
    private final Map<Integer, int[]> baseEntity2SubEntities = new ConcurrentHashMap<>(256, 0.5f);

    // Delay furniture cache remove for about 4-5 ticks
    private static final int DELAYED_TICK = 5;
    private final IntSet[] delayedRemove = new IntSet[DELAYED_TICK];
    // Event listeners
    private final Listener dismountListener;
    private final FurnitureEventListener furnitureEventListener;
    // tick task
    private SchedulerTask tickTask;

    public static BukkitFurnitureManager instance() {
        return instance;
    }

    public BukkitFurnitureManager(BukkitCraftEngine plugin) {
        this.plugin = plugin;
        this.furnitureEventListener = new FurnitureEventListener(this);
        this.dismountListener = VersionHelper.isVersionNewerThan1_20_3() ? new DismountListener1_20_3(this) : new DismountListener1_20(this::handleDismount);
        for (int i = 0; i < DELAYED_TICK; i++) {
            this.delayedRemove[i] = new IntOpenHashSet();
        }
        instance = this;
    }

    public LoadedFurniture place(CustomFurniture furniture, Location location, AnchorType anchorType, boolean playSound) {
        Entity furnitureEntity = EntityUtils.spawnEntity(location.getWorld(), location, EntityType.ITEM_DISPLAY, entity -> {
            ItemDisplay display = (ItemDisplay) entity;
            display.getPersistentDataContainer().set(BukkitFurnitureManager.FURNITURE_KEY, PersistentDataType.STRING, furniture.id().toString());
            display.getPersistentDataContainer().set(BukkitFurnitureManager.FURNITURE_ANCHOR_KEY, PersistentDataType.STRING, anchorType.name());
            handleEntityLoadEarly(display);
        });
        if (playSound) {
            location.getWorld().playSound(location, furniture.settings().sounds().placeSound().toString(), SoundCategory.BLOCKS,1f, 1f);
        }
        return getLoadedFurnitureByBaseEntityId(furnitureEntity.getEntityId());
    }

    @SuppressWarnings("unchecked")
    @Override
    public void parseSection(Pack pack, Path path, Key id, Map<String, Object> section) {
        Map<String, Object> lootMap = MiscUtils.castToMap(section.get("loot"), true);
        Map<String, Object> settingsMap = MiscUtils.castToMap(section.get("settings"), true);
        Map<String, Object> placementMap = MiscUtils.castToMap(section.get("placement"), true);
        EnumMap<AnchorType, CustomFurniture.Placement> placements = new EnumMap<>(AnchorType.class);
        if (placementMap == null) {
            throw new IllegalArgumentException("Missing required parameter 'placement' for furniture_item behavior");
        }
        for (Map.Entry<String, Object> entry : placementMap.entrySet()) {
            AnchorType anchorType = AnchorType.valueOf(entry.getKey().toUpperCase(Locale.ENGLISH));
            Map<String, Object> placementArguments = MiscUtils.castToMap(entry.getValue(), true);

            List<FurnitureElement> elements = new ArrayList<>();
            List<Map<String, Object>> elementConfigs = (List<Map<String, Object>>) placementArguments.getOrDefault("elements", List.of());
            for (Map<String, Object> element : elementConfigs) {
                String key = (String) element.get("item");
                if (key == null) {
                    throw new IllegalArgumentException("Missing required parameter 'item' for furniture_item behavior");
                }
                ItemDisplayContext transform = ItemDisplayContext.valueOf(element.getOrDefault("transform", "NONE").toString().toUpperCase(Locale.ENGLISH));
                Billboard billboard = Billboard.valueOf(element.getOrDefault("billboard", "FIXED").toString().toUpperCase(Locale.ENGLISH));
                FurnitureElement furnitureElement = new FurnitureElement(Key.of(key), billboard, transform,
                        MiscUtils.getVector3f(element.getOrDefault("scale", "1")),
                        MiscUtils.getVector3f(element.getOrDefault("translation", "0")),
                        MiscUtils.getVector3f(element.getOrDefault("position", "0")),
                        MiscUtils.getQuaternionf(element.getOrDefault("rotation", "0"))
                );
                elements.add(furnitureElement);
            }
            List<Map<String, Object>> hitboxConfigs = (List<Map<String, Object>>) placementArguments.getOrDefault("hitboxes", List.of());
            List<HitBox> hitboxes = new ArrayList<>();
            for (Map<String, Object> config : hitboxConfigs) {
                List<String> seats = (List<String>) config.getOrDefault("seats", List.of());
                Seat[] seatArray = seats.stream()
                        .map(arg -> {
                            String[] split = arg.split(" ");
                            if (split.length == 1) return new Seat(MiscUtils.getVector3f(split[0]), 0, false);
                            return new Seat(MiscUtils.getVector3f(split[0]), Float.parseFloat(split[1]), true);
                        })
                        .toArray(Seat[]::new);
                Vector3f position = MiscUtils.getVector3f(config.getOrDefault("position", "0"));
                float width = MiscUtils.getAsFloat(config.getOrDefault("width", "1"));
                float height = MiscUtils.getAsFloat(config.getOrDefault("height", "1"));
                HitBox hitBox = new HitBox(
                        position,
                        new Vector3f(width, height, width),
                        seatArray,
                        (boolean) config.getOrDefault("interactive", true)
                );
                hitboxes.add(hitBox);
            }
            if (hitboxes.isEmpty()) {
                hitboxes.add(new HitBox(
                        new Vector3f(),
                        new Vector3f(1,1,1),
                        new Seat[0],
                        true
                ));
            }
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
                        alignmentRule
                ));
            } else {
                placements.put(anchorType, new CustomFurniture.Placement(
                        elements.toArray(new FurnitureElement[0]),
                        hitboxes.toArray(new HitBox[0]),
                        RotationRule.ANY,
                        AlignmentRule.CENTER
                ));
            }
        }

        CustomFurniture furniture = new CustomFurniture(
                id,
                FurnitureSettings.fromMap(settingsMap),
                placements,
                lootMap == null ? null : LootTable.fromMap(lootMap)
        );

        this.byId.put(id, furniture);
    }

    public void tick() {
        IntSet first = this.delayedRemove[0];
        for (int i : first) {
            // unloaded furniture might be loaded again
            LoadedFurniture furniture = getLoadedFurnitureByBaseEntityId(i);
            if (furniture == null)
                this.baseEntity2SubEntities.remove(i);
        }
        first.clear();
        for (int i = 1; i < DELAYED_TICK; i++) {
            this.delayedRemove[i - 1] = this.delayedRemove[i];
        }
        this.delayedRemove[DELAYED_TICK-1] = first;
    }

    @Override
    public void delayedLoad() {
        for (World world : Bukkit.getWorlds()) {
            List<Entity> entities = world.getEntities();
            for (Entity entity : entities) {
                handleEntityLoadEarly(entity);
            }
        }
    }

    @Override
    public void unload() {
        this.byId.clear();
        HandlerList.unregisterAll(this.dismountListener);
        HandlerList.unregisterAll(this.furnitureEventListener);
        if (tickTask != null && !tickTask.cancelled()) {
            tickTask.cancel();
        }
    }

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(this.dismountListener, this.plugin.bootstrap());
        Bukkit.getPluginManager().registerEvents(this.furnitureEventListener, this.plugin.bootstrap());
        this.tickTask = plugin.scheduler().sync().runRepeating(this::tick, 1, 1);
    }

    @Override
    public void disable() {
        unload();
        for (Player player : Bukkit.getOnlinePlayers()) {
            Entity vehicle = player.getVehicle();
            if (vehicle != null) {
                tryLeavingSeat(player, vehicle);
            }
        }
    }

    @Override
    public Optional<CustomFurniture> getFurniture(Key id) {
        return Optional.ofNullable(this.byId.get(id));
    }

    @Nullable
    @Override
    public int[] getSubEntityIdsByBaseEntityId(int entityId) {
        return this.baseEntity2SubEntities.get(entityId);
    }

    @Override
    public boolean isFurnitureBaseEntity(int entityId) {
        return this.furnitureByBaseEntityId.containsKey(entityId);
    }

    @Nullable
    public LoadedFurniture getLoadedFurnitureByBaseEntityId(int entityId) {
        return this.furnitureByBaseEntityId.get(entityId);
    }

    @Nullable
    public LoadedFurniture getLoadedFurnitureByInteractionEntityId(int entityId) {
        return this.furnitureByInteractionEntityId.get(entityId);
    }

    protected void handleEntityUnload(Entity entity) {
        int id = entity.getEntityId();
        LoadedFurniture furniture = this.furnitureByBaseEntityId.remove(id);
        if (furniture != null) {
            furniture.destroySeats();
            for (int sub : furniture.interactionEntityIds()) {
                this.furnitureByInteractionEntityId.remove(sub);
            }
            this.delayedRemove[DELAYED_TICK-1].add(id);
        }
    }

    @SuppressWarnings("deprecation") // just a misleading name `getTrackedPlayers`
    protected void handleEntityLoadLate(Entity entity) {
        if (entity instanceof ItemDisplay display) {
            String id = entity.getPersistentDataContainer().get(FURNITURE_KEY, PersistentDataType.STRING);
            if (id == null) return;
            Key key = Key.of(id);
            Optional<CustomFurniture> optionalFurniture = getFurniture(key);
            if (optionalFurniture.isEmpty()) return;
            CustomFurniture customFurniture = optionalFurniture.get();
            LoadedFurniture previous = this.furnitureByBaseEntityId.get(display.getEntityId());
            if (previous != null) return;
            LoadedFurniture furniture = addNewFurniture(display, customFurniture, getAnchorType(entity, customFurniture));
            for (Player player : display.getTrackedPlayers()) {
                this.plugin.networkManager().sendPacket(player, furniture.spawnPacket());
            }
        }
    }

    public void handleEntityLoadEarly(Entity entity) {
        if (entity instanceof ItemDisplay display) {
            String id = entity.getPersistentDataContainer().get(FURNITURE_KEY, PersistentDataType.STRING);
            if (id == null) return;
            Key key = Key.of(id);
            Optional<CustomFurniture> optionalFurniture = getFurniture(key);
            if (optionalFurniture.isPresent()) {
                CustomFurniture customFurniture = optionalFurniture.get();
                LoadedFurniture previous = this.furnitureByBaseEntityId.get(display.getEntityId());
                if (previous != null) return;
                addNewFurniture(display, customFurniture, getAnchorType(entity, customFurniture));
                return;
            }
            // Remove the entity if it's not a valid furniture
            if (ConfigManager.removeInvalidFurniture()) {
                if (ConfigManager.furnitureToRemove().isEmpty() || ConfigManager.furnitureToRemove().contains(id)) {
                    entity.remove();
                }
            }
        }
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
        this.furnitureByBaseEntityId.put(loadedFurniture.baseEntityId(), loadedFurniture);
        this.baseEntity2SubEntities.put(loadedFurniture.baseEntityId(), loadedFurniture.subEntityIds());
        for (int entityId : loadedFurniture.interactionEntityIds()) {
            this.furnitureByInteractionEntityId.put(entityId, loadedFurniture);
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
        LoadedFurniture furniture = getLoadedFurnitureByBaseEntityId(baseFurniture);
        if (furniture == null) {
            return;
        }
        String vector3f = vehicle.getPersistentDataContainer().get(BukkitFurnitureManager.FURNITURE_SEAT_VECTOR_3F_KEY, PersistentDataType.STRING);
        if (vector3f == null) {
            plugin.logger().warn("Failed to get vector3f for player " + player.getName() + "'s seat");
            return;
        }
        Vector3f seatPos = MiscUtils.getVector3f(vector3f);
        if (!furniture.releaseSeat(seatPos)) {
            plugin.logger().warn("Failed to release seat " + seatPos + " for player " + player.getName());
        }
    }

    protected boolean isSeatCarrierType(Entity entity) {
        return (entity instanceof ArmorStand || entity instanceof ItemDisplay);
    }
}
