package net.momirealms.craftengine.bukkit.entity.furniture;

import com.destroystokyo.paper.event.entity.EntityAddToWorldEvent;
import com.destroystokyo.paper.event.entity.EntityRemoveFromWorldEvent;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.momirealms.craftengine.bukkit.item.behavior.FurnitureItemBehavior;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.core.entity.furniture.AnchorType;
import net.momirealms.craftengine.core.entity.furniture.FurnitureManager;
import net.momirealms.craftengine.core.item.CustomItem;
import net.momirealms.craftengine.core.plugin.config.ConfigManager;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.util.MiscUtils;
import net.momirealms.craftengine.core.util.VersionHelper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.event.world.EntitiesLoadEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class BukkitFurnitureManager implements FurnitureManager, Listener {
    public static final NamespacedKey FURNITURE_KEY = Objects.requireNonNull(NamespacedKey.fromString("craftengine:furniture_id"));
    public static final NamespacedKey FURNITURE_ANCHOR_KEY = Objects.requireNonNull(NamespacedKey.fromString("craftengine:anchor_type"));
    public static final NamespacedKey FURNITURE_SEAT_BASE_ENTITY_KEY = Objects.requireNonNull(NamespacedKey.fromString("craftengine:seat_to_base_entity"));
    public static final NamespacedKey FURNITURE_SEAT_VECTOR_3F_KEY = Objects.requireNonNull(NamespacedKey.fromString("craftengine:seat_vector"));
    private static BukkitFurnitureManager instance;
    private final BukkitCraftEngine plugin;
    private final Listener dismountListener;
    private final Map<Integer, LoadedFurniture> furnitureByBaseEntityId;
    private final Map<Integer, LoadedFurniture> furnitureByInteractionEntityId;
    private final Map<Integer, int[]> baseEntity2SubEntities;
    private static final int DELAYED_TICK = 5;
    // Delay furniture cache remove for about 4-5 ticks
    private final IntSet[] delayedRemove = new IntSet[DELAYED_TICK];

    public BukkitFurnitureManager(BukkitCraftEngine plugin) {
        this.plugin = plugin;
        this.furnitureByBaseEntityId = new ConcurrentHashMap<>(256, 0.5f);
        this.furnitureByInteractionEntityId = new ConcurrentHashMap<>(512, 0.5f);
        this.baseEntity2SubEntities = new ConcurrentHashMap<>(256, 0.5f);
        this.dismountListener = VersionHelper.isVersionNewerThan1_20_3() ? new DismountListener1_20_3() : new DismountListener1_20(this::handleDismount);
        for (int i = 0; i < DELAYED_TICK; i++) {
            this.delayedRemove[i] = new IntOpenHashSet();
        }
        instance = this;
    }

    public void tick() {
        IntSet first = delayedRemove[0];
        for (int i : first) {
            // unloaded furniture might be loaded again
            LoadedFurniture furniture = getLoadedFurnitureByBaseEntityId(i);
            if (furniture == null)
                this.baseEntity2SubEntities.remove(i);
        }
        first.clear();
        for (int i = 1; i < DELAYED_TICK; i++) {
            delayedRemove[i - 1] = delayedRemove[i];
        }
        delayedRemove[DELAYED_TICK-1] = first;
    }

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
        HandlerList.unregisterAll(this);
        HandlerList.unregisterAll(this.dismountListener);
    }

    @Override
    public void load() {
        Bukkit.getPluginManager().registerEvents(this, plugin.bootstrap());
        Bukkit.getPluginManager().registerEvents(this.dismountListener, plugin.bootstrap());
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

    @Nullable
    public int[] getSubEntityIdsByBaseEntityId(int entityId) {
        return this.baseEntity2SubEntities.get(entityId);
    }

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

    public static BukkitFurnitureManager instance() {
        return instance;
    }

    /*
     * Load Entities
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntitiesLoadEarly(EntitiesLoadEvent event) {
        List<Entity> entities = event.getEntities();
        for (Entity entity : entities) {
            handleEntityLoadEarly(entity);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onWorldLoad(WorldLoadEvent event) {
        List<Entity> entities = event.getWorld().getEntities();
        for (Entity entity : entities) {
            handleEntityLoadEarly(entity);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onEntityLoad(EntityAddToWorldEvent event) {
        handleEntityLoadLate(event.getEntity());
    }

    /*
     * Unload Entities
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onChunkUnload(ChunkUnloadEvent event) {
        Entity[] entities = event.getChunk().getEntities();
        for (Entity entity : entities) {
            handleEntityUnload(entity);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onWorldUnload(WorldUnloadEvent event) {
        List<Entity> entities = event.getWorld().getEntities();
        for (Entity entity : entities) {
            handleEntityUnload(entity);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onEntityUnload(EntityRemoveFromWorldEvent event) {
        handleEntityUnload(event.getEntity());
    }

    private void handleEntityUnload(Entity entity) {
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

    @SuppressWarnings("deprecation")
    public void handleEntityLoadLate(Entity entity) {
        if (entity instanceof ItemDisplay display) {
            String id = entity.getPersistentDataContainer().get(FURNITURE_KEY, PersistentDataType.STRING);
            if (id == null) return;
            Key key = Key.of(id);
            Optional<CustomItem<ItemStack>> optionalItem = plugin.itemManager().getCustomItem(key);
            if (optionalItem.isEmpty()) return;
            CustomItem<ItemStack> customItem = optionalItem.get();
            if (!(customItem.behavior() instanceof FurnitureItemBehavior behavior)) return;
            LoadedFurniture previous = this.furnitureByBaseEntityId.get(display.getEntityId());
            if (previous != null) return;
            Quaternionf rotation = display.getTransformation().getRightRotation();
            LoadedFurniture furniture = addNewFurniture(display, key, behavior, getAnchorType(entity, behavior), rotation);
            for (Player player : display.getTrackedPlayers()) {
                plugin.networkManager().sendPacket(player, furniture.spawnPacket());
            }
        }
    }

    public void handleEntityLoadEarly(Entity entity) {
        if (entity instanceof ItemDisplay display) {
            String id = entity.getPersistentDataContainer().get(FURNITURE_KEY, PersistentDataType.STRING);
            if (id == null) return;
            Key key = Key.of(id);
            Optional<CustomItem<ItemStack>> optionalItem = plugin.itemManager().getCustomItem(key);
            if (optionalItem.isPresent()) {
                Quaternionf rotation = display.getTransformation().getRightRotation();
                CustomItem<ItemStack> customItem = optionalItem.get();
                if (customItem.behavior() instanceof FurnitureItemBehavior behavior) {
                    LoadedFurniture previous = this.furnitureByBaseEntityId.get(display.getEntityId());
                    if (previous != null) return;
                    addNewFurniture(display, key, behavior, getAnchorType(entity, behavior), rotation);
                    return;
                }
            }
            // Remove the entity if it's not a valid furniture
            if (ConfigManager.removeInvalidFurniture()) {
                if (ConfigManager.furnitureToRemove().isEmpty() || ConfigManager.furnitureToRemove().contains(id)) {
                    entity.remove();
                }
            }
        }
    }

    private AnchorType getAnchorType(Entity entity, FurnitureItemBehavior behavior) {
        String anchorType = entity.getPersistentDataContainer().get(FURNITURE_ANCHOR_KEY, PersistentDataType.STRING);
        if (anchorType != null) {
            try {
                AnchorType unverified = AnchorType.valueOf(anchorType);
                if (behavior.isAllowedPlacement(unverified)) {
                    return unverified;
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        AnchorType anchorTypeEnum = behavior.getAnyPlacement();
        entity.getPersistentDataContainer().set(FURNITURE_ANCHOR_KEY, PersistentDataType.STRING, anchorTypeEnum.name());
        return anchorTypeEnum;
    }

    private synchronized LoadedFurniture addNewFurniture(ItemDisplay display, Key key, FurnitureItemBehavior behavior, AnchorType anchorType, Quaternionf rotation) {
        Location location = display.getLocation();
        LoadedFurniture furniture = new LoadedFurniture(key, display, behavior, anchorType, new Vector3d(location.getX(), location.getY(), location.getZ()), rotation);
        this.furnitureByBaseEntityId.put(furniture.baseEntityId(), furniture);
        this.baseEntity2SubEntities.put(furniture.baseEntityId(), furniture.subEntityIds());
        for (int entityId : furniture.interactionEntityIds()) {
            this.furnitureByInteractionEntityId.put(entityId, furniture);
        }
        return furniture;
    }

    public class DismountListener1_20_3 implements Listener {
        @EventHandler(ignoreCancelled = true)
        public void onDismount(EntityDismountEvent event) {
            if (event.getEntity() instanceof Player player) {
                handleDismount(player, event.getDismounted());
            }
        }
    }

    public void handleDismount(Player player, Entity entity) {
        if (!isSeatCarrier(entity)) return;
        Location location = entity.getLocation();
        plugin.scheduler().sync().runDelayed(() -> tryLeavingSeat(player, entity), player.getWorld(), location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Entity entity = player.getVehicle();
        if (entity == null) return;
        if (isSeatCarrier(entity)) {
            tryLeavingSeat(player, entity);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        Entity entity = player.getVehicle();
        if (entity == null) return;
        if (isSeatCarrier(entity)) {
            tryLeavingSeat(player, entity);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onInteractArmorStand(PlayerInteractAtEntityEvent event) {
        Entity clicked = event.getRightClicked();
        if (clicked instanceof ArmorStand armorStand) {
            Integer baseFurniture = armorStand.getPersistentDataContainer().get(FURNITURE_SEAT_BASE_ENTITY_KEY, PersistentDataType.INTEGER);
            if (baseFurniture == null) return;
            event.setCancelled(true);
        }
    }

    private void tryLeavingSeat(@NotNull Player player, @NotNull Entity vehicle) {
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

    public boolean isSeatCarrier(Entity entity) {
        return (entity instanceof ArmorStand || entity instanceof ItemDisplay);
    }
}
