package net.momirealms.craftengine.bukkit.api;

import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurniture;
import net.momirealms.craftengine.bukkit.entity.furniture.BukkitFurnitureManager;
import net.momirealms.craftengine.bukkit.entity.seat.BukkitSeatManager;
import net.momirealms.craftengine.bukkit.nms.CollisionEntity;
import net.momirealms.craftengine.bukkit.nms.FastNMS;
import net.momirealms.craftengine.bukkit.plugin.BukkitCraftEngine;
import net.momirealms.craftengine.bukkit.plugin.user.BukkitServerPlayer;
import net.momirealms.craftengine.core.entity.furniture.AnchorType;
import net.momirealms.craftengine.core.entity.furniture.CustomFurniture;
import net.momirealms.craftengine.core.entity.furniture.Furniture;
import net.momirealms.craftengine.core.entity.furniture.FurnitureDataAccessor;
import net.momirealms.craftengine.core.entity.player.InteractionHand;
import net.momirealms.craftengine.core.item.Item;
import net.momirealms.craftengine.core.loot.LootTable;
import net.momirealms.craftengine.core.plugin.context.ContextHolder;
import net.momirealms.craftengine.core.plugin.context.parameter.DirectContextParameters;
import net.momirealms.craftengine.core.util.Key;
import net.momirealms.craftengine.core.world.World;
import net.momirealms.craftengine.core.world.WorldPosition;
import net.momirealms.sparrow.nbt.CompoundTag;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public final class CraftEngineFurniture {

    private CraftEngineFurniture() {}

    /**
     * Returns an unmodifiable map of all currently loaded custom furniture.
     * The map keys represent unique identifiers, and the values are the corresponding CustomFurniture instances.
     *
     * <p><strong>Important:</strong> Do not attempt to access this method during the onEnable phase
     * as it will be empty. Instead, listen for the {@code CraftEngineReloadEvent} and use this method
     * after the event is fired to obtain the complete furniture list.
     *
     * @return a non-null map containing all loaded custom furniture
     */
    @NotNull
    public static Map<Key, CustomFurniture> loadedFurniture() {
        return BukkitFurnitureManager.instance().loadedFurniture();
    }

    /**
     * Gets custom furniture by ID
     *
     * @param id id
     * @return the custom furniture
     */
    public static CustomFurniture byId(@NotNull Key id) {
        return BukkitFurnitureManager.instance().furnitureById(id).orElse(null);
    }

    /**
     * Performs ray tracing to find the furniture entity that the player is currently targeting
     *
     * @param player The player performing the ray trace
     * @param maxDistance Maximum ray trace distance (in blocks)
     * @return The furniture being targeted by the player, or null if no furniture is found
     */
    @Nullable
    public static BukkitFurniture rayTrace(Player player, double maxDistance) {
        BukkitServerPlayer serverPlayer = BukkitAdaptors.adapt(player);
        if (serverPlayer == null) return null;
        Location eyeLocation = serverPlayer.getEyeLocation();
        RayTraceResult result = player.getWorld().rayTrace(eyeLocation, eyeLocation.getDirection(), maxDistance, FluidCollisionMode.NEVER, true, 0d, CraftEngineFurniture::isCollisionEntity);
        if (result == null)
            return null;
        Entity hitEntity = result.getHitEntity();
        if (hitEntity == null)
            return null;
        return getLoadedFurnitureByCollider(hitEntity);
    }

    /**
     * Performs ray tracing to find the furniture entity that the player is currently targeting
     *
     * @param player The player performing the ray trace
     * @return The furniture being targeted by the player, or null if no furniture is found
     */
    @Nullable
    public static BukkitFurniture rayTrace(Player player) {
        BukkitServerPlayer serverPlayer = BukkitAdaptors.adapt(player);
        if (serverPlayer == null) return null;
        Location eyeLocation = serverPlayer.getEyeLocation();
        RayTraceResult result = player.getWorld().rayTrace(eyeLocation, eyeLocation.getDirection(), serverPlayer.getCachedInteractionRange(), FluidCollisionMode.NEVER, true, 0d, CraftEngineFurniture::isCollisionEntity);
        if (result == null)
            return null;
        Entity hitEntity = result.getHitEntity();
        if (hitEntity == null)
            return null;
        return getLoadedFurnitureByCollider(hitEntity);
    }

    /**
     * Places furniture at certain location
     *
     * @param location    location
     * @param furnitureId furniture to place
     * @return the loaded furniture
     */
    @Nullable
    public static BukkitFurniture place(Location location, Key furnitureId) {
        CustomFurniture furniture = byId(furnitureId);
        if (furniture == null) return null;
        return place(location, furniture, furniture.anyVariantName(), false);
    }

    /**
     * Places furniture at certain location
     *
     * @param location    location
     * @param furnitureId furniture to place
     * @param anchorType  anchor type
     * @return the loaded furniture
     */
    @Nullable
    @Deprecated(since = "0.0.66", forRemoval = true)
    public static BukkitFurniture place(Location location, Key furnitureId, AnchorType anchorType) {
        return place(location, furnitureId, anchorType.variantName());
    }

    /**
     * Places furniture at certain location
     *
     * @param location    location
     * @param furnitureId furniture to place
     * @param variant  variant type
     * @return the loaded furniture
     */
    @Nullable
    public static BukkitFurniture place(Location location, Key furnitureId, String variant) {
        CustomFurniture furniture = byId(furnitureId);
        if (furniture == null) return null;
        return BukkitFurnitureManager.instance().place(location, furniture, FurnitureDataAccessor.ofVariant(variant), true);
    }

    /**
     * Places furniture at certain location
     *
     * @param location   location
     * @param furniture  furniture to place
     * @param anchorType anchor type
     * @return the loaded furniture
     */
    @NotNull
    @Deprecated(since = "0.0.66", forRemoval = true)
    public static BukkitFurniture place(Location location, CustomFurniture furniture, AnchorType anchorType) {
        return place(location, furniture, anchorType.variantName(), true);
    }

    /**
     * Places furniture at certain location
     *
     * @param location    location
     * @param furnitureId furniture to place
     * @param anchorType  anchor type
     * @param playSound   whether to play place sounds
     * @return the loaded furniture
     */
    @Nullable
    @Deprecated(since = "0.0.66", forRemoval = true)
    public static BukkitFurniture place(Location location, Key furnitureId, AnchorType anchorType, boolean playSound) {
        CustomFurniture furniture = byId(furnitureId);
        if (furniture == null) return null;
        return place(location, furniture, anchorType.variantName(), playSound);
    }

    /**
     * Places furniture at certain location
     *
     * @param location    location
     * @param furnitureId furniture to place
     * @param variant     variant
     * @param playSound   whether to play place sounds
     * @return the loaded furniture
     */
    @Nullable
    public static BukkitFurniture place(Location location, Key furnitureId, String variant, boolean playSound) {
        CustomFurniture furniture = byId(furnitureId);
        if (furniture == null) return null;
        return place(location, furniture, variant, playSound);
    }

    /**
     * Places furniture at certain location
     *
     * @param location   location
     * @param furniture  furniture to place
     * @param anchorType anchor type
     * @param playSound  whether to play place sounds
     * @return the loaded furniture
     */
    @NotNull
    @Deprecated(since = "0.0.66", forRemoval = true)
    public static BukkitFurniture place(Location location, CustomFurniture furniture, AnchorType anchorType, boolean playSound) {
        return place(location, furniture, anchorType.variantName(), playSound);
    }

    /**
     * Places furniture at certain location
     *
     * @param location   location
     * @param furniture  furniture to place
     * @param variant    variant
     * @param playSound  whether to play place sounds
     * @return the loaded furniture
     */
    @NotNull
    public static BukkitFurniture place(Location location, CustomFurniture furniture, String variant, boolean playSound) {
        return BukkitFurnitureManager.instance().place(location, furniture, FurnitureDataAccessor.ofVariant(variant), playSound);
    }

    /**
     * Places furniture at certain location
     *
     * @param location   location
     * @param furniture  furniture to place
     * @param data       furniture data
     * @param playSound  whether to play place sounds
     * @return the loaded furniture
     */
    @NotNull
    public static BukkitFurniture place(Location location, CustomFurniture furniture, CompoundTag data, boolean playSound) {
        return BukkitFurnitureManager.instance().place(location, furniture, FurnitureDataAccessor.of(data), playSound);
    }

    /**
     * Places furniture at certain location
     *
     * @param location   location
     * @param furniture  furniture to place
     * @param dataAccessor furniture data accessor
     * @param playSound  whether to play place sounds
     * @return the loaded furniture
     */
    @NotNull
    public static BukkitFurniture place(Location location, CustomFurniture furniture, FurnitureDataAccessor dataAccessor, boolean playSound) {
        return BukkitFurnitureManager.instance().place(location, furniture, dataAccessor, playSound);
    }

    /**
     * Check if an entity is a piece of furniture
     *
     * @param entity entity to check
     * @return is furniture or not
     */
    public static boolean isFurniture(@NotNull Entity entity) {
        String furnitureId = entity.getPersistentDataContainer().get(BukkitFurnitureManager.FURNITURE_KEY, PersistentDataType.STRING);
        return furnitureId != null;
    }

    /**
     * Check if an entity is a collision entity
     *
     * @param entity entity to check
     * @return is collision entity or not
     */
    public static boolean isCollisionEntity(@NotNull Entity entity) {
        Object nmsEntity = FastNMS.INSTANCE.method$CraftEntity$getHandle(entity);
        return nmsEntity instanceof CollisionEntity;
    }

    /**
     * Check if an entity is a seat
     *
     * @param entity entity to check
     * @return is seat or not
     */
    public static boolean isSeat(@NotNull Entity entity) {
        return entity.getPersistentDataContainer().has(BukkitSeatManager.SEAT_KEY);
    }

    /**
     * Gets the furniture by the meta entity
     *
     * @param baseEntity base entity
     * @return the loaded furniture
     */
    @Nullable
    public static BukkitFurniture getLoadedFurnitureByMetaEntity(@NotNull Entity baseEntity) {
        return BukkitFurnitureManager.instance().loadedFurnitureByMetaEntityId(baseEntity.getEntityId());
    }

    /**
     * Gets the furniture by the meta entity
     *
     * @param baseEntity base entity
     * @return the loaded furniture
     */
    @Nullable
    @Deprecated(since = "0.0.66")
    public static BukkitFurniture getLoadedFurnitureByBaseEntity(@NotNull Entity baseEntity) {
        return getLoadedFurnitureByMetaEntity(baseEntity);
    }

    /**
     * Gets the furniture by the seat entity
     *
     * @param seat seat entity
     * @return the loaded furniture
     */
    @Nullable
    public static BukkitFurniture getLoadedFurnitureBySeat(@NotNull Entity seat) {
        if (isSeat(seat)) {
            CompoundTag seatExtraData = BukkitSeatManager.instance().getSeatExtraData(seat);
            int entityId = seatExtraData.getInt("entity_id");
            return BukkitFurnitureManager.instance().loadedFurnitureByMetaEntityId(entityId);
        }
        return null;
    }

    /**
     * Gets the furniture by the collider entity
     *
     * @param collider collider entity
     * @return the loaded furniture
     */
    @Nullable
    public static BukkitFurniture getLoadedFurnitureByCollider(@NotNull Entity collider) {
        Object nmsEntity = FastNMS.INSTANCE.method$CraftEntity$getHandle(collider);
        if (nmsEntity instanceof CollisionEntity collisionEntity) {
            return BukkitFurnitureManager.instance().loadedFurnitureByColliderEntityId(collisionEntity.getEntityId());
        }
        return null;
    }

    /**
     * Removes furniture
     *
     * @param entity furniture base entity
     * @return success or not
     */
    public static boolean remove(@NotNull Entity entity) {
        if (!isFurniture(entity)) return false;
        BukkitFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByMetaEntityId(entity.getEntityId());
        if (furniture == null) return false;
        furniture.destroy();
        return true;
    }

    /**
     * Removes furniture, with more options
     *
     * @param entity furniture base entity
     * @param dropLoot whether to drop loots
     * @param playSound whether to play break sound
     * @return success or not
     */
    public static boolean remove(@NotNull Entity entity,
                                 boolean dropLoot,
                                 boolean playSound) {
        if (!isFurniture(entity)) return false;
        BukkitFurniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByMetaEntityId(entity.getEntityId());
        if (furniture == null) return false;
        remove(furniture, (net.momirealms.craftengine.core.entity.player.Player) null, dropLoot, playSound);
        return true;
    }

    /**
     * Removes furniture, with more options
     *
     * @param entity furniture base entity
     * @param player the player who removes the furniture
     * @param dropLoot whether to drop loots
     * @param playSound whether to play break sound
     * @return success or not
     */
    public static boolean remove(@NotNull Entity entity,
                                 @Nullable Player player,
                                 boolean dropLoot,
                                 boolean playSound) {
        if (!isFurniture(entity)) return false;
        Furniture furniture = BukkitFurnitureManager.instance().loadedFurnitureByMetaEntityId(entity.getEntityId());
        if (furniture == null) return false;
        remove(furniture, player, dropLoot, playSound);
        return true;
    }

    /**
     * Removes furniture by providing furniture instance
     *
     * @param furniture loaded furniture
     * @param dropLoot whether to drop loots
     * @param playSound whether to play break sound
     */
    public static void remove(@NotNull Furniture furniture,
                              boolean dropLoot,
                              boolean playSound) {
        remove(furniture, (net.momirealms.craftengine.core.entity.player.Player) null, dropLoot, playSound);
    }

    /**
     * Removes furniture by providing furniture instance
     *
     * @param furniture loaded furniture
     * @param player the player who removes the furniture
     * @param dropLoot whether to drop loots
     * @param playSound whether to play break sound
     */

    public static void remove(@NotNull Furniture furniture,
                              @Nullable Player player,
                              boolean dropLoot,
                              boolean playSound) {
        remove(furniture, player == null ? null : BukkitCraftEngine.instance().adapt(player), dropLoot, playSound);
    }

    /**
     * Removes furniture by providing furniture instance
     *
     * @param furniture loaded furniture
     * @param player the player who removes the furniture
     * @param dropLoot whether to drop loots
     * @param playSound whether to play break sound
     */
    @SuppressWarnings("unchecked")
    public static void remove(@NotNull Furniture furniture,
                              @Nullable net.momirealms.craftengine.core.entity.player.Player player,
                              boolean dropLoot,
                              boolean playSound) {
        if (!furniture.isValid()) return;
        Location location = ((BukkitFurniture) furniture).getDropLocation();
        furniture.destroy();
        LootTable<ItemStack> lootTable = (LootTable<ItemStack>) furniture.config.lootTable(furniture);
        World world = BukkitAdaptors.adapt(location.getWorld());
        WorldPosition position = new WorldPosition(world, location.getX(), location.getY(), location.getZ());
        if (dropLoot && lootTable != null) {
            ContextHolder.Builder builder = ContextHolder.builder()
                    .withParameter(DirectContextParameters.POSITION, position)
                    .withParameter(DirectContextParameters.FURNITURE, furniture)
                    .withOptionalParameter(DirectContextParameters.FURNITURE_ITEM, furniture.dataAccessor.item().orElse(null));
            if (player != null) {
                Item<?> itemInHand = player.getItemInHand(InteractionHand.MAIN_HAND);
                builder.withParameter(DirectContextParameters.PLAYER, player)
                        .withOptionalParameter(DirectContextParameters.ITEM_IN_HAND, itemInHand.isEmpty() ? null : itemInHand);
            }
            List<Item<ItemStack>> items = lootTable.getRandomItems(builder.build(), world, player);
            for (Item<ItemStack> item : items) {
                world.dropItemNaturally(position, item);
            }
        }
        if (playSound) {
            world.playBlockSound(position, furniture.config.settings().sounds().breakSound());
        }
    }
}
